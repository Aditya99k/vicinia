package com.vicinia.userservice.web;

import com.vicinia.common.security.HeaderNames;
import com.vicinia.userservice.domain.Address;
import com.vicinia.userservice.dto.AddressRequest;
import com.vicinia.userservice.dto.AddressResponse;
import com.vicinia.userservice.exception.AddressNotFoundException;
import com.vicinia.userservice.repository.AddressRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/addresses")
public class AddressController {

    private final AddressRepository addressRepository;

    public AddressController(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @GetMapping
    public List<AddressResponse> list(@RequestHeader(HeaderNames.USER_ID) String userId) {
        return addressRepository.findByUserId(UUID.fromString(userId)).stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Transactional
    @PostMapping
    public ResponseEntity<AddressResponse> create(@RequestHeader(HeaderNames.USER_ID) String userId,
                                                    @Valid @RequestBody AddressRequest request) {
        Address address = new Address(UUID.fromString(userId), request.label(), request.line1(), request.line2(),
                request.city(), request.state(), request.pincode(), request.isDefault());
        address = addressRepository.save(address);
        if (request.isDefault()) {
            addressRepository.clearDefaultForOthers(UUID.fromString(userId), address.getId());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(AddressResponse.from(address));
    }

    @Transactional
    @PutMapping("/{addressId}")
    public AddressResponse update(@RequestHeader(HeaderNames.USER_ID) String userId,
                                   @PathVariable String addressId,
                                   @Valid @RequestBody AddressRequest request) {
        Address address = addressRepository.findByIdAndUserId(UUID.fromString(addressId), UUID.fromString(userId));
        if (address == null) {
            throw new AddressNotFoundException(addressId);
        }
        address.update(request.label(), request.line1(), request.line2(),
                request.city(), request.state(), request.pincode(), request.isDefault());
        addressRepository.save(address);
        if (request.isDefault()) {
            addressRepository.clearDefaultForOthers(UUID.fromString(userId), address.getId());
        }
        return AddressResponse.from(address);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> delete(@RequestHeader(HeaderNames.USER_ID) String userId,
                                        @PathVariable String addressId) {
        Address address = addressRepository.findByIdAndUserId(UUID.fromString(addressId), UUID.fromString(userId));
        if (address == null) {
            throw new AddressNotFoundException(addressId);
        }
        addressRepository.delete(address);
        return ResponseEntity.noContent().build();
    }
}
