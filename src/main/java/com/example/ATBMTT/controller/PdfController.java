package com.example.ATBMTT.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.ATBMTT.model.DigitalSignature;
import com.example.ATBMTT.model.KeyPair;
import com.example.ATBMTT.model.PdfDocument;
import com.example.ATBMTT.model.User;
import com.example.ATBMTT.services.KeyService;
import com.example.ATBMTT.services.PdfService;
import com.example.ATBMTT.services.SignatureService;
import com.example.ATBMTT.services.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/documents")
public class PdfController {

    @Autowired
    private PdfService pdfService;

    @Autowired
    private UserService userService;

    @Autowired
    private SignatureService signatureService;

    @Autowired
    private KeyService keyService;

    /* ------------------------------------------------------------------ */
    /*  Danh sách tài liệu                                                   */
    /* ------------------------------------------------------------------ */
    @GetMapping
    public String listDocuments(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) return "redirect:/login";

        User user = userOpt.get();
        List<PdfDocument> documents = pdfService.getDocuments(user);

        model.addAttribute("userName", user.getFullName());
        model.addAttribute("documents", documents);
        return "pages/document";
    }

    /* ------------------------------------------------------------------ */
    /*  Upload tài liệu PDF                                                  */
    /* ------------------------------------------------------------------ */
    @PostMapping("/upload")
    public String uploadDocument(@RequestParam("file") MultipartFile file,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) return "redirect:/login";

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn file PDF!");
            return "redirect:/documents";
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
            redirectAttributes.addFlashAttribute("error", "Chỉ chấp nhận file PDF!");
            return "redirect:/documents";
        }

        try {
            PdfDocument doc = pdfService.uploadPdf(file, userOpt.get());
            redirectAttributes.addFlashAttribute("success",
                    "Upload thành công: " + doc.getFileName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Lỗi upload: " + e.getMessage());
        }

        return "redirect:/documents";
    }

    /* ------------------------------------------------------------------ */
    /*  Download tài liệu PDF                                               */
    /* ------------------------------------------------------------------ */
    @GetMapping("/download/{id}")
    public ResponseEntity<ByteArrayResource> downloadDocument(@PathVariable Long id,
                                                               HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<PdfDocument> docOpt = pdfService.findById(id);
        if (docOpt.isEmpty()) return ResponseEntity.notFound().build();

        PdfDocument doc = docOpt.get();
        // Chỉ cho phép tải file của chính mình
        if (!doc.getUser().getId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        try {
            byte[] data = pdfService.readFileBytes(doc);
            ByteArrayResource resource = new ByteArrayResource(data);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + doc.getFileName() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(data.length)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Chi tiết tài liệu (SHA-256, chữ ký, public key)                     */
    /* ------------------------------------------------------------------ */
    @GetMapping("/detail/{id}")
    public String documentDetail(@PathVariable Long id,
                                 HttpSession session,
                                 Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) return "redirect:/login";

        User user = userOpt.get();

        Optional<PdfDocument> docOpt = pdfService.findById(id);
        if (docOpt.isEmpty() || !docOpt.get().getUser().getId().equals(userId)) {
            return "redirect:/documents";
        }

        PdfDocument document = docOpt.get();

        // Lấy chữ ký mới nhất
        Optional<DigitalSignature> signatureOpt = signatureService.getLatestSignature(document);
        DigitalSignature signature = signatureOpt.orElse(null);

        // Lấy public key
        String publicKey = null;
        if (signature != null && signature.getKeyPair() != null) {
            publicKey = signature.getKeyPair().getPublicKey();
        } else {
            // Fallback: lấy public key hiện tại của user
            Optional<KeyPair> keyPairOpt = keyService.getKeyPair(user);
            if (keyPairOpt.isPresent()) {
                publicKey = keyPairOpt.get().getPublicKey();
            }
        }

        model.addAttribute("userName", user.getFullName());
        model.addAttribute("document", document);
        model.addAttribute("signature", signature);
        model.addAttribute("publicKey", publicKey);
        model.addAttribute("isSigned", signature != null);

        return "pages/document-detail";
    }
}
