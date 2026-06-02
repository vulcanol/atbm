package com.example.ATBMTT.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/sign")
public class SignatureController {

    @Autowired
    private SignatureService signatureService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private KeyService keyService;

    @Autowired
    private UserService userService;

    /* ------------------------------------------------------------------ */
    /*  Trang ký số                                                          */
    /* ------------------------------------------------------------------ */
    @GetMapping
    public String signPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) return "redirect:/login";

        User user = userOpt.get();
        model.addAttribute("userName", user.getFullName());

        // Danh sách tài liệu của user để chọn ký
        List<PdfDocument> documents = pdfService.getDocuments(user);
        model.addAttribute("documents", documents);

        // Kiểm tra đã có cặp khoá chưa
        boolean hasKey = keyService.getKeyPair(user).isPresent();
        model.addAttribute("hasKey", hasKey);

        return "pages/sign";
    }

    /* ------------------------------------------------------------------ */
    /*  Thực hiện ký số                                                      */
    /* ------------------------------------------------------------------ */
    @PostMapping("/do-sign")
    public String doSign(@RequestParam Long documentId,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) return "redirect:/login";

        User user = userOpt.get();

        // Kiểm tra cặp khoá
        Optional<KeyPair> keyPairOpt = keyService.getKeyPair(user);
        if (keyPairOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "Bạn chưa có cặp khoá RSA. Vui lòng tạo khoá trước!");
            return "redirect:/sign";
        }

        // Kiểm tra tài liệu
        Optional<PdfDocument> docOpt = pdfService.findById(documentId);
        if (docOpt.isEmpty() || !docOpt.get().getUser().getId().equals(userId)) {
            redirectAttributes.addFlashAttribute("error", "Tài liệu không tồn tại!");
            return "redirect:/sign";
        }

        try {
            byte[] pdfBytes = pdfService.readFileBytes(docOpt.get());
            DigitalSignature sig = signatureService.signDocument(docOpt.get(), keyPairOpt.get(), pdfBytes);
            redirectAttributes.addFlashAttribute("success",
                    "Ký số thành công! Chữ ký ID: " + sig.getId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Lỗi ký số: " + e.getMessage());
        }

        return "redirect:/sign";
    }

    /* ------------------------------------------------------------------ */
    /*  Trang xác minh chữ ký                                               */
    /* ------------------------------------------------------------------ */
    @GetMapping("/verify")
    public String verifyPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        model.addAttribute("userName", session.getAttribute("userName"));
        return "pages/verify";
    }

    /* ------------------------------------------------------------------ */
    /*  Thực hiện xác minh                                                  */
    /* ------------------------------------------------------------------ */
    @PostMapping("/do-verify")
    public String doVerify(@RequestParam("file") MultipartFile file,
                           @RequestParam String signatureBase64,
                           @RequestParam String publicKeyBase64,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        if (file.isEmpty() || signatureBase64.isBlank() || publicKeyBase64.isBlank()) {
            redirectAttributes.addFlashAttribute("error",
                    "Vui lòng cung cấp đầy đủ: file PDF, chữ ký và khoá công khai!");
            return "redirect:/sign/verify";
        }

        try {
            byte[] pdfBytes = file.getBytes();
            boolean valid = signatureService.verifySignature(pdfBytes,
                    signatureBase64.trim(), publicKeyBase64.trim());

            if (valid) {
                redirectAttributes.addFlashAttribute("success",
                        "✅ Chữ ký HỢP LỆ! Tài liệu chưa bị chỉnh sửa.");
            } else {
                redirectAttributes.addFlashAttribute("error",
                        "❌ Chữ ký KHÔNG HỢP LỆ! Tài liệu đã bị thay đổi hoặc khoá không khớp.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Lỗi xác minh: " + e.getMessage());
        }

        return "redirect:/sign/verify";
    }
}
