package com.roomkh.backend.service;

import com.roomkh.backend.dto.admin.*;
import com.roomkh.backend.entity.PlanType;
import com.roomkh.backend.entity.RoleName;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface AdminDashboardService {
    AdminDashboardStatsResponse getDashboardStats();

    Page<AdminUserListItemResponse> getUsers(
            String search,
            RoleName role,
            String status,
            LocalDate startDate, LocalDate endDate,
            int page,
            int size);

    void updateUserStatus(Long userId, UpdateUserStatusRequest request);

    void createUser(AdminCreateUserRequest request);

    Page<AdminDashboardPropertyResponse> getProperties(
            String search,
            String status,
            String type,
            String city,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size);

    AdminPropertyStatsResponse getPropertyStats(LocalDate startDate, LocalDate endDate);

    AdminPropertyDetailResponse getPropertyDetail(Long id);

    void softDeleteProperty(Long id);

    byte[] exportPropertiesToExcel();

    byte[] exportUsersToExcel();

    AdminUserStatsResponse getUserStats(LocalDate startDate, LocalDate endDate);

    Page<AdminOwnerListItemResponse> getOwners(String search, String status, PlanType plan, LocalDate startDate, LocalDate endDate, int page, int size);

    AdminOwnerStatsResponse getOwnerStats(LocalDate startDate, LocalDate endDate);

    void exportOwnersToCsv(String search, String status, PlanType plan, LocalDate startDate, LocalDate endDate, java.io.Writer writer) throws java.io.IOException;
}