package com.example.demo.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * ユーザーの希望休や有給の申請情報を保持するエンティティ。
 */
@Entity
@Table(name = "shift_requests")
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
}
