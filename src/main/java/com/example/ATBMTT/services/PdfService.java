package com.example.ATBMTT.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.ATBMTT.model.PdfDocument;
import com.example.ATBMTT.model.User;
import com.example.ATBMTT.repository.PdfDocumentRepository;

@Service
public class PdfService {

    private static final String UPLOAD_DIR = "uploads/pdf/";

    @Autowired
    private PdfDocumentRepository pdfDocumentRepository;

    @Autowired
    private HashService hashService;

    /**
     * Upload file PDF, tính hash SHA-256 và lưu vào DB
     */
    public PdfDocument uploadPdf(MultipartFile file, User user) throws IOException {
        // Tạo thư mục nếu chưa có
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Đặt tên file duy nhất
        String originalName = file.getOriginalFilename();
        String uniqueName = UUID.randomUUID() + "_" + originalName;
        Path filePath = uploadPath.resolve(uniqueName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Tính SHA-256
        byte[] fileBytes = Files.readAllBytes(filePath);
        String hashValue = hashService.sha256Hex(fileBytes);

        // Lưu vào DB
        PdfDocument doc = new PdfDocument();
        doc.setFileName(originalName);
        doc.setFilePath(filePath.toString());
        doc.setHashValue(hashValue);
        doc.setUploadedAt(LocalDateTime.now());
        doc.setUser(user);

        return pdfDocumentRepository.save(doc);
    }

    /**
     * Lấy danh sách tài liệu của người dùng
     */
    public List<PdfDocument> getDocuments(User user) {
        return pdfDocumentRepository.findByUserOrderByUploadedAtDesc(user);
    }

    /**
     * Lấy tài liệu theo ID
     */
    public Optional<PdfDocument> findById(Long id) {
        return pdfDocumentRepository.findById(id);
    }

    /**
     * Đọc nội dung file PDF
     */
    public byte[] readFileBytes(PdfDocument doc) throws IOException {
        return Files.readAllBytes(Paths.get(doc.getFilePath()));
    }
}
