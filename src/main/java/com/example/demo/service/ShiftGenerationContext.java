package com.example.demo.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.demo.model.Shift;
import com.example.demo.model.ShiftRequest;
import com.example.demo.model.ShiftRequirement;
import com.example.demo.model.TemporaryWorkerAssignment;
import com.example.demo.model.UserProfile;

/**
 * シフト自動生成処理で使用する入力データ・中間データをまとめるクラス。
 */
public class ShiftGenerationContext {

    private String department;
    private YearMonth targetMonth;
    private List<LocalDate> dates;
    private List<UserProfile> users;
    private List<Shift> existingShifts;
    private List<ShiftRequirement> requirements;
    private List<ShiftRequest> requests;
    private List<TemporaryWorkerAssignment> temporaryAssignments;

    /**
     * 生成処理用のシフトマップ。
     * userId -> date -> shiftType
     */
    private Map<Long, Map<LocalDate, String>> scheduleMap = new HashMap<>();

    /**
     * 手入力など、自動生成で上書きしてはいけないセル。
     * key = userId_yyyy-MM-dd
     */
    private Map<String, Boolean> lockedMap = new HashMap<>();

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public YearMonth getTargetMonth() {
        return targetMonth;
    }

    public void setTargetMonth(YearMonth targetMonth) {
        this.targetMonth = targetMonth;
    }

    public List<LocalDate> getDates() {
        return dates;
    }

    public void setDates(List<LocalDate> dates) {
        this.dates = dates;
    }

    public List<UserProfile> getUsers() {
        return users;
    }

    public void setUsers(List<UserProfile> users) {
        this.users = users;
    }

    public List<Shift> getExistingShifts() {
        return existingShifts;
    }

    public void setExistingShifts(List<Shift> existingShifts) {
        this.existingShifts = existingShifts;
    }

    public List<ShiftRequirement> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<ShiftRequirement> requirements) {
        this.requirements = requirements;
    }

    public List<ShiftRequest> getRequests() {
        return requests;
    }

    public void setRequests(List<ShiftRequest> requests) {
        this.requests = requests;
    }

    public List<TemporaryWorkerAssignment> getTemporaryAssignments() {
        return temporaryAssignments;
    }

    public void setTemporaryAssignments(List<TemporaryWorkerAssignment> temporaryAssignments) {
        this.temporaryAssignments = temporaryAssignments;
    }

    public Map<Long, Map<LocalDate, String>> getScheduleMap() {
        return scheduleMap;
    }

    public void setScheduleMap(Map<Long, Map<LocalDate, String>> scheduleMap) {
        this.scheduleMap = scheduleMap;
    }

    public Map<String, Boolean> getLockedMap() {
        return lockedMap;
    }

    public void setLockedMap(Map<String, Boolean> lockedMap) {
        this.lockedMap = lockedMap;
    }
}