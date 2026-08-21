package com.roomkh.backend.repository;

import com.roomkh.backend.entity.LoginSecurityKeyType;
import com.roomkh.backend.entity.LoginSecurityRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginSecurityRecordRepository extends JpaRepository<LoginSecurityRecord, Long> {
    Optional<LoginSecurityRecord> findByKeyTypeAndKeyHash(LoginSecurityKeyType keyType, String keyHash);
}