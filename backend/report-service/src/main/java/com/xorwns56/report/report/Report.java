package com.xorwns56.report.report;

import com.xorwns56.report.missing.Missing;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MSA: User 엔티티 직접 참조 불가 → userId만 저장 (비회원 제보 시 null 허용)
    @Column(name = "user_id")
    private Long userId;

    // 같은 서비스 내부이므로 JPA 연관관계 사용 가능
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "missing_id", nullable = false)
    private Missing missing;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Lob
    private String petImage;

    @Column(name = "pet_report_place")
    private String petReportPlace;

    @Column(columnDefinition = "DECIMAL(10,8)")
    private Double latitude;

    @Column(columnDefinition = "DECIMAL(11,8)")
    private Double longitude;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
