package com.roomkh.backend.service.impl;

import com.roomkh.backend.dto.seller.CreateSellerRequest;
import com.roomkh.backend.dto.seller.SellerRequestResponse;
import com.roomkh.backend.dto.seller.SellerRequestSummaryResponse;
import com.roomkh.backend.entity.RoleName;
import com.roomkh.backend.entity.SellerRequest;
import com.roomkh.backend.entity.SellerRequestStatus;
import com.roomkh.backend.entity.User;
import com.roomkh.backend.exception.BadRequestException;
import com.roomkh.backend.exception.DuplicateResourceException;
import com.roomkh.backend.exception.ResourceNotFoundException;
import com.roomkh.backend.mapper.SellerRequestMapper;
import com.roomkh.backend.repository.SellerRequestRepository;
import com.roomkh.backend.repository.UserRepository;
import com.roomkh.backend.service.SellerRequestRateLimitService;
import com.roomkh.backend.service.SellerRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SellerRequestServiceImpl implements SellerRequestService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern CAMBODIA_LOCAL_PHONE_PATTERN = Pattern.compile("^0\\d{8,9}$");
    private static final Pattern CAMBODIA_E164_PHONE_PATTERN = Pattern.compile("^\\+855\\d{8,9}$");

    private final SellerRequestRepository sellerRequestRepository;
    private final UserRepository userRepository;
    private final SellerRequestMapper sellerRequestMapper;
    private final SellerRequestRateLimitService sellerRequestRateLimitService;

    @Override
    @Transactional
    public SellerRequestResponse submit(CreateSellerRequest request, Long authenticatedUserId, String clientIp) {
        sellerRequestRateLimitService.checkAndRecordAttempt(clientIp, authenticatedUserId);

        User user = null;

        if (authenticatedUserId != null) {
            user = userRepository.findById(authenticatedUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));

            RoleName role = user.getRole().getName();
            if (role == RoleName.SELLER || role == RoleName.ADMIN) {
                throw new BadRequestException("Only USER accounts can submit a seller request.");
            }

            if (sellerRequestRepository.existsByUser_IdAndStatus(user.getId(), SellerRequestStatus.PENDING)) {
                throw new DuplicateResourceException("You already have a pending seller request.");
            }
        }

        String normalizedPhone = normalizeCambodiaPhone(request.getPhoneNumber());
        String normalizedEmail = (request.getEmail() != null && !request.getEmail().isBlank())
                ? normalizeEmail(request.getEmail())
                : null;

        if (user == null && sellerRequestRepository.existsByPhoneNumberAndStatus(normalizedPhone, SellerRequestStatus.PENDING)) {
            throw new DuplicateResourceException("A pending seller request already exists for this phone number.");
        }

        SellerRequest sellerRequest = SellerRequest.builder()
                .user(user)
                .fullName(request.getFullName())
                .email(normalizedEmail)
                .phoneNumber(normalizedPhone)
                .position(request.getPosition())
                .businessName(request.getBusinessName())
                .reason(request.getReason())
                .termsAccepted(request.isTermsAccepted())
                .status(SellerRequestStatus.PENDING)
                .build();

        SellerRequest saved = sellerRequestRepository.save(sellerRequest);
        return sellerRequestMapper.toResponse(saved);
    }

    @Override
    public Page<SellerRequestSummaryResponse> list(SellerRequestStatus status, String keyword, Pageable pageable) {
        Specification<SellerRequest> spec = Specification.where((Specification<SellerRequest>) null);

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        if (keyword != null && !keyword.isBlank()) {
            String likePattern = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), likePattern),
                    cb.like(cb.lower(cb.coalesce(root.get("email"), "")), likePattern),
                    cb.like(cb.lower(root.get("phoneNumber")), likePattern),
                    cb.like(cb.lower(cb.coalesce(root.get("businessName"), "")), likePattern)
            ));
        }

        return sellerRequestRepository.findAll(spec, pageable).map(sellerRequestMapper::toSummaryResponse);
    }

    @Override
    public SellerRequestResponse getById(Long id) {
        SellerRequest sellerRequest = sellerRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seller request not found."));
        return sellerRequestMapper.toResponse(sellerRequest);
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