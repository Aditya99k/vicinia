package com.vicinia.userservice.web;

import com.vicinia.userservice.domain.UserProfile;
import com.vicinia.userservice.dto.ContactSummaryResponse;
import com.vicinia.userservice.exception.ProfileNotFoundException;
import com.vicinia.userservice.repository.UserProfileRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Service-to-service only, by convention — same trust model as
 * inventory-service's ListingController./{id} (called service-to-service,
 * needs only InternalRequestFilter to pass, no per-caller ownership check
 * here). order-service uses this to resolve "who is this order's rider,
 * and how do I reach them" once one is assigned, for the customer's own
 * order-detail page — the real ownership check already happened one hop
 * earlier, in OrderService.getById.
 */
@RestController
@RequestMapping("/api/users")
public class ContactController {

    private final UserProfileRepository userProfileRepository;

    public ContactController(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @GetMapping("/{userId}/contact-summary")
    public ContactSummaryResponse contactSummary(@PathVariable UUID userId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ProfileNotFoundException(userId.toString()));
        return new ContactSummaryResponse(profile.getFullName(), profile.getPhone());
    }
}
