package com.roomkh.backend.service;

import com.roomkh.backend.dto.analytics.PlatformGrowthDto;
import com.roomkh.backend.entity.PlatformDailyStat;
import com.roomkh.backend.repository.PlatformDailyStatRepository;
import com.roomkh.backend.repository.PropertyRepository;
import com.roomkh.backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardStatService {

    private final PlatformDailyStatRepository dailyStatRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;

    @Scheduled(cron = "0 1 0 * * ?")
    @Transactional
    public void generateDailyStats() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        LocalDateTime startOfDay = yesterday.atStartOfDay();
        LocalDateTime endOfDay = yesterday.atTime(LocalTime.MAX);

        OffsetDateTime startOffset = startOfDay.atZone(ZoneId.systemDefault()).toOffsetDateTime();
        OffsetDateTime endOffset = endOfDay.atZone(ZoneId.systemDefault()).toOffsetDateTime();

        try {
            long newUsers = userRepository.countByCreatedAtBetween(startOfDay, endOfDay);
            long newListings = propertyRepository.countByCreatedAtBetween(startOffset, endOffset);

            PlatformDailyStat stat = PlatformDailyStat.builder()
                    .recordDate(yesterday)
                    .newUsersCount((int) newUsers)
                    .newListingsCount((int) newListings)
                    .build();

            dailyStatRepository.save(stat);
            log.info("Successfully generated daily stats for: {}", yesterday);

        } catch (Exception e) {
            log.error("Failed to generate daily stats for {}: {}", yesterday, e.getMessage());
        }
    }

    public List<PlatformGrowthDto> getPlatformGrowthData(LocalDate startDate, LocalDate endDate) {
        List<PlatformDailyStat> stats = dailyStatRepository.findByRecordDateBetweenOrderByRecordDateAsc(startDate, endDate);

        return stats.stream().map(stat ->
                PlatformGrowthDto.builder()
                        .date(stat.getRecordDate())
                        .newUsers(stat.getNewUsersCount())
                        .newListings(stat.getNewListingsCount())
                        .build()
        ).collect(Collectors.toList());
    }
}