package com.example.ATBMTT.services;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.ATBMTT.model.DigitalSignature;
import com.example.ATBMTT.model.KeyPair;
import com.example.ATBMTT.model.PdfDocument;
import com.example.ATBMTT.repository.DigitalSignatureRepository;

@Service
public class SignatureService {

    private static final String ALGORITHM = "SHA256withRSA";

    @Autowired
    private DigitalSignatureRepository digitalSignatureRepository;

    @Autowired
    private HashService hashService;

    @Autowired
    private KeyService keyService;

    /**
     * Ký số tài liệu PDF bằng khoá riêng RSA
     * Quy trình: Hash(PDF bytes) → Sign(hash, privateKey) → Base64 encode
     */
    public DigitalSignature signDocument(PdfDocument document, KeyPair keyPair, byte[] pdfBytes) throws Exception {
        // Lấy private key
        PrivateKey privateKey = keyService.decodePrivateKey(keyPair.getPrivateKey());

        // Tính hash SHA-256 của nội dung PDF
        byte[] hash = hashService.sha256(pdfBytes);

        // Ký hash bằng RSA SHA256withRSA
        Signature signer = Signature.getInstance(ALGORITHM);
        signer.initSign(privateKey);
        signer.update(hash);
        byte[] signatureBytes = signer.sign();

        // Mã hoá Base64
        String signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);

        // Lưu vào DB
        DigitalSignature digitalSignature = new DigitalSignature();
        digitalSignature.setSignatureValue(signatureBase64);
        digitalSignature.setAlgorithm(ALGORITHM);
        digitalSignature.setSignedAt(LocalDateTime.now());
        digitalSignature.setDocument(document);
        digitalSignature.setKeyPair(keyPair);

        return digitalSignatureRepository.save(digitalSignature);
    }

    /**
     * Xác minh chữ ký số
     * Quy trình: Hash(PDF bytes) → Verify(hash, signature, publicKey)
     */
    public boolean verifySignature(byte[] pdfBytes, String signatureBase64, String publicKeyBase64) {
        try {
            PublicKey publicKey = keyService.decodePublicKey(publicKeyBase64);

            // Tính hash SHA-256 của nội dung PDF
            byte[] hash = hashService.sha256(pdfBytes);

            // Giải mã chữ ký từ Base64
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

            // Xác minh
            Signature verifier = Signature.getInstance(ALGORITHM);
            verifier.initVerify(publicKey);
            verifier.update(hash);
            return verifier.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lấy danh sách chữ ký của tài liệu
     */
    public List<DigitalSignature> getSignatures(PdfDocument document) {
        return digitalSignatureRepository.findByDocument(document);
    }

    /**
     * Lấy chữ ký mới nhất của tài liệu
     */
    public Optional<DigitalSignature> getLatestSignature(PdfDocument document) {
        return digitalSignatureRepository.findTopByDocumentOrderBySignedAtDesc(document);
    }
}
