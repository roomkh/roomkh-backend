package com.roomkh.backend.service.impl;

import com.roomkh.backend.dto.admin.AdminDashboardStatsResponse;
import com.roomkh.backend.entity.PropertyStatus;
import com.roomkh.backend.entity.RoleName;
import com.roomkh.backend.repository.PropertyRepository;
import com.roomkh.backend.repository.UserRepository;
import com.roomkh.backend.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getDashboardStats() {
        long totalUsers = userRepository.countByRole_Name(RoleName.USER);
        long totalOwners = userRepository.countByRole_Name(RoleName.SELLER);
        long totalListings = propertyRepository.count();
        long pendingListings = propertyRepository.countByStatus(PropertyStatus.PENDING);
        
        // Mocked monthly revenue for UI layout purposes (Phase 13)
        double mockedMonthlyRevenue = 42850.00;

        return AdminDashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalOwners(totalOwners)
                .totalListings(totalListings)
                .pendingListings(pendingListings)
                .monthlyRevenue(mockedMonthlyRevenue)
                .build();
    }
}