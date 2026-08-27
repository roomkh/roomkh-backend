package com.roomkh.backend.service;

import com.roomkh.backend.dto.property.AdminPropertyListItemResponse;
import org.springframework.data.domain.Page;

public interface AdminPropertyService {
    Page<AdminPropertyListItemResponse> listProperties(String status, int page, int size, String sortBy);
}