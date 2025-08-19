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
 * 勤務情報を表すエンティティ。
 * ユーザー、日付、勤務種別、時間帯、所属部署、臨時かどうかなどを保持する。
 */
@Entity
@Table(name = "shifts")
public class Shift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 勤務日
    private LocalDate date;

    // 対象ユーザー（正社員／パート／臨時）
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserProfile user;

    // 勤務種別（"日", "夜", "明", "休", "有", "臨(確)", "臨(自)"）
    private String shiftType;

    // 勤務時間帯（例: "9:00-14:00"）
    private String timeSlot;

    // 所属部署
    private String department;

    // 臨時職員かどうか
    private boolean isTemporary;

    // 臨時職員が事前に指定されていたかどうか
    private boolean isFixed;

    // ===== Getter・Setter =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public UserProfile getUser() { return user; }
    public void setUser(UserProfile user) { this.user = user; }

    public String getShiftType() { return shiftType; }
    public void setShiftType(String shiftType) { this.shiftType = shiftType; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public boolean isTemporary() { return isTemporary; }
    public void setTemporary(boolean temporary) { isTemporary = temporary; }

    public boolean isFixed() { return isFixed; }
    public void setFixed(boolean fixed) { isFixed = fixed; }
} 

