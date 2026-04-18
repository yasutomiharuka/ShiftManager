// src/main/java/com/example/demo/form/ShiftRequirementRow.java
package com.example.demo.form;

import java.time.LocalDate;

/**
 * 【必要人員入力フォーム（1日分の行）】
 *
 * ・シフト生成画面（generate.html）で使用する「必要人員」入力用フォームオブジェクト。
 * ・1レコード＝ある1日分の「日付 + 4つの時間帯ごとの必要人数」を表す。
 *
 * ＜主な利用箇所＞
 * ・ShiftRequirementForm の rows 要素として、1ヶ月分（日数分）の行リストとして保持される。
 * ・ShiftRequirementService#buildForm(...) で初期値がセットされ、画面に表示される。
 * ・シフト生成画面からPOSTされた値を ShiftRequirementService#saveFromForm(...) で
 *   ShiftRequirement エンティティ（date / department / timeSlot / requiredCount）へ反映する。
 */
public class ShiftRequirementRow {

    /** 対象日（例：2025-12-01）。テーブルの左端の「日付」列に相当する。 */
    private LocalDate date;          // その行の日付

    /** 9:00-14:00 の時間帯に必要な人数 */
    private Integer count0900_1400;  // 9:00-14:00

    /** 14:00-16:00 の時間帯に必要な人数 */
    private Integer count1400_1600;  // 14:00-16:00

    /** 16:00-18:00 の時間帯に必要な人数 */
    private Integer count1600_1800;  // 16:00-18:00

    /** 夜間帯に必要な人数（時間帯の詳細は画面側の定義に従う） */
    private Integer countNight;      // 夜間帯

    // ===== Getter / Setter =====

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Integer getCount0900_1400() { return count0900_1400; }
    public void setCount0900_1400(Integer count0900_1400) { this.count0900_1400 = count0900_1400; }

    public Integer getCount1400_1600() { return count1400_1600; }
    public void setCount1400_1600(Integer count1400_1600) { this.count1400_1600 = count1400_1600; }

    public Integer getCount1600_1800() { return count1600_1800; }
    public void setCount1600_1800(Integer count1600_1800) { this.count1600_1800 = count1600_1800; }

    public Integer getCountNight() { return countNight; }
    public void setCountNight(Integer countNight) { this.countNight = countNight; }
}
