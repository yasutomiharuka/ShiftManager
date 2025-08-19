package com.example.demo.form;

import java.time.YearMonth;
import java.util.List;

public class ShiftGenerationForm {

    // 希望休・有給の入力（shiftRequests[0].userId などでフォームにバインド可能）
    private List<ShiftRequestForm> shiftRequests;

    // 臨時職員指定の入力
    private List<TemporaryAssignmentForm> temporaryAssignments;

    // シフト生成対象の月
    private YearMonth targetMonth;

    // 所属部署（例: "amami", "main"）
    private String department;

    // --- Getter / Setter ---

    public List<ShiftRequestForm> getShiftRequests() {
        return shiftRequests;
    }

    public void setShiftRequests(List<ShiftRequestForm> shiftRequests) {
        this.shiftRequests = shiftRequests;
    }

    public List<TemporaryAssignmentForm> getTemporaryAssignments() {
        return temporaryAssignments;
    }

    public void setTemporaryAssignments(List<TemporaryAssignmentForm> temporaryAssignments) {
        this.temporaryAssignments = temporaryAssignments;
    }

    public YearMonth getTargetMonth() {
        return targetMonth;
    }

    public void setTargetMonth(YearMonth targetMonth) {
        this.targetMonth = targetMonth;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
