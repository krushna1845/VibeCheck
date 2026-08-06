package com.krushna.moviebooking.payment.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Security utility for computing and verifying HMAC-SHA256 signatures for payment webhooks.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HmacUtils {

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Computes the HMAC-SHA256 hex string for a given payload and secret.
     *
     * @param payload Raw payload string
     * @param secret  Secret key
     * @return Lowercase hex-encoded HMAC-SHA256 signature
     */
    public static String calculateHmacSha256(String payload, String secret) {
        if (payload == null || secret == null) {
            throw new IllegalArgumentException("Payload and secret must not be null for HMAC computation");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to compute HMAC-SHA256", e);
            throw new IllegalStateException("Error computing HMAC signature", e);
        }
    }

    /**
     * Verifies that the expected signature matches the provided signature using constant-time comparison.
     *
     * @param payload           Raw request body/payload
     * @param providedSignature Header/query signature string
     * @param secret            Configured secret
     * @return true if signatures match, false otherwise
     */
    public static boolean verifyHmacSha256(String payload, String providedSignature, String secret) {
        if (payload == null || providedSignature == null || secret == null) {
            return false;
        }
        String calculated = calculateHmacSha256(payload, secret);
        return constantTimeEquals(calculated.toLowerCase(), providedSignature.trim().toLowerCase());
    }

    /**
     * Constant-time comparison to prevent timing attacks against signature checks.
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
