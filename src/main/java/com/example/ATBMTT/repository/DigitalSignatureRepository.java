package com.example.ATBMTT.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ATBMTT.model.DigitalSignature;
import com.example.ATBMTT.model.PdfDocument;

@Repository
public interface DigitalSignatureRepository extends JpaRepository<DigitalSignature, Long> {
    List<DigitalSignature> findByDocument(PdfDocument document);
    Optional<DigitalSignature> findTopByDocumentOrderBySignedAtDesc(PdfDocument document);
}
