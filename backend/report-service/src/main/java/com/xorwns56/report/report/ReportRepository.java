package com.xorwns56.report.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByMissingId(Long missingId, Pageable pageable);

    // 회원 탈퇴 시 해당 유저의 목격 제보 전체 삭제
    void deleteAllByUserId(Long userId);
}
