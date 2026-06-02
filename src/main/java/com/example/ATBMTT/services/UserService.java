package com.example.ATBMTT.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.ATBMTT.model.User;
import com.example.ATBMTT.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Đăng ký người dùng mới (mật khẩu lưu plain-text cho demo, production nên bcrypt)
     */
    public User register(String fullName, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã được sử dụng: " + email);
        }
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(password); // TODO: mã hoá bcrypt khi production
        return userRepository.save(user);
    }

    /**
     * Đăng nhập đơn giản (so sánh plain-text)
     */
    public Optional<User> login(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(u -> u.getPassword().equals(password));
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}
