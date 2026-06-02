package com.example.ATBMTT.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Service;

@Service
public class HashService {

    /**
     * Tính SHA-256 hash của dữ liệu byte[]
     */
    public byte[] sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 không khả dụng", e);
        }
    }

    /**
     * Tính SHA-256 hash của chuỗi, trả về hex string
     */
    public String sha256Hex(String text) {
        return bytesToHex(sha256(text.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Tính SHA-256 hash của byte[], trả về hex string
     */
    public String sha256Hex(byte[] data) {
        return bytesToHex(sha256(data));
    }

    /**
     * Chuyển mảng byte thành chuỗi HEX
     */
    public String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
