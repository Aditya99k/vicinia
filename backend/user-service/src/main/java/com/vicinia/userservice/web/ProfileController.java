package com.vicinia.userservice.web;

import com.vicinia.common.security.HeaderNames;
import com.vicinia.userservice.domain.UserProfile;
import com.vicinia.userservice.dto.ProfileResponse;
import com.vicinia.userservice.dto.UpdateProfileRequest;
import com.vicinia.userservice.exception.ProfileNotFoundException;
import com.vicinia.userservice.repository.UserProfileRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Trusts X-User-Id as injected by api-gateway from a validated JWT — never
 * a client-supplied value (ARCHITECTURE.md §14, IDOR prevention). There is
 * deliberately no {userId} path variable here: "me" always means whoever
 * the gateway says the caller is.
 */
@RestController
@RequestMapping("/api/users/me/profile")
public class ProfileController {

    private final UserProfileRepository userProfileRepository;

    public ProfileController(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @GetMapping
    public ProfileResponse getProfile(@RequestHeader(HeaderNames.USER_ID) String userId) {
        UserProfile profile = userProfileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ProfileNotFoundException(userId));
        return toResponse(profile);
    }

    @PutMapping
    public ProfileResponse updateProfile(@RequestHeader(HeaderNames.USER_ID) String userId,
                                          @Valid @RequestBody UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ProfileNotFoundException(userId));
        profile.setFullName(request.fullName());
        profile.setPhone(request.phone());
        userProfileRepository.save(profile);
        return toResponse(profile);
    }

    private ProfileResponse toResponse(UserProfile profile) {
        return new ProfileResponse(profile.getId().toString(), profile.getEmail(), profile.getFullName(), profile.getPhone());
    }
}
