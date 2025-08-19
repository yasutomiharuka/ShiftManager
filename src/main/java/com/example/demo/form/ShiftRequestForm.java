package com.example.demo.form;

import java.time.LocalDate;

public class ShiftRequestForm {
    private Long userId;
    private LocalDate date;
    private String requestType; // "休" or "有"
    private String department;

    // getter/setter
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
