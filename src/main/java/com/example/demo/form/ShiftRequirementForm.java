// src/main/java/com/example/demo/form/ShiftRequirementForm.java
package com.example.demo.form;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 【必要人員入力フォーム（1ヶ月分）】
 *
 * ・シフト生成画面（generate.html）の「必要人員」入力テーブル用フォームオブジェクト。
 * ・1ヶ月分の「部署 × 年月 × 日付×時間帯ごとの必要人数」をまとめて保持する。
 *
 * ＜なぜ Map で持つのか＞
 * ・画面は「日付×時間帯」の二重ループ構造になりやすく、List（行・列）の index で管理すると
 *   並び順ズレや初期値の詰め戻し漏れが起きやすい。
 * ・Map にすると、キー（date + timeSlot）で確実に参照できるため
 *   「保存後に再描画しても表示されない」事故を防ぎやすい。
 *
 * ＜想定フロー＞
 *  1. ShiftRequirementService#buildForm(department, month) で DBの ShiftRequirement を取得し、
 *     requiredCounts に詰めた状態で本フォームを生成して generate.html へ渡す。
 *  2. 画面で入力 → 「必要人員を保存」ボタン押下 → /shift/requirement/save にPOSTされる。
 *  3. ShiftRequirementService#saveFromForm(…) で requiredCounts の全要素を走査して
 *     ShiftRequirement（date / department / timeSlot / requiredCount）へ反映する。
 *
 * ＜キー仕様＞
 * ・キーは "yyyy-MM-dd_timeSlot"（例： "2026-01-15_9:00-14:00" ）
 * ・timeSlot は DB保存値と完全一致させる（末尾スペース等の揺れがあると表示できないため）。
 */
public class ShiftRequirementForm {

    /** 対象部署（例：amami / honsha など）。画面の所属タブの選択内容と対応する。 */
    private String department;

    /** 対象年（例：2026） */
    private int year;

    /** 対象月（1〜12） */
    private int month;

    /**
     * 必要人数の保持領域。
     * key:  "yyyy-MM-dd_timeSlot"
     * value: 必要人数（null も許容。未入力＝null、入力済＝数値 のように扱える）
     */
    private Map<String, Integer> requiredCounts = new HashMap<>();

    // ===== Getter / Setter =====

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public Map<String, Integer> getRequiredCounts() { return requiredCounts; }
    public void setRequiredCounts(Map<String, Integer> requiredCounts) {
        this.requiredCounts = (requiredCounts == null) ? new HashMap<>() : requiredCounts;
    }

    // ===== Utility =====

    /**
     * 画面・サービスで統一して使うためのキー生成。
     * 例）2026-01-15 + "9:00-14:00" → "2026-01-15_9:00-14:00"
     */
    public static String toKey(LocalDate date, String timeSlot) {
        return date.toString() + "_" + timeSlot;
    }
}
