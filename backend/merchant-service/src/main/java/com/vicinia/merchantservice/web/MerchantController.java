package com.vicinia.merchantservice.web;

import com.vicinia.common.security.HeaderNames;
import com.vicinia.merchantservice.domain.Merchant;
import com.vicinia.merchantservice.dto.ApplyRequest;
import com.vicinia.merchantservice.dto.MerchantResponse;
import com.vicinia.merchantservice.dto.MerchantStatusResponse;
import com.vicinia.merchantservice.dto.MerchantSummaryResponse;
import com.vicinia.merchantservice.dto.UpdateHoursRequest;
import com.vicinia.merchantservice.dto.UpdateStoreProfileRequest;
import com.vicinia.merchantservice.service.MerchantService;
import com.vicinia.merchantservice.util.GeoDistance;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Mapped at /api/merchants to match api-gateway's route exactly (no path rewriting, per Stage 1). */
@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping("/apply")
    public ResponseEntity<MerchantResponse> apply(@RequestHeader(HeaderNames.USER_ID) String userId,
                                                    @Valid @RequestBody ApplyRequest request) {
        Merchant merchant = merchantService.apply(UUID.fromString(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MerchantResponse.from(merchant));
    }

    @GetMapping("/me")
    public MerchantResponse getMine(@RequestHeader(HeaderNames.USER_ID) String userId) {
        return MerchantResponse.from(merchantService.getMine(UUID.fromString(userId)));
    }

    @PutMapping("/me")
    public MerchantResponse updateProfile(@RequestHeader(HeaderNames.USER_ID) String userId,
                                           @Valid @RequestBody UpdateStoreProfileRequest request) {
        return MerchantResponse.from(merchantService.updateProfile(UUID.fromString(userId), request));
    }

    @PutMapping("/me/hours")
    public MerchantResponse updateHours(@RequestHeader(HeaderNames.USER_ID) String userId,
                                         @Valid @RequestBody UpdateHoursRequest request) {
        return MerchantResponse.from(merchantService.updateHours(UUID.fromString(userId), request));
    }

    @PostMapping("/me/go-live")
    public MerchantResponse goLive(@RequestHeader(HeaderNames.USER_ID) String userId) {
        return MerchantResponse.from(merchantService.goLive(UUID.fromString(userId)));
    }

    @PostMapping("/me/close")
    public MerchantResponse close(@RequestHeader(HeaderNames.USER_ID) String userId) {
        return MerchantResponse.from(merchantService.close(UUID.fromString(userId)));
    }

    @PostMapping("/me/reopen")
    public MerchantResponse reopen(@RequestHeader(HeaderNames.USER_ID) String userId) {
        return MerchantResponse.from(merchantService.reopen(UUID.fromString(userId)));
    }

    /** Public per api-gateway's public-paths (no auth headers present). See MerchantService.nearby's own comment for the lat/lng vs city precedence. */
    @GetMapping("/nearby")
    public List<MerchantSummaryResponse> nearby(@RequestParam(required = false) String city,
                                                 @RequestParam(required = false) Double latitude,
                                                 @RequestParam(required = false) Double longitude) {
        return merchantService.nearby(city, latitude, longitude).stream()
                .map(m -> MerchantSummaryResponse.from(m, distanceKmOrNull(m, latitude, longitude)))
                .toList();
    }

    private Double distanceKmOrNull(Merchant m, Double latitude, Double longitude) {
        if (latitude == null || longitude == null || m.getLatitude() == null || m.getLongitude() == null) {
            return null;
        }
        return GeoDistance.km(latitude, longitude, m.getLatitude(), m.getLongitude());
    }

    /** A store's live open/closed state — see MerchantStatusResponse for why this exists alongside /nearby. Not public: only reached from an already-authenticated customer's store/product page. */
    @GetMapping("/{ownerUserId}/status")
    public MerchantStatusResponse status(@PathVariable UUID ownerUserId) {
        return MerchantStatusResponse.from(merchantService.getByOwnerUserId(ownerUserId));
    }
}
