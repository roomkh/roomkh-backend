package com.roomkh.backend.repository;

import com.roomkh.backend.entity.PasswordResetOtp;
import com.roomkh.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {
    Optional<PasswordResetOtp> findTopByUserAndIsUsedFalseOrderByExpiresAtDesc(User user);
}