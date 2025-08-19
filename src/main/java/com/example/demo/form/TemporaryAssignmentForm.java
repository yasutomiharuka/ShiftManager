package com.example.demo.form;

public class TemporaryAssignmentForm {

    private Long userId;         // ユーザーID
    private String date;         // 対象日（例："2025-06-15"）※フォームでは文字列で受け取る方が扱いやすい
    private String timeSlot;     // 勤務時間帯（例："9:00-13:00"）
    private String department;   // 所属部署（例："amami"）

    // --- Getter / Setter ---

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
