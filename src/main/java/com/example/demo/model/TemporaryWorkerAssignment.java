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
 * 臨時職員の事前出勤予定を保持するエンティティ。
 */
@Entity
@Table(name = "temporary_worker_assignments")
public class TemporaryWorkerAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 臨時出勤するユーザー
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserProfile user;

    // 出勤日
    private LocalDate date;

    // 所属部署
    private String department;

    // 勤務時間帯
    private String timeSlot;

    // 事前指定フラグ（常に true）
    private boolean isFixed = true;

    // ===== Getter・Setter =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserProfile getUser() { return user; }
    public void setUser(UserProfile user) { this.user = user; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public boolean isFixed() { return isFixed; }
    public void setFixed(boolean fixed) { isFixed = fixed; }
}
