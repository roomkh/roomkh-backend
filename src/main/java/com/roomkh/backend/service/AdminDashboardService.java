package com.roomkh.backend.service;

import com.roomkh.backend.dto.admin.AdminDashboardStatsResponse;
import com.roomkh.backend.dto.admin.AdminUserListItemResponse;
import com.roomkh.backend.dto.admin.UpdateUserStatusRequest;
import com.roomkh.backend.entity.RoleName;
import org.springframework.data.domain.Page;

public interface AdminDashboardService {
    AdminDashboardStatsResponse getDashboardStats();

    Page<AdminUserListItemResponse> getUsers(
            String search,
            RoleName role,
            String status,
            int page,
            int size);

    void updateUserStatus(Long userId, UpdateUserStatusRequest request);
}