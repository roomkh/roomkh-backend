package com.roomkh.backend.service.impl;

import com.roomkh.backend.dto.seller.SellerActivationResponse;
import com.roomkh.backend.dto.seller.SellerRequestApprovalResponse;
import com.roomkh.backend.entity.*;
import com.roomkh.backend.exception.BadRequestException;
import com.roomkh.backend.exception.DuplicateResourceException;
import com.roomkh.backend.exception.ResourceNotFoundException;
import com.roomkh.backend.repository.RoleRepository;
import com.roomkh.backend.repository.SellerRequestRepository;
import com.roomkh.backend.repository.UserRepository;
import com.roomkh.backend.service.SellerRequestApprovalService;
import com.roomkh.backend.service.SellerRequestOtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class SellerRequestApprovalServiceImpl implements SellerRequestApprovalService {

    private final SellerRequestRepository sellerRequestRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SellerRequestOtpService sellerRequestOtpService;

    @Override
    @Transactional
    public SellerRequestApprovalResponse approve(Long sellerRequestId, String adminNote, Long adminUserId) {
        SellerRequest sellerRequest = sellerRequestRepository.findByIdForUpdate(sellerRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller request not found."));

        if (sellerRequest.getStatus() != SellerRequestStatus.PENDING) {
            throw new DuplicateResourceException("Only PENDING seller requests can be approved.");
        }

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found."));

        OffsetDateTime now = OffsetDateTime.now();

        if (sellerRequest.getUser() != null) {
            User owner = sellerRequest.getUser();

            if (owner.getRole().getName() != RoleName.USER) {
                throw new DuplicateResourceException("Linked user does not have the USER role.");
            }

            Role sellerRole = roleRepository.findByName(RoleName.SELLER)
                    .orElseThrow(() -> new ResourceNotFoundException("SELLER role is not configured."));

            owner.setRole(sellerRole);
            userRepository.save(owner);

            sellerRequest.setStatus(SellerRequestStatus.APPROVED);
            sellerRequest.setAdminNote(adminNote);
            sellerRequest.setReviewedAt(now);
            sellerRequest.setReviewedBy(adminUser);
            sellerRequestRepository.save(sellerRequest);

            return SellerRequestApprovalResponse.builder()
                    .sellerRequestId(sellerRequest.getId())
                    .status(sellerRequest.getStatus())
                    .userId(owner.getId())
                    .role(sellerRole.getName().name())
                    .build();
        }

        if (userRepository.existsByPhoneNumber(sellerRequest.getPhoneNumber())) {
            throw new DuplicateResourceException("A user account already exists with this phone number.");
        }

        if (sellerRequest.getEmail() != null && userRepository.existsByEmailIgnoreCase(sellerRequest.getEmail())) {
            throw new DuplicateResourceException("A user account already exists with this email.");
        }

        sellerRequest.setStatus(SellerRequestStatus.APPROVED_PENDING_ACTIVATION);
        sellerRequest.setAdminNote(adminNote);
        sellerRequest.setReviewedAt(now);
        sellerRequest.setReviewedBy(adminUser);
        SellerRequest savedRequest = sellerRequestRepository.save(sellerRequest);

        SellerRequestOtpCode otp = sellerRequestOtpService.generateAndSendOtp(savedRequest);

        return SellerRequestApprovalResponse.builder()
                .sellerRequestId(savedRequest.getId())
                .status(savedRequest.getStatus())
                .otpExpiresAt(otp.getExpiresAt())
                .build();
    }

    @Override
    @Transactional
    public SellerRequestApprovalResponse reject(Long sellerRequestId, String adminNote, Long adminUserId) {
        if (adminNote == null || adminNote.trim().length() < 5) {
            throw new BadRequestException("Admin note must be at least 5 characters when rejecting a request.");
        }

        SellerRequest sellerRequest = sellerRequestRepository.findByIdForUpdate(sellerRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller request not found."));

        if (sellerRequest.getStatus() != SellerRequestStatus.PENDING) {
            throw new DuplicateResourceException("Only PENDING seller requests can be rejected.");
        }

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found."));

        sellerRequest.setStatus(SellerRequestStatus.REJECTED);
        sellerRequest.setAdminNote(adminNote.trim());
        sellerRequest.setReviewedAt(OffsetDateTime.now());
        sellerRequest.setReviewedBy(adminUser);
        SellerRequest saved = sellerRequestRepository.save(sellerRequest);

        return SellerRequestApprovalResponse.builder()
                .sellerRequestId(saved.getId())
                .status(saved.getStatus())
                .build();
    }

    @Override
    @Transactional
    public SellerRequestApprovalResponse resendActivationOtp(Long sellerRequestId) {
        SellerRequest sellerRequest = sellerRequestRepository.findByIdForUpdate(sellerRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller request not found."));

        if (sellerRequest.getUser() != null) {
            throw new BadRequestException("OTP resend is only available for guest seller requests.");
        }

        if (sellerRequest.getStatus() != SellerRequestStatus.APPROVED_PENDING_ACTIVATION) {
            throw new DuplicateResourceException("Only requests awaiting activation can have their OTP resent.");
        }

        SellerRequestOtpCode otp = sellerRequestOtpService.resendOtp(sellerRequest);

        return SellerRequestApprovalResponse.builder()
                .sellerRequestId(sellerRequest.getId())
                .status(sellerRequest.getStatus())
                .otpExpiresAt(otp.getExpiresAt())
                .build();
    }

    @Override
    @Transactional
    public SellerActivationResponse activate(Long sellerRequestId, String otpCode, String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new BadRequestException("Password confirmation does not match.");
        }

        SellerRequest sellerRequest = sellerRequestRepository.findByIdForUpdate(sellerRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller request not found."));

        if (sellerRequest.getUser() != null) {
            throw new BadRequestException("This seller request does not require activation.");
        }

        if (sellerRequest.getStatus() != SellerRequestStatus.APPROVED_PENDING_ACTIVATION) {
            throw new DuplicateResourceException("This seller request is not awaiting activation.");
        }

        sellerRequestOtpService.verifyOtp(sellerRequest, otpCode);

        if (userRepository.existsByPhoneNumber(sellerRequest.getPhoneNumber())) {
            throw new DuplicateResourceException("A user account already exists with this phone number.");
        }

        if (sellerRequest.getEmail() != null && userRepository.existsByEmailIgnoreCase(sellerRequest.getEmail())) {
            throw new DuplicateResourceException("A user account already exists with this email.");
        }

        Role sellerRole = roleRepository.findByName(RoleName.SELLER)
                .orElseThrow(() -> new ResourceNotFoundException("SELLER role is not configured."));

        User newUser = User.builder()
                .fullName(sellerRequest.getFullName())
                .email(sellerRequest.getEmail())
                .phoneNumber(sellerRequest.getPhoneNumber())
                .password(passwordEncoder.encode(password))
                .authProvider(AuthProvider.LOCAL)
                .accountStatus(AccountStatus.ACTIVE)
                .sellerStatus(SellerStatus.APPROVED)
                .role(sellerRole)
                .build();

        User savedUser = userRepository.save(newUser);

        sellerRequest.setUser(savedUser);
        sellerRequest.setStatus(SellerRequestStatus.APPROVED);
        sellerRequestRepository.save(sellerRequest);

        return SellerActivationResponse.builder()
                .userId(savedUser.getId())
                .phoneNumber(savedUser.getPhoneNumber())
                .role(sellerRole.getName().name())
                .build();
    }
}