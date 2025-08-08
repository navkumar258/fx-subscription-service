package com.example.fx.subscription.service.dto.user;

import java.util.List;

public record UserListResponse(
        List<UserSummaryResponse> users,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
) {
}