package com.roomkh.backend.service.impl;

import com.roomkh.backend.dto.admin.*;
import com.roomkh.backend.entity.*;
import com.roomkh.backend.exception.BadRequestException;
import com.roomkh.backend.exception.ResourceNotFoundException;
import com.roomkh.backend.repository.PropertyRepository;
import com.roomkh.backend.repository.RoleRepository;
import com.roomkh.backend.repository.UserRepository;
import com.roomkh.backend.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminDashboardPropertyResponse> getProperties(String search, String status, String type, String city, int page, int size) {
        if (page < 1) throw new IllegalArgumentException("page must be at least 1.");

        String safeSearch = (search == null) ? "" : search.trim();
        String safeCity = (city == null || city.equalsIgnoreCase("All Cities")) ? "" : city.trim();

        PropertyStatus targetStatus = null;
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("All Status")) {
            try {
                targetStatus = PropertyStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        PropertyType targetType = null;
        if (type != null && !type.trim().isEmpty() && !type.equalsIgnoreCase("All Types")) {
            try {
                targetType = PropertyType.valueOf(type.toUpperCase().replace(" ", "_"));
            } catch (IllegalArgumentException ignored) {}
        }

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Property> propertiesPage = propertyRepository.findPropertiesWithFilters(
                safeSearch, targetStatus, targetType, safeCity, pageable);

        return propertiesPage.map(p -> {
            String combinedLocation = "";
            if (p.getDistrict() != null && !p.getDistrict().isEmpty()) {
                combinedLocation += p.getDistrict();
            }
            if (p.getProvince() != null && !p.getProvince().isEmpty()) {
                combinedLocation += (combinedLocation.isEmpty() ? "" : ", ") + p.getProvince();
            }

            return AdminDashboardPropertyResponse.builder()
                    .id(p.getId())
                    .propertyCode("#LST-" + p.getId())
                    .title(p.getTitle())
                    .ownerName(p.getSeller() != null ? p.getSeller().getFullName() : "Unknown")
                    .ownerId(p.getSeller() != null ? "OWN-" + p.getSeller().getId() : "N/A")
                    .ownerAvatarUrl(p.getSeller() != null ? p.getSeller().getAvatarUrl() : null)
                    .type(p.getPropertyType() != null ? p.getPropertyType().name() : "N/A")
                    .location(combinedLocation.isEmpty() ? "Unknown" : combinedLocation)
                    .price(p.getPrice())
                    .status(p.getStatus() != null ? p.getStatus().name() : "PENDING")
                    .coverImageUrl(p.getImages() != null ?
                            p.getImages().stream()
                                    .filter(PropertyImage::isCover)
                                    .findFirst()
                                    .map(PropertyImage::getUrl)
                                    .orElse(null)
                            : null)
                    .build();
        });
    }

    @Override
    @Transactional
    public void createUser(AdminCreateUserRequest request) {
        if (userRepository.findByEmailIgnoreCase(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists.");
        }

        RoleName roleName;
        try {
            roleName = RoleName.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role. Allowed values: USER, SELLER, ADMIN");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        User newUser = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(role)
                .authProvider(AuthProvider.LOCAL)
                .accountStatus(AccountStatus.ACTIVE)
                .sellerStatus(roleName == RoleName.SELLER ? SellerStatus.APPROVED : null)
                .build();

        userRepository.save(newUser);
    }

    @Override
    @Transactional
    public void updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String action = request.getAction().toUpperCase();

        switch (action) {
            case "ACTIVATE":
                user.setAccountStatus(AccountStatus.ACTIVE);
                break;
            case "INACTIVE":
                user.setAccountStatus(AccountStatus.INACTIVE);
                break;
            case "BAN":
                user.setAccountStatus(AccountStatus.BANNED);
                break;
            case "APPROVE_SELLER":
                if (user.getSellerStatus() == null) {
                    throw new IllegalArgumentException("Cannot approve: User is not a seller.");
                }
                user.setSellerStatus(SellerStatus.APPROVED);
                break;
            case "REJECT_SELLER":
                if (user.getSellerStatus() == null) {
                    throw new IllegalArgumentException("Cannot reject: User is not a seller.");
                }
                user.setSellerStatus(SellerStatus.REJECTED);
                break;
            default:
                throw new IllegalArgumentException("Invalid action. Allowed values: ACTIVATE, INACTIVE, BAN, APPROVE_SELLER, REJECT_SELLER");
        }

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserListItemResponse> getUsers(String search, RoleName role, String status, int page, int size) {
        if (page < 1) throw new IllegalArgumentException("page must be at least 1.");

        String safeSearch = (search == null) ? "" : search.trim();

        String statusType = "ALL";
        com.roomkh.backend.entity.AccountStatus targetAccountStatus = null;
        com.roomkh.backend.entity.SellerStatus pendingSellerStatus = com.roomkh.backend.entity.SellerStatus.PENDING;

        if (status != null && !status.trim().isEmpty()) {
            if (status.equalsIgnoreCase("PENDING")) {
                statusType = "PENDING";
            } else {
                statusType = "ACCOUNT";
                try {
                    targetAccountStatus = com.roomkh.backend.entity.AccountStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    statusType = "ALL";
                }
            }
        }

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<User> usersPage = userRepository.findUsersWithAllFilters(
                safeSearch, role, statusType, pendingSellerStatus, targetAccountStatus, pageable);

        return usersPage.map(user -> AdminUserListItemResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole() != null && user.getRole().getName() != null ? user.getRole().getName().name() : null)
                .status(
                        (user.getSellerStatus() != null && user.getSellerStatus().name().equals("PENDING"))
                                ? "PENDING"
                                : user.getAccountStatus().name()
                )
                .joinedDate(user.getCreatedAt())
                .build());
    }

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