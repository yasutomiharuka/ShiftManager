package com.example.demo.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 各日付・時間帯・部署ごとの必要人員数を表すエンティティ。
 *
 * - DRAFT：入力途中（一時保存）
 * - CONFIRMED：確定（シフト生成で参照する正式値）
 *
 * 1セル（date + department + timeSlot）につき1レコードとし、
 * 保存時は上書き更新（アップサート）で扱う想定。
 * 
 * ※ UNIQUE（date, department, time_slot）もDBに付ける前提で、エンティティ側にも付けておきます。
 * 
 */
@Entity
@Table(
    name = "shift_requirements",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_shift_requirements_date_dept_slot",
        columnNames = { "date", "department", "time_slot" }
    )
)
public class ShiftRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // DBがINTEGERのため Integer に合わせる

    // 対象日（NOT NULL）
    @Column(nullable = false)
    private LocalDate date;

    // 所属部署（NOT NULL, VARCHAR(50)）
    @Column(nullable = false, length = 50)
    private String department;

    // 勤務時間帯（NOT NULL, VARCHAR(50)）
    @Column(name = "time_slot", nullable = false, length = 50)
    private String timeSlot;

    // 必要な人数（NOT NULL）
    @Column(name = "required_count", nullable = false)
    private Integer requiredCount;

    // 保存状態（DB側CHECKで DRAFT/CONFIRMED を制限）
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.DRAFT;

    public enum Status {
        DRAFT,      // 一時保存
        CONFIRMED   // 確定（生成に反映）
    }

    /** JPA用（必須） */
    protected ShiftRequirement() {}

    /**
     * 業務用途（新規作成/上書き更新時）
     *
     * @param date 対象日（必須）
     * @param department 所属部署（必須）
     * @param timeSlot 勤務時間帯（必須）
     * @param requiredCount 必要人数（必須）
     * @param status 保存状態（nullの場合はDRAFT）
     * 
     */
    public ShiftRequirement(LocalDate date, String department, String timeSlot, Integer requiredCount, Status status) {
        this.date = date;
        this.department = department;
        this.timeSlot = timeSlot;
        this.requiredCount = requiredCount;
        this.status = (status != null) ? status : Status.DRAFT;
    }

    /** 互換コンストラクタ（status省略時はDRAFT） */
    public ShiftRequirement(LocalDate date, String department, String timeSlot, Integer requiredCount) {
        this(date, department, timeSlot, requiredCount, Status.DRAFT);
    }

    // ===== Getter・Setter =====
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public Integer getRequiredCount() { return requiredCount; }
    public void setRequiredCount(Integer requiredCount) { this.requiredCount = requiredCount; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
