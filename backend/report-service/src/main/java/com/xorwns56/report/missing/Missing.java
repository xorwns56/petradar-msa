package com.xorwns56.report.missing;

import com.xorwns56.report.report.Report;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "missing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Missing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MSA: User 엔티티 직접 참조 불가 → userId만 저장
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "pet_name", nullable = false)
    private String petName;

    @Column(name = "pet_type")
    private String petType;

    @Column(name = "pet_gender")
    private String petGender;

    @Column(name = "pet_breed")
    private String petBreed;

    @Column(name = "pet_age")
    private String petAge;

    @Column(name = "pet_missing_date")
    private String petMissingDate;

    @Column(name = "pet_missing_place")
    private String petMissingPlace;

    @Column(columnDefinition = "DECIMAL(10,8)")
    private Double latitude;

    @Column(columnDefinition = "DECIMAL(11,8)")
    private Double longitude;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Lob
    private String petImage;

    // 같은 서비스 내부이므로 JPA 연관관계 사용 가능
    @OneToMany(mappedBy = "missing", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Report> reports = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
