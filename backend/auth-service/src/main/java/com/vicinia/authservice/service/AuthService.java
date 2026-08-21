package com.vicinia.authservice.service;

import com.vicinia.authservice.domain.Role;
import com.vicinia.authservice.domain.UserCredential;
import com.vicinia.authservice.dto.AuthResponse;
import com.vicinia.authservice.dto.LoginRequest;
import com.vicinia.authservice.dto.RefreshRequest;
import com.vicinia.authservice.dto.ResetPasswordRequest;
import com.vicinia.authservice.dto.SignupRequest;
import com.vicinia.authservice.exception.EmailAlreadyExistsException;
import com.vicinia.authservice.exception.InvalidCredentialsException;
import com.vicinia.authservice.exception.InvalidRoleException;
import com.vicinia.authservice.exception.InvalidTokenException;
import com.vicinia.authservice.messaging.UserEventPublisher;
import com.vicinia.authservice.repository.RoleRepository;
import com.vicinia.authservice.repository.UserCredentialRepository;
import com.vicinia.common.jwt.JwtTokenProvider;
import com.vicinia.common.jwt.TokenClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Set<String> SELF_REGISTERABLE_ROLES = Set.of("CUSTOMER", "MERCHANT", "DELIVERY_PARTNER");
    private static final String DEFAULT_ROLE = "CUSTOMER";

    private final UserCredentialRepository userCredentialRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordResetService passwordResetService;
    private final UserEventPublisher userEventPublisher;

    public AuthService(UserCredentialRepository userCredentialRepository,
                        RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder,
                        JwtTokenProvider jwtTokenProvider,
                        RefreshTokenService refreshTokenService,
                        TokenBlacklistService tokenBlacklistService,
                        PasswordResetService passwordResetService,
                        UserEventPublisher userEventPublisher) {
        this.userCredentialRepository = userCredentialRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.passwordResetService = passwordResetService;
        this.userEventPublisher = userEventPublisher;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userCredentialRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        String requestedRole = (request.role() == null || request.role().isBlank())
                ? DEFAULT_ROLE : request.role().toUpperCase();
        if (!SELF_REGISTERABLE_ROLES.contains(requestedRole)) {
            throw new InvalidRoleException(requestedRole);
        }

        Role role = roleRepository.findByName(requestedRole)
                .orElseThrow(() -> new IllegalStateException("Role " + requestedRole + " is not seeded"));

        UserCredential user = new UserCredential(request.email(), passwordEncoder.encode(request.password()));
        user.addRole(role);
        user = userCredentialRepository.save(user);

        userEventPublisher.publishUserRegistered(user.getId().toString(), user.getEmail(), user.roleNames());

        return issueTokens(user);
    }

    public AuthResponse login(LoginRequest request) {
        UserCredential user = userCredentialRepository.findByEmail(request.email())
                .filter(UserCredential::isEnabled)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return issueTokens(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
        if (!refreshTokenService.validateAndConsume(request.userId(), request.refreshToken())) {
            throw new InvalidTokenException("Refresh token is invalid, expired, or already used");
        }

        UserCredential user = userCredentialRepository.findById(UUID.fromString(request.userId()))
                .filter(UserCredential::isEnabled)
                .orElseThrow(() -> new InvalidTokenException("User no longer exists"));

        return issueTokens(user);
    }

    /** Idempotent: an already-expired or malformed token just means there's nothing left to blacklist. */
    public void logout(String rawAccessToken) {
        TokenClaims claims;
        try {
            claims = jwtTokenProvider.parse(rawAccessToken);
        } catch (RuntimeException e) {
            return;
        }
        tokenBlacklistService.blacklist(claims.jti(), claims.expiresAt());
        refreshTokenService.revoke(claims.userId());
    }

    /** Always appears to succeed whether or not the email exists — doesn't leak which emails are registered. */
    public void forgotPassword(String email) {
        userCredentialRepository.findByEmail(email).ifPresent(user -> {
            String token = passwordResetService.createToken(user.getId());
            // notification-service doesn't exist until Stage 12 — logging the
            // link is the deliberate V1 stub called out in ARCHITECTURE.md.
            log.info("Password reset requested for {} — reset token: {}", email, token);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        UUID userId = passwordResetService.consumeToken(request.token())
                .orElseThrow(() -> new InvalidTokenException("Reset token is invalid or expired"));

        UserCredential user = userCredentialRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("Reset token is invalid or expired"));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userCredentialRepository.save(user);
        refreshTokenService.revoke(userId.toString());
    }

    private AuthResponse issueTokens(UserCredential user) {
        Set<String> roles = user.roleNames();
        Set<String> permissions = user.permissionNames();
        String accessToken = jwtTokenProvider.issueAccessToken(
                user.getId().toString(), user.getEmail(), List.copyOf(roles), List.copyOf(permissions));
        String refreshToken = refreshTokenService.issue(user.getId().toString());

        return new AuthResponse(
                user.getId().toString(),
                user.getEmail(),
                roles,
                permissions,
                accessToken,
                refreshToken,
                jwtTokenProvider.accessTokenTtlSeconds()
        );
    }
}
