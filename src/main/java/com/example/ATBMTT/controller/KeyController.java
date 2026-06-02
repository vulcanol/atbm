package com.example.ATBMTT.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.ATBMTT.model.KeyPair;
import com.example.ATBMTT.model.User;
import com.example.ATBMTT.services.KeyService;
import com.example.ATBMTT.services.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/keys")
public class KeyController {

    @Autowired
    private KeyService keyService;

    @Autowired
    private UserService userService;

    /* ------------------------------------------------------------------ */
    /*  Trang quản lý khoá                                                  */
    /* ------------------------------------------------------------------ */
    @GetMapping
    public String keyPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) return "redirect:/login";

        User user = userOpt.get();
        model.addAttribute("userName", user.getFullName());

        // Kiểm tra cặp khoá hiện tại
        keyService.getKeyPair(user).ifPresent(kp -> {
            model.addAttribute("keyPair", kp);
            // Hiển thị 80 ký tự đầu của public key để UI gọn
            String pubKeyPreview = kp.getPublicKey().substring(0, Math.min(80, kp.getPublicKey().length())) + "...";
            model.addAttribute("publicKeyPreview", pubKeyPreview);
        });

        return "pages/key";
    }

    /* ------------------------------------------------------------------ */
    /*  Tạo cặp khoá RSA mới                                                */
    /* ------------------------------------------------------------------ */
    @PostMapping("/generate")
    public String generateKey(@RequestParam(defaultValue = "2048") int keySize,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) return "redirect:/login";

        try {
            keyService.generateKeyPair(userOpt.get(), keySize);
            redirectAttributes.addFlashAttribute("success",
                    "Tạo cặp khoá RSA-" + keySize + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Lỗi khi tạo khoá: " + e.getMessage());
        }

        return "redirect:/keys";
    }
}
