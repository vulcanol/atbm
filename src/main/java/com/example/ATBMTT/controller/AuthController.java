package com.example.ATBMTT.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.ATBMTT.model.User;
import com.example.ATBMTT.services.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    /* ------------------------------------------------------------------ */
    /*  Trang chủ -> redirect về login                                      */
    /* ------------------------------------------------------------------ */
    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    /* ------------------------------------------------------------------ */
    /*  ĐĂNG NHẬP                                                           */
    /* ------------------------------------------------------------------ */
    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/dashboard";
        }
        return "pages/login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email,
                          @RequestParam String password,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        Optional<User> opt = userService.login(email, password);
        if (opt.isPresent()) {
            User user = opt.get();
            session.setAttribute("userId", user.getId());
            session.setAttribute("userName", user.getFullName());
            return "redirect:/dashboard";
        }
        redirectAttributes.addFlashAttribute("error", "Email hoặc mật khẩu không đúng!");
        return "redirect:/login";
    }

    /* ------------------------------------------------------------------ */
    /*  ĐĂNG KÝ                                                             */
    /* ------------------------------------------------------------------ */
    @GetMapping("/register")
    public String registerPage(HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/dashboard";
        }
        return "pages/register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String fullName,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam String confirmPassword,
                             RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "redirect:/register";
        }
        try {
            userService.register(fullName, email, password);
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }

    /* ------------------------------------------------------------------ */
    /*  ĐĂNG XUẤT                                                           */
    /* ------------------------------------------------------------------ */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    /* ------------------------------------------------------------------ */
    /*  DASHBOARD                                                            */
    /* ------------------------------------------------------------------ */
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        model.addAttribute("userName", session.getAttribute("userName"));
        return "pages/dashboard";
    }
}
