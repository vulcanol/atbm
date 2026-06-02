package com.example.ATBMTT.services;

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.ATBMTT.model.KeyPair;
import com.example.ATBMTT.model.User;
import com.example.ATBMTT.repository.KeyPairRepository;

@Service
public class KeyService {

    @Autowired
    private KeyPairRepository keyPairRepository;

    /**
     * Sinh cặp khoá RSA với kích thước keySize (1024, 2048, 4096)
     */
    public KeyPair generateKeyPair(User user, int keySize) throws Exception {
        // Xoá cặp khoá cũ nếu có
        keyPairRepository.findByUser(user).ifPresent(keyPairRepository::delete);

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(keySize);
        java.security.KeyPair rsaKeyPair = generator.generateKeyPair();

        String publicKeyBase64 = Base64.getEncoder().encodeToString(
                rsaKeyPair.getPublic().getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(
                rsaKeyPair.getPrivate().getEncoded());

        KeyPair keyPair = new KeyPair();
        keyPair.setPublicKey(publicKeyBase64);
        keyPair.setPrivateKey(privateKeyBase64);
        keyPair.setKeySize(keySize);
        keyPair.setUser(user);

        return keyPairRepository.save(keyPair);
    }

    /**
     * Lấy cặp khoá của người dùng
     */
    public Optional<KeyPair> getKeyPair(User user) {
        return keyPairRepository.findByUser(user);
    }

    public Optional<KeyPair> getKeyPairByUserId(Long userId) {
        return keyPairRepository.findByUserId(userId);
    }

    /**
     * Chuyển Base64 thành PublicKey
     */
    public PublicKey decodePublicKey(String base64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }

    /**
     * Chuyển Base64 thành PrivateKey
     */
    public PrivateKey decodePrivateKey(String base64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePrivate(spec);
    }
}
