package com.roomkh.backend.repository;

import com.roomkh.backend.entity.AccountStatus;
import com.roomkh.backend.entity.RoleName;
import com.roomkh.backend.entity.SellerStatus;
import com.roomkh.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);

    long countByRole_Name(RoleName name);

    @Query("SELECT u FROM User u WHERE " +
            "(:search = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:roleName IS NULL OR u.role.name = :roleName) " +
            "AND (:statusType = 'ALL' OR " +
            "     (:statusType = 'PENDING' AND u.sellerStatus = :pendingEnum) OR " +
            "     (:statusType = 'ACCOUNT' AND u.accountStatus = :accountEnum AND (u.sellerStatus IS NULL OR u.sellerStatus != :pendingEnum)))")
    Page<User> findUsersWithAllFilters(
            @Param("search") String search,
            @Param("roleName") RoleName roleName,
            @Param("statusType") String statusType,
            @Param("pendingEnum") SellerStatus pendingEnum,
            @Param("accountEnum") AccountStatus accountEnum,
            Pageable pageable);
}