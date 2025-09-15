package com.example.demo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * ユーザーのシフト(日・夜・休・有など)を保持するエンティティ。
 */
@Entity
@Table(
    name = "shift_requests",
    // ▼ 同一ユーザー・同一日・同一部署で一意（重複登録防止）
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "date", "department"})
)
public class ShiftRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 対象ユーザー
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserProfile user;

    // 対象日
    private LocalDate date;

    // 申請種別（"休" または "有"）
    private String requestType;

    // 所属部署
    private String department;

    // =====  ステータス（DRAFT:一時保存 / CONFIRMED:確定） =====
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.DRAFT;

    // =====  監査項目（最終更新者・最終更新時刻） =====
    private String updatedBy;                 // 管理者IDや名前など（任意）
    private LocalDateTime updatedAt;

    // =====  自動で更新時刻を入れる簡易フック =====
    @PrePersist
    @PreUpdate
    public void touchTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    // ===== Getter・Setter =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserProfile getUser() { return user; }
    public void setUser(UserProfile user) { this.user = user; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ===== ステータス用のenum =====
    public enum RequestStatus {
        DRAFT,      // 一時保存
        CONFIRMED   // 確定（生成に反映）
    }
}
