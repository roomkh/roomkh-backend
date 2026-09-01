package com.roomkh.backend.service.impl;

import com.roomkh.backend.dto.admin.*;
import com.roomkh.backend.entity.*;
import com.roomkh.backend.exception.BadRequestException;
import com.roomkh.backend.exception.ResourceNotFoundException;
import com.roomkh.backend.repository.NotificationRepository;
import com.roomkh.backend.repository.PropertyRepository;
import com.roomkh.backend.repository.RoleRepository;
import com.roomkh.backend.repository.UserRepository;
import com.roomkh.backend.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {


    private static final java.util.regex.Pattern EMAIL_PATTERN = java.util.regex.Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final java.util.regex.Pattern CAMBODIA_E164_PHONE_PATTERN = java.util.regex.Pattern.compile("^\\+855[1-9]\\d{7,8}$");
    private static final java.util.regex.Pattern CAMBODIA_LOCAL_PHONE_PATTERN = java.util.regex.Pattern.compile("^0[1-9]\\d{7,8}$");

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminPropertyDetailResponse getPropertyDetail(Long id) {
        Property p = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        String combinedLocation = "";
        if (p.getDistrict() != null && !p.getDistrict().isEmpty()) {
            combinedLocation += p.getDistrict();
        }
        if (p.getProvince() != null && !p.getProvince().isEmpty()) {
            combinedLocation += (combinedLocation.isEmpty() ? "" : ", ") + p.getProvince();
        }

        String coverImage = null;
        java.util.List<String> allImages = new java.util.ArrayList<>();
        if (p.getImages() != null) {
            for (PropertyImage img : p.getImages()) {
                allImages.add(img.getUrl());
                if (img.isCover()) {
                    coverImage = img.getUrl();
                }
            }
        }

        java.util.List<String> amenityNames = new java.util.ArrayList<>();
        if (p.getAmenities() != null) {
            for (Amenity a : p.getAmenities()) {
                amenityNames.add(a.getName());
            }
        }

        return AdminPropertyDetailResponse.builder()
                .id(p.getId())
                .propertyCode("#LST-" + p.getId())
                .title(p.getTitle())
                .description(p.getDescription())
                .status(p.getStatus() != null ? p.getStatus().name() : "PENDING")
                .type(p.getPropertyType() != null ? p.getPropertyType().name() : "N/A")
                .purpose(p.getPurpose() != null ? p.getPurpose().name() : "N/A")
                .price(p.getPrice())
                .currency(p.getCurrency())
                .priceUnit(p.getPriceUnit() != null ? p.getPriceUnit().name() : "N/A")
                .location(combinedLocation.isEmpty() ? "Unknown" : combinedLocation)
                .address(p.getAddress())
                .bedrooms(p.getBedrooms())
                .bathrooms(p.getBathrooms())
                .sizeSqm(p.getSizeSqm())
                .floor(p.getFloor())
                .furnished(p.isFurnished())
                .ownerId(p.getSeller() != null ? "OWN-" + p.getSeller().getId() : "N/A")
                .ownerName(p.getSeller() != null ? p.getSeller().getFullName() : "Unknown")
                .ownerPhone(p.getSeller() != null ? p.getSeller().getPhoneNumber() : null)
                .ownerEmail(p.getSeller() != null ? p.getSeller().getEmail() : null)
                .ownerAvatarUrl(p.getSeller() != null ? p.getSeller().getAvatarUrl() : null)
                .coverImageUrl(coverImage)
                .imageUrls(allImages)
                .amenities(amenityNames)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPropertyStatsResponse getPropertyStats(LocalDate startDate, LocalDate endDate) {
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30); // Default to last 30 days
        }
        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        // Calculate days in the current period to find the exact previous period length
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;

        LocalDate previousEndDate = startDate.minusDays(1);
        LocalDate previousStartDate = previousEndDate.minusDays(daysBetween - 1);

        OffsetDateTime currentStart = startDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime currentEnd = endDate.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);

        OffsetDateTime previousStart = previousStartDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime previousEnd = previousEndDate.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);

        // Fetch counts for current period
        long currentTotal = propertyRepository.countByCreatedAtBetween(currentStart, currentEnd);
        long currentActive = propertyRepository.countByStatusAndCreatedAtBetween(PropertyStatus.ACTIVE, currentStart, currentEnd);
        long currentPending = propertyRepository.countByStatusAndCreatedAtBetween(PropertyStatus.PENDING, currentStart, currentEnd);
        long currentInactive = propertyRepository.countByStatusAndCreatedAtBetween(PropertyStatus.INACTIVE, currentStart, currentEnd);

        // Fetch counts for previous period
        long previousTotal = propertyRepository.countByCreatedAtBetween(previousStart, previousEnd);
        long previousActive = propertyRepository.countByStatusAndCreatedAtBetween(PropertyStatus.ACTIVE, previousStart, previousEnd);
        long previousPending = propertyRepository.countByStatusAndCreatedAtBetween(PropertyStatus.PENDING, previousStart, previousEnd);
        long previousInactive = propertyRepository.countByStatusAndCreatedAtBetween(PropertyStatus.INACTIVE, previousStart, previousEnd);

        return AdminPropertyStatsResponse.builder()
                .totalListings(currentTotal)
                .totalTrend(calculateTrend(currentTotal, previousTotal))
                .activeListings(currentActive)
                .activeTrend(calculateTrend(currentActive, previousActive))
                .pendingListings(currentPending)
                .pendingTrend(calculateTrend(currentPending, previousPending))
                .inactiveListings(currentInactive)
                .inactiveTrend(calculateTrend(currentInactive, previousInactive))
                .build();
    }

    private Double calculateTrend(long current, long previous) {
        double trend = previous == 0
                ? (current > 0 ? 100.0 : 0.0)
                : ((double) (current - previous) / previous) * 100.0;
        return Math.round(trend * 10.0) / 10.0;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminDashboardPropertyResponse> getProperties(String search, String status, String type, String city, LocalDate startDate, LocalDate endDate, int page, int size) {
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

        OffsetDateTime start = (startDate != null)
                ? startDate.atStartOfDay().atOffset(ZoneOffset.UTC)
                : OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC);

        OffsetDateTime end = (endDate != null)
                ? endDate.atTime(java.time.LocalTime.MAX).atOffset(ZoneOffset.UTC)
                : OffsetDateTime.now().plusYears(100);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Property> propertiesPage = propertyRepository.findPropertiesWithFilters(
                safeSearch, targetStatus, targetType, safeCity, start, end, pageable);

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
    public Page<AdminUserListItemResponse> getUsers(String search, RoleName role, String status, LocalDate startDate, LocalDate endDate, int page, int size) {
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

        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<User> usersPage = userRepository.findUsersWithAllFilters(
                safeSearch, role, statusType, pendingSellerStatus, targetAccountStatus, startDateTime, endDateTime, pageable);

        return usersPage.map(user -> AdminUserListItemResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole() != null && user.getRole().getName() != null ? user.getRole().getName().name() : null)
                .status(
                        (user.getSellerStatus() != null && user.getSellerStatus().name().equals("PENDING"))
                                ? "PENDING"
                                : (user.getAccountStatus() != null ? user.getAccountStatus().name() : null)
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

    @Override
    @Transactional
    public void softDeleteProperty(Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        property.setStatus(PropertyStatus.BANNED);
        property.setUpdatedAt(OffsetDateTime.now());
        propertyRepository.save(property);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPropertiesToExcel(String search, PropertyStatus status, PropertyType type, String city, LocalDate startDate, LocalDate endDate) throws IOException {
        String safeSearch = (search == null) ? "" : search.trim();
        String safeCity = (city == null) ? "" : city.trim();

        OffsetDateTime startOffsetDateTime = (startDate != null)
                ? startDate.atStartOfDay().atOffset(ZoneOffset.UTC)
                : OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        OffsetDateTime endOffsetDateTime = (endDate != null)
                ? endDate.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC)
                : OffsetDateTime.now();

        Page<Property> propertiesPage = propertyRepository.findPropertiesWithFilters(
                safeSearch, status, type, safeCity, startOffsetDateTime, endOffsetDateTime, Pageable.unpaged());
        List<Property> properties = propertiesPage.getContent();

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Properties");

            // Header Row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Property Code", "Title", "Owner Name", "Type", "Location", "Price", "Status", "Listed Date"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Data Rows
            int rowIdx = 1;
            for (Property property : properties) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(property.getId());
                row.createCell(1).setCellValue("#LST-" + property.getId());
                row.createCell(2).setCellValue(property.getTitle());

                String ownerName = property.getSeller() != null ? property.getSeller().getFullName() : "N/A";
                row.createCell(3).setCellValue(ownerName);

                row.createCell(4).setCellValue(property.getPropertyType() != null ? property.getPropertyType().name() : "N/A");

                String district = property.getDistrict() != null ? property.getDistrict() : "";
                String province = property.getProvince() != null ? property.getProvince() : "";
                String location = district + (district.isEmpty() || province.isEmpty() ? "" : ", ") + province;
                row.createCell(5).setCellValue(location.isEmpty() ? "Unknown" : location);

                row.createCell(6).setCellValue(property.getPrice() != null ? property.getPrice().doubleValue() : 0.0);
                row.createCell(7).setCellValue(property.getStatus() != null ? property.getStatus().name() : "N/A");
                row.createCell(8).setCellValue(property.getListedAt() != null ? property.getListedAt().toString() : "N/A");
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to export properties to Excel file", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportUsersToExcel(String search, RoleName role, String status, LocalDate startDate, LocalDate endDate) {
        String safeSearch = (search == null) ? "" : search.trim();

        String statusType = "ALL";
        AccountStatus targetAccountStatus = null;
        SellerStatus pendingSellerStatus = SellerStatus.PENDING;

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

        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        Page<User> usersPage = userRepository.findUsersWithAllFilters(
                safeSearch, role, statusType, pendingSellerStatus, targetAccountStatus, startDateTime, endDateTime, Pageable.unpaged());
        List<User> users = usersPage.getContent();

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Users");

            // Header Row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"User ID", "Name", "Email", "Phone", "Role", "Status", "Joined Date"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Data Rows
            int rowIdx = 1;
            for (User user : users) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue("USR-" + user.getId());
                row.createCell(1).setCellValue(user.getFullName() != null ? user.getFullName() : "N/A");
                row.createCell(2).setCellValue(user.getEmail() != null ? user.getEmail() : "N/A");
                row.createCell(3).setCellValue(user.getPhoneNumber() != null ? user.getPhoneNumber() : "N/A");
                String roleName = user.getRole() != null ? String.valueOf(user.getRole().getName()) : "N/A";
                row.createCell(4).setCellValue(roleName);
                row.createCell(5).setCellValue(user.getAccountStatus() != null ? user.getAccountStatus().name() : "N/A");
                row.createCell(6).setCellValue(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "N/A");
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to export users to Excel file", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserStatsResponse getUserStats(LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : LocalDate.of(2000, 1, 1);

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

        long totalUsers = userRepository.countByCreatedAtBetween(startDateTime, endDateTime);

        long totalOwners = userRepository.countByRoleNameAndCreatedAtBetween(RoleName.SELLER, startDateTime, endDateTime);

        long totalAgents = 0;

        long activeThisMonth = userRepository.countByAccountStatusAndCreatedAtBetween(AccountStatus.ACTIVE, startDateTime, endDateTime);

        long ownersPending = userRepository.countByRoleNameAndSellerStatusAndCreatedAtBetween(RoleName.SELLER, SellerStatus.PENDING, startDateTime, endDateTime);

        return AdminUserStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalOwners(totalOwners)
                .totalAgents(totalAgents)
                .activeThisMonth(activeThisMonth)
                .ownersPendingApproval(ownersPending)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminOwnerListItemResponse> getOwners(String search, String status, PlanType plan, LocalDate startDate, LocalDate endDate, int page, int size) {
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

        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<User> ownersPage = userRepository.findOwnersFiltered(
                safeSearch, statusType, pendingSellerStatus, targetAccountStatus, plan, startDateTime, endDateTime, pageable);

        return ownersPage.map(user -> AdminOwnerListItemResponse.builder()
                .id(user.getId())
                .ownerName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .plan(user.getPlanType() != null ? user.getPlanType().name() : "FREE")

                .propertiesCount(propertyRepository.countBySellerId(user.getId()))

                .joinDate(user.getCreatedAt())
                .status(
                        (user.getSellerStatus() != null && user.getSellerStatus().name().equals("PENDING"))
                                ? "PENDING"
                                : (user.getAccountStatus() != null ? user.getAccountStatus().name() : null)
                )
                .build());
    }


    @Override
    @Transactional(readOnly = true)
    public AdminOwnerStatsResponse getOwnerStats(LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : LocalDate.of(2000, 1, 1);

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

        long totalOwners = userRepository.countByRoleNameAndCreatedAtBetween(
                RoleName.SELLER, startDateTime, endDateTime);

        long activeOwners = userRepository.countByRoleNameAndAccountStatusAndCreatedAtBetween(
                RoleName.SELLER, AccountStatus.ACTIVE, startDateTime, endDateTime);

        long pendingOwners = userRepository.countByRoleNameAndSellerStatusAndCreatedAtBetween(
                RoleName.SELLER, SellerStatus.PENDING, startDateTime, endDateTime);

        // Assuming your enum for inactive users is SUSPENDED. Change to INACTIVE if that matches your AccountStatus enum.
        long inactiveOwners = userRepository.countByRoleNameAndAccountStatusAndCreatedAtBetween(
                RoleName.SELLER, AccountStatus.INACTIVE, startDateTime, endDateTime);

        long bannedOwners = userRepository.countByRoleNameAndAccountStatusAndCreatedAtBetween(
                RoleName.SELLER, AccountStatus.BANNED, startDateTime, endDateTime);
        inactiveOwners += bannedOwners;

        return AdminOwnerStatsResponse.builder()
                .totalOwners(totalOwners)
                .activeOwners(activeOwners)
                .pendingOwners(pendingOwners)
                .inactiveOwners(inactiveOwners)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public void exportOwnersToCsv(String search, String status, PlanType plan, LocalDate startDate, LocalDate endDate, java.io.Writer writer) throws java.io.IOException {
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

        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        Page<User> ownersPage = userRepository.findOwnersFiltered(
                safeSearch, statusType, pendingSellerStatus, targetAccountStatus, plan, startDateTime, endDateTime, Pageable.unpaged());

        writer.write("ID,Owner Name,Email,Phone,Plan,Properties Count,Join Date,Status\n");

        for (User user : ownersPage.getContent()) {
            String id = String.valueOf(user.getId());

            // Replacing commas in text fields to prevent CSV column breaking
            String name = user.getFullName() != null ? user.getFullName().replace(",", " ") : "";
            String email = user.getEmail() != null ? user.getEmail().replace(",", " ") : "";
            String phone = user.getPhoneNumber() != null ? user.getPhoneNumber().replace(",", " ") : "";

            String planName = user.getPlanType() != null ? user.getPlanType().name() : "FREE";
            String propertiesCount = String.valueOf(propertyRepository.countBySellerId(user.getId()));
            String joinDate = user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate().toString() : "";

            String userStatus = (user.getSellerStatus() != null && user.getSellerStatus().name().equals("PENDING"))
                    ? "PENDING"
                    : (user.getAccountStatus() != null ? user.getAccountStatus().name() : "");

            writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s\n",
                    id, name, email, phone, planName, propertiesCount, joinDate, userStatus));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AdminProfileResponse getAdminProfile(String identifier) {
        User admin = findUserByIdentifier(identifier);

        return AdminProfileResponse.builder()
                .id(admin.getId())
                .fullName(admin.getFullName())
                .email(admin.getEmail())
                .phoneNumber(admin.getPhoneNumber())
                .avatarUrl(admin.getAvatarUrl())
                .roleName(admin.getRole() != null ? admin.getRole().getName().name() : null)
                .accountStatus(admin.getAccountStatus() != null ? admin.getAccountStatus().name() : null)
                .authProvider(admin.getAuthProvider() != null ? admin.getAuthProvider().name() : null)
                .joinedDate(admin.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationItemResponse> getUnreadNotifications(String identifier) {
        User admin = findUserByIdentifier(identifier);

        List<Notification> unreadNotifications = notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(admin);

        return unreadNotifications.stream()
                .map(notification -> NotificationItemResponse.builder()
                        .id(notification.getId())
                        .title(notification.getTitle())
                        .message(notification.getMessage())
                        .createdAt(notification.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private User findUserByIdentifier(String identifier) {
        try {
            Long userId = Long.parseLong(identifier);
            return userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Admin not found with ID: " + userId));
        } catch (NumberFormatException e) {
            return userRepository.findByEmailIgnoreCase(identifier)
                    .orElseThrow(() -> new RuntimeException("Admin not found with email: " + identifier));
        }
    }

    @Override
    @Transactional
    public void createUser(AdminCreateUserRequest request) {
        String rawIdentifier = request.getIdentifier();
        String email = null;
        String phoneNumber = null;

        if (looksLikeEmail(rawIdentifier)) {
            email = normalizeEmail(rawIdentifier);
            if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
                throw new BadRequestException("Email already exists.");
            }
        } else {
            phoneNumber = normalizeCambodiaPhone(rawIdentifier);
            if (userRepository.findByPhoneNumber(phoneNumber).isPresent()) {
                throw new BadRequestException("Phone number already exists.");
            }
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
                .email(email)
                .phoneNumber(phoneNumber)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .authProvider(AuthProvider.LOCAL)
                .accountStatus(AccountStatus.ACTIVE)
                .sellerStatus(roleName == RoleName.SELLER ? SellerStatus.APPROVED : null)
                .build();

        userRepository.save(newUser);
    }

    // --- Helper Methods ---

    private boolean looksLikeEmail(String identifier) {
        return identifier != null && identifier.contains("@");
    }

    private String normalizeEmail(String rawEmail) {
        String trimmed = rawEmail.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new BadRequestException("Please provide a valid email address.");
        }
        return trimmed;
    }

    private String normalizeCambodiaPhone(String rawPhone) {
        String cleaned = rawPhone.trim().replaceAll("[\\s\\-().]", "");

        if (CAMBODIA_E164_PHONE_PATTERN.matcher(cleaned).matches()) {
            return cleaned;
        }

        if (CAMBODIA_LOCAL_PHONE_PATTERN.matcher(cleaned).matches()) {
            return "+855" + cleaned.substring(1);
        }

        throw new BadRequestException("Please provide a valid Cambodia phone number.");
    }
}