package com.example.demo.form;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * シフト生成画面から送信されるフォームオブジェクト。
 * 希望休・臨時職員・シフト入力をまとめて受け取る。
 */
public class ShiftGenerationForm {

    // 希望休・有給の入力（shiftRequests[0].userId などでフォームにバインド可能）
    private List<ShiftRequestForm> shiftRequests;

    // 臨時職員指定の入力
    private List<TemporaryAssignmentForm> temporaryAssignments;

    // シフト生成対象の月
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth targetMonth;

    // 所属部署（例: "amami", "main"）
    private String department;

    // ▼ 追加: ユーザー×日付のシフト入力結果
    // HTML側では name="shifts[123_2025-09-11]" value="日" のように送信される想定
    private Map<String, String> shifts;

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

    // ▼ 新規追加: shifts の Getter / Setter
    public Map<String, String> getShifts() {
        return shifts;
    }

    public void setShifts(Map<String, String> shifts) {
        this.shifts = shifts;
    }
}
