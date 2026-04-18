// src/main/java/com/example/demo/service/ShiftRequirementService.java
package com.example.demo.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.form.ShiftRequirementForm;
import com.example.demo.model.ShiftRequirement;
import com.example.demo.repository.ShiftRequirementRepository;

/**
 * 【必要人員（ShiftRequirement）に関する業務ロジックを担当するサービス】
 *
 * ■役割
 * ・各日付・時間帯・部署ごとの「必要人員数」を取得/保存する。
 * ・generate.html の入力フォーム（ShiftRequirementForm）との相互変換（詰め戻し/保存）を担当する。
 *
 * ■重要ポイント（表示されない問題の根本対策）
 * ・generate.html は th:field でフォーム（requiredCounts）を参照して表示するため、
 *   GET表示時に「DBの値をフォームへ詰め戻す」必要がある。
 * ・List(rows) の index 管理はズレやすいので、date+timeSlot をキーにした Map(requiredCounts) で管理する。
 *
 * ■状態管理（DRAFT / CONFIRMED）
 * ・本設計は「1セル=1レコード（date+department+timeSlotで一意）」の運用。
 * ・status は「そのレコードがいま確定扱いか、下書き扱いか」を表すフラグ。
 *   ※「確定と下書きを同時に2本保持する」設計ではない（ユニーク制約が status を含まないため）。
 */
@Service
public class ShiftRequirementService {

    private final ShiftRequirementRepository shiftRequirementRepository;

    /** 画面・DBで共通に扱う時間帯コード（DB保存値と完全一致させること） */
    public static final List<String> TIME_SLOTS = Arrays.asList(
            "09-14",
            "14-16",
            "16-18",
            "NIGHT"
    );

    public ShiftRequirementService(ShiftRequirementRepository shiftRequirementRepository) {
        this.shiftRequirementRepository = shiftRequirementRepository;
    }

    // ======================================================================
    // 参照系（画面表示用）
    // ======================================================================

    /**
     * 指定した部署・年月の「日付×時間帯ごとの必要人員」を取得する。
     *
     * 戻り値の構造：
     *  Map<LocalDate, Map<String, Integer>>
     *   - 第一キー：日付（例: 2026-01-01）
     *   - 第二キー：timeSlot（例: "09-14"）
     *   - 値：requiredCount（必要人数）
     *
     * 【基本方針（運用ルール）】
     * ・本システムは「1セル（date + department + timeSlot）= 1レコード」を前提とし、
     *   一時保存/確定は status を上書き更新する運用（確定で上書き）とする。
     *
     * 【表示ルール（実務上の安全策）】
     * ・原則は上記のとおりレコードは1件のはずだが、データ移行・手動更新・制約漏れ等により
     *   同一セルのデータが混在した場合でも画面表示がブレないよう、取得結果は
     *   「CONFIRMED が存在するセルは CONFIRMED を優先、なければ DRAFT を採用」
     *   という優先順位でマッピングして返す。
     *
     * ※「CONFIRMEDのみ表示したい」等の要件が将来出た場合は、
     *   Repository 側で status 条件を追加して絞り込む方式に切り替える。
     */

    public Map<LocalDate, Map<String, Integer>> getRequirementMap(String department, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        List<ShiftRequirement> entities =
                shiftRequirementRepository.findByDepartmentAndDateBetween(department, start, end);

        // 日付順に扱いたいため LinkedHashMap を使用（全日付をキーとして持つ）
        Map<LocalDate, Map<String, Integer>> result = new LinkedHashMap<>();
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            result.put(month.atDay(day), new LinkedHashMap<>());
        }

        // ★表示の優先制御用：セルごとの status を記録（CONFIRMED を優先）
        Map<String, ShiftRequirement.Status> statusByCell = new LinkedHashMap<>();

        for (ShiftRequirement r : entities) {
            LocalDate date = r.getDate();
            String slot = r.getTimeSlot();

            // 月外は無視（安全策）
            if (date.isBefore(start) || date.isAfter(end)) {
                continue;
            }

            // セルキー（フォームと同じ規則に寄せると事故が減る）
            String cellKey = ShiftRequirementForm.toKey(date, slot);

            ShiftRequirement.Status incoming = r.getStatus();
            ShiftRequirement.Status existing = statusByCell.get(cellKey);

            // すでに CONFIRMED が入っているセルに DRAFT が来たら無視
            if (existing == ShiftRequirement.Status.CONFIRMED && incoming == ShiftRequirement.Status.DRAFT) {
                continue;
            }

            // それ以外は採用（DRAFT→CONFIRMED は上書き、同ステータスも上書き）
            statusByCell.put(cellKey, incoming);

            result
                .computeIfAbsent(date, d -> new LinkedHashMap<>())
                .put(slot, r.getRequiredCount());
        }

        return result;
    }


    /**
     * シフト生成画面（generate.html）表示用に、
     * 指定した部署・年月の必要人員を詰めたフォームオブジェクトを組み立てる。
     *
     * ・requiredCounts（Map）に「yyyy-MM-dd_timeSlot」をキーとして値を詰める。
     * ・DBに値がないセルは 0 を初期値として詰める（画面側で空欄にならないようにする）。
     */
    public ShiftRequirementForm buildForm(String department, YearMonth month) {
        Map<LocalDate, Map<String, Integer>> requirementMap = getRequirementMap(department, month);

        ShiftRequirementForm form = new ShiftRequirementForm();
        form.setDepartment(department);
        form.setYear(month.getYear());
        form.setMonth(month.getMonthValue());

        // ★ここが肝：画面が参照するのはフォームの requiredCounts なので、DB値をここに詰め戻す
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            Map<String, Integer> perSlot = requirementMap.getOrDefault(date, Map.of());

            for (String timeSlot : TIME_SLOTS) {
                int count = perSlot.getOrDefault(timeSlot, 0);
                String key = ShiftRequirementForm.toKey(date, timeSlot);
                form.getRequiredCounts().put(key, count);
            }
        }

        return form;
    }

    // ======================================================================
    // 保存系（Controller action 分岐に対応）
    // ======================================================================

    /**
     * 一時保存（下書きとして保存）
     * - Controller の action が未指定/その他 の場合に呼ばれる想定。
     */
    @Transactional
    public void saveDraftFromForm(ShiftRequirementForm form) {
        saveFromFormWithStatus(form, ShiftRequirement.Status.DRAFT);
    }

    /**
     * 確定（生成で参照する正式値として保存）
     * - Controller の action=CONFIRMED の場合に呼ばれる想定。
     */
    @Transactional
    public void saveConfirmedFromForm(ShiftRequirementForm form) {
        saveFromFormWithStatus(form, ShiftRequirement.Status.CONFIRMED);
    }

    /**
     * 確定解除（当月・部署の確定データを解除して下書きに戻す）
     * - Controller の action=UNCONFIRM の場合に呼ばれる想定。
     *
     * ※現状設計は 1セル=1レコード なので「CONFIRMED → DRAFT」へ一括更新するだけで成立する。
     */
    @Transactional
    public void unconfirm(String department, int year, int month) {
        if (department == null || department.isBlank()) {
            return;
        }

        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        // 更新件数が必要なら戻り値（int）を受けてログ出力してもOK
        shiftRequirementRepository.unconfirmInMonth(department, start, end);
    }

    // ======================================================================
    // 既存互換：呼び出し元が残っていても動くようにしておく
    // ======================================================================

    /**
     * 既存互換：従来どおり「フォーム保存=下書き保存」として扱う。
     * 既存 Controller が saveFromForm(form) を呼んでいても破綻しないように残す。
     */
    @Transactional
    public void saveFromForm(ShiftRequirementForm form) {
        saveDraftFromForm(form);
    }

    // ======================================================================
    // 内部共通（フォーム保存の本体）
    // ======================================================================

    /**
     * フォームから送信された必要人員を、指定された status（DRAFT/CONFIRMED）として保存する。
     *
     * ・Map の key（yyyy-MM-dd_timeSlot）を分解して date と timeSlot を復元する。
     * ・value が null の場合は 0 として扱い保存する（「空欄＝0人」で上書きする運用）。
     * ・フォームに紛れた “月外データ” は保存しない（安全策）。
     */
    @Transactional
    protected void saveFromFormWithStatus(ShiftRequirementForm form, ShiftRequirement.Status status) {
        String dept = form.getDepartment();
        if (dept == null || dept.isBlank()) {
            return; // 異常入力の保険（必要なら例外にしてOK）
        }

        Map<String, Integer> counts = form.getRequiredCounts();
        if (counts == null || counts.isEmpty()) {
            return;
        }

        YearMonth ym = YearMonth.of(form.getYear(), form.getMonth());
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            String key = e.getKey(); // "yyyy-MM-dd_timeSlot"
            if (key == null || !key.contains("_")) {
                continue;
            }

            String[] parts = key.split("_", 2);
            if (parts.length != 2) {
                continue;
            }

            LocalDate date;
            try {
                date = LocalDate.parse(parts[0]);
            } catch (Exception ex) {
                continue; // 想定外キーは無視
            }

            // 月外のキーが紛れた場合は無視（安全策）
            if (date.isBefore(start) || date.isAfter(end)) {
                continue;
            }

            String timeSlot = parts[1];

            // 画面で空欄にされた場合は null になり得る → 0として保存
            int requiredCount = (e.getValue() == null) ? 0 : e.getValue();

            upsertRequirement(dept, date, timeSlot, requiredCount, status);
        }
    }

    // ======================================================================
    // アップサート（新規 or 更新）
    // ======================================================================

    /**
     * 部署＋日付＋時間帯をキーに、必要人員を1件アップサート（新規 or 更新）する。
     *
     * ※ requiredCount は「0も有効値」として保存する運用。
     *    0を保存したくない（未入力はレコード自体を消したい）運用の場合は
     *    requiredCount==0 のとき delete に分岐する等へ変更してください。
     *
     * ※本メソッドは「主キー（id）」に依存しない。
     *   （id は DB 採番であり、業務上の一意性は date+department+timeSlot で決まるため）
     */
    @Transactional
    public void upsertRequirement(String department, LocalDate date, String timeSlot, int requiredCount, ShiftRequirement.Status status) {
        ShiftRequirement entity = shiftRequirementRepository
                .findByDepartmentAndDateAndTimeSlot(department, date, timeSlot)
                .orElseGet(() -> new ShiftRequirement(date, department, timeSlot, requiredCount, status));

        entity.setRequiredCount(requiredCount);
        entity.setStatus(status);

        shiftRequirementRepository.save(entity);
    }

    /**
     * 既存互換：status を指定しない場合は DRAFT 扱いで保存する。
     * （以前の呼び出し箇所が残っていてもコンパイルエラーにならないようにしておく）
     */
    @Transactional
    public void upsertRequirement(String department, LocalDate date, String timeSlot, int requiredCount) {
        upsertRequirement(department, date, timeSlot, requiredCount, ShiftRequirement.Status.DRAFT);
    }
}
