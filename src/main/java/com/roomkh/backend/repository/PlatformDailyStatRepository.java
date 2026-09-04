package com.roomkh.backend.repository;

import com.roomkh.backend.entity.PlatformDailyStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PlatformDailyStatRepository extends JpaRepository<PlatformDailyStat, Long> {
    List<PlatformDailyStat> findByRecordDateBetweenOrderByRecordDateAsc(LocalDate startDate, LocalDate endDate);
}