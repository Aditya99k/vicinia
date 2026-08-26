package com.vicinia.catalogservice.dto;

/** Everything the browser needs to upload a file directly to Cloudinary — the image bytes never touch our own servers, only this short-lived signature does. */
public record CloudinarySignatureResponse(
        String signature,
        long timestamp,
        String apiKey,
        String cloudName,
        String folder
) {
}
