package com.example.ATBMTT.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ATBMTT.model.KeyPair;
import com.example.ATBMTT.model.User;

@Repository
public interface KeyPairRepository extends JpaRepository<KeyPair, Long> {
    Optional<KeyPair> findByUser(User user);
    Optional<KeyPair> findByUserId(Long userId);
}
