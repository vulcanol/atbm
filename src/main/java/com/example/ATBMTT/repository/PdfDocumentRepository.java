package com.example.ATBMTT.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ATBMTT.model.PdfDocument;
import com.example.ATBMTT.model.User;

@Repository
public interface PdfDocumentRepository extends JpaRepository<PdfDocument, Long> {
    List<PdfDocument> findByUserOrderByUploadedAtDesc(User user);
    List<PdfDocument> findByUserId(Long userId);
}
