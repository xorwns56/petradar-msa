package com.xorwns56.report.missing;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissingRepository extends JpaRepository<Missing, Long> {

    List<Missing> findByTitleContainingIgnoreCase(String title, Sort sort);

    List<Missing> findByUserId(Long userId, Sort sort);

    // 회원 탈퇴 시 해당 유저의 실종 신고 전체 삭제
    void deleteAllByUserId(Long userId);
}
