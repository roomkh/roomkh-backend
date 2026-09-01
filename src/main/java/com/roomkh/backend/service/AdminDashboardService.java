package com.roomkh.backend.service;

import com.roomkh.backend.dto.admin.*;
import com.roomkh.backend.entity.PlanType;
import com.roomkh.backend.entity.PropertyStatus;
import com.roomkh.backend.entity.PropertyType;
import com.roomkh.backend.entity.RoleName;
import org.springframework.data.domain.Page;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface    AdminDashboardService {
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

    byte[] exportUsersToExcel(String search, RoleName role, String status, LocalDate startDate, LocalDate endDate) throws IOException;

    byte[] exportPropertiesToExcel(String search, PropertyStatus status, PropertyType type, String city, LocalDate startDate, LocalDate endDate) throws IOException;

    AdminUserStatsResponse getUserStats(LocalDate startDate, LocalDate endDate);

    Page<AdminOwnerListItemResponse> getOwners(String search, String status, PlanType plan, LocalDate startDate, LocalDate endDate, int page, int size);

    AdminOwnerStatsResponse getOwnerStats(LocalDate startDate, LocalDate endDate);

    void exportOwnersToCsv(String search, String status, PlanType plan, LocalDate startDate, LocalDate endDate, java.io.Writer writer) throws IOException;

    AdminProfileResponse getAdminProfile(String email);

    List<NotificationItemResponse> getUnreadNotifications(String email);
}