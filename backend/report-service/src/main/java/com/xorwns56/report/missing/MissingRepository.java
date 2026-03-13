package com.xorwns56.report.missing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissingRepository extends JpaRepository<Missing, Long> {

    List<Missing> findByTitleContainingIgnoreCase(String title, Sort sort);

    // 실종 신고 목록 조회 (검색 + 페이지네이션)
    Page<Missing> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    List<Missing> findByUserId(Long userId, Sort sort);

    // ID 목록으로 실종 신고 조회 (search-service 연동용)
    List<Missing> findByIdIn(List<Long> ids);

    // 회원 탈퇴 시 해당 유저의 실종 신고 전체 삭제
    void deleteAllByUserId(Long userId);
}
