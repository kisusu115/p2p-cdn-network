package com.p2pnetwork.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class FileKeyGenerator {
    public static String generateFileKey(String fileName) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(fileName.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
