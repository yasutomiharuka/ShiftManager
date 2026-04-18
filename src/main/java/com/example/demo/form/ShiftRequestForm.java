package com.example.demo.form;

import java.time.LocalDate;

/**
 * 【シフト希望（休み／有給）登録フォーム】
 * シフト生成画面（generate.html）で、ユーザーが日付セルをクリックして入力した
 * 「希望休」「有給」の内容を、Controller が受け取るためのフォーム（画面入力の受け皿）
 *
 * - 目的：画面上の「希望休／有給入力モード」で選択された内容をPOSTで受け取る
 * - 用途：/api/shift/request/save などの保存APIに送る入力値を束ねる
 * - 注意：DBエンティティではなく、画面入力をそのまま受けるための“入力用モデル”
 *
 * 実務メモ：
 * - requestType は画面の表示記号（"休","有"）に合わせていますが、
 *   可能ならEnum化（REQUEST_OFF, PAID_LEAVE 等）してバリデーションを強化すると安全
 */
public class ShiftRequestForm {

    /**
     * 希望を登録する対象ユーザーID（シフト表の行＝従業員を識別）
     * 例：画面で「山田さん」の行を選択して入力した場合、その山田さんの userId が入る
     */
    private Long userId;

    /**
     * 希望を登録する日付（シフト表の列＝対象日）
     * 例：2026-01-15 のセルをクリックして「休」を入れた場合、2026-01-15 が入る
     */
    private LocalDate date;

    /**
     * 希望種別（画面入力の区分）
     * - "休"：希望休（公休扱い。ルール上の休みカウント対象）
     * - "有"：有給（有給扱い。休みカウントは仕様に合わせて別扱いにすることが多い）
     *
     * 実務では、想定外の値が来ないよう Controller/Service でチェック（ホワイトリスト）するのが定石。
     */
    private String requestType; // "休" or "有"

    /**
     * 所属部署（タブ/セレクトで選択された部署）
     * - 部署ごとに表示対象ユーザー・必要人員・保存先を切り替えるために使用
     * - 例："天美" / "本社" など
     *
     * 実務では departmentId（数値）を持たせ、表示名は別マスタ参照にすることも多い
     */
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
