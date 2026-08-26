package com.vicinia.catalogservice.service;

import com.vicinia.catalogservice.dto.CloudinarySignatureResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Signed direct-upload, not a proxy: the merchant's browser uploads the
 * image bytes straight to Cloudinary, never through this service — this
 * only mints the short-lived signature Cloudinary's own upload API
 * requires, over exactly the params the frontend will actually send
 * (Cloudinary's own documented rule: the signature covers every upload
 * param except file/api_key/signature/resource_type, sorted
 * alphabetically as "key=value" pairs joined with "&", then the API
 * secret appended directly — no separator — before hashing).
 */
@Service
public class CloudinaryService {

    private static final String FOLDER = "vicinia/products";

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    public CloudinaryService(@Value("${vicinia.cloudinary.cloud-name:}") String cloudName,
                              @Value("${vicinia.cloudinary.api-key:}") String apiKey,
                              @Value("${vicinia.cloudinary.api-secret:}") String apiSecret) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public CloudinarySignatureResponse signUpload() {
        long timestamp = Instant.now().getEpochSecond();
        String paramsToSign = "folder=" + FOLDER + "&timestamp=" + timestamp;
        String signature = sha1Hex(paramsToSign + apiSecret);
        return new CloudinarySignatureResponse(signature, timestamp, apiKey, cloudName, FOLDER);
    }

    private String sha1Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to compute Cloudinary signature", e);
        }
    }
}
