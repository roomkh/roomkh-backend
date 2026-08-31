package com.roomkh.backend.repository;

import com.roomkh.backend.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);

    long countByRole_Name(RoleName name);

    @Query("SELECT u FROM User u WHERE " +
            "(:search = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR u.phoneNumber LIKE CONCAT('%', :search, '%')) " +
            "AND (:role IS NULL OR u.role.name = :role) " +
            "AND (" +
            "  :statusType = 'ALL' " +
            "  OR (:statusType = 'PENDING' AND u.sellerStatus = :pendingSellerStatus) " +
            "  OR (:statusType = 'ACCOUNT' AND u.accountStatus = :targetAccountStatus) " +
            ") " +
            "AND (cast(:startDateTime as timestamp) IS NULL OR u.createdAt >= :startDateTime) " +
            "AND (cast(:endDateTime as timestamp) IS NULL OR u.createdAt <= :endDateTime)")
    Page<User> findUsersWithAllFilters(
            @Param("search") String search,
            @Param("role") RoleName role,
            @Param("statusType") String statusType,
            @Param("pendingSellerStatus") SellerStatus pendingSellerStatus,
            @Param("targetAccountStatus") AccountStatus targetAccountStatus,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            Pageable pageable);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName AND u.createdAt BETWEEN :start AND :end")
    long countByRoleNameAndCreatedAtBetween(@Param("roleName") RoleName roleName, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(u) FROM User u WHERE u.accountStatus = :status AND u.createdAt BETWEEN :start AND :end")
    long countByAccountStatusAndCreatedAtBetween(@Param("status") AccountStatus status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName AND u.sellerStatus = :sellerStatus AND u.createdAt BETWEEN :start AND :end")
    long countByRoleNameAndSellerStatusAndCreatedAtBetween(@Param("roleName") RoleName roleName, @Param("sellerStatus") SellerStatus sellerStatus, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT u FROM User u WHERE u.role.name = 'SELLER' " +
            "AND (:search = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR u.phoneNumber LIKE CONCAT('%', :search, '%')) " +
            "AND (:statusType = 'ALL' OR (:statusType = 'PENDING' AND u.sellerStatus = :pendingSellerStatus) OR (:statusType = 'ACCOUNT' AND u.accountStatus = :targetAccountStatus)) " +
            "AND (:planType IS NULL OR u.planType = :planType) " +
            "AND (cast(:startDateTime as timestamp) IS NULL OR u.createdAt >= :startDateTime) " +
            "AND (cast(:endDateTime as timestamp) IS NULL OR u.createdAt <= :endDateTime)")
    Page<User> findOwnersFiltered(
            @Param("search") String search,
            @Param("statusType") String statusType,
            @Param("pendingSellerStatus") SellerStatus pendingSellerStatus,
            @Param("targetAccountStatus") AccountStatus targetAccountStatus,
            @Param("planType") PlanType planType,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName AND u.accountStatus = :accountStatus AND u.createdAt BETWEEN :start AND :end")
    long countByRoleNameAndAccountStatusAndCreatedAtBetween(
            @Param("roleName") RoleName roleName,
            @Param("accountStatus") AccountStatus accountStatus,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}