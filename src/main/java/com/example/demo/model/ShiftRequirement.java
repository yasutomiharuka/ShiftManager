package com.example.demo.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 各日付・時間帯・部署ごとの必要人員数を表すエンティティ。
 */
@Entity
@Table(name = "shift_requirements")
public class ShiftRequirement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 対象日
    private LocalDate date;

    // 所属部署
    private String department;

    // 勤務時間帯
    private String timeSlot;

    // 必要な人数
    private int requiredCount;

    // ===== Getter・Setter =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public int getRequiredCount() { return requiredCount; }
    public void setRequiredCount(int requiredCount) { this.requiredCount = requiredCount; }
}
