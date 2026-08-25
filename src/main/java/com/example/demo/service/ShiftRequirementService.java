// src/main/java/com/example/demo/service/ShiftRequirementService.java
package com.example.demo.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.form.ShiftRequirementForm;
import com.example.demo.model.ShiftRequirement;
import com.example.demo.repository.ShiftRequirementRepository;

/**
 * 必要人員（ShiftRequirement）に関する業務ロジックを担当するサービス。
 *
 * 主な役割：
 * ・部署、日付、時間帯ごとの必要人員を取得する。
 * ・シフト生成画面用の必要人員フォームを作成する。
 * ・必要人員の一時保存、確定、確定解除を行う。
 * ・過去に保存した必要人員を翌月以降の初期値として引き継ぐ。
 *
 * 必要人員の初期値は、以下の優先順位で設定する。
 *
 * 1. 対象月に保存済みの必要人員
 * 2. 同じ部署、曜日、時間帯の直近の過去登録値
 * 3. 0
 */
@Service
public class ShiftRequirementService {

    private final ShiftRequirementRepository shiftRequirementRepository;

    /**
     * 画面・DBで共通に扱う時間帯コード。
     */
    public static final List<String> TIME_SLOTS = Arrays.asList(
            "09-14",
            "14-16",
            "16-18",
            "NIGHT"
    );

    public ShiftRequirementService(
            ShiftRequirementRepository shiftRequirementRepository) {

        this.shiftRequirementRepository = shiftRequirementRepository;
    }

    // ======================================================================
    // 参照系：画面表示用
    // ======================================================================

    /**
     * 指定した部署・年月の必要人員を取得する。
     *
     * 戻り値の構造：
     *
     * Map<
     *     LocalDate,
     *     Map<String, Integer>
     * >
     *
     * 例：
     *
     * 2026-09-01 -> {
     *     "09-14" -> 3,
     *     "14-16" -> 2,
     *     "16-18" -> 2,
     *     "NIGHT" -> 2
     * }
     *
     * 同一セルのデータが複数存在する場合は、
     * CONFIRMED を DRAFT より優先する。
     *
     * @param department 対象部署
     * @param month 対象年月
     * @return 日付・時間帯ごとの必要人数
     */
    public Map<LocalDate, Map<String, Integer>> getRequirementMap(
            String department,
            YearMonth month) {

        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        List<ShiftRequirement> entities =
                shiftRequirementRepository.findByDepartmentAndDateBetween(
                        department,
                        start,
                        end
                );

        Map<LocalDate, Map<String, Integer>> result =
                new LinkedHashMap<>();

        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            result.put(
                    month.atDay(day),
                    new LinkedHashMap<>()
            );
        }

        /*
         * 同一セルの状態を記録し、
         * CONFIRMED を DRAFT より優先する。
         */
        Map<String, ShiftRequirement.Status> statusByCell =
                new LinkedHashMap<>();

        for (ShiftRequirement requirement : entities) {

            LocalDate date = requirement.getDate();
            String timeSlot = requirement.getTimeSlot();

            if (date == null || timeSlot == null) {
                continue;
            }

            /*
             * 対象月以外のデータは表示対象から除外する。
             */
            if (date.isBefore(start) || date.isAfter(end)) {
                continue;
            }

            String cellKey = ShiftRequirementForm.toKey(
                    date,
                    timeSlot
            );

            ShiftRequirement.Status incoming =
                    requirement.getStatus();

            ShiftRequirement.Status existing =
                    statusByCell.get(cellKey);

            /*
             * すでにCONFIRMEDの値がある場合は、
             * 後から取得したDRAFTで上書きしない。
             */
            if (existing == ShiftRequirement.Status.CONFIRMED
                    && incoming == ShiftRequirement.Status.DRAFT) {

                continue;
            }

            statusByCell.put(
                    cellKey,
                    incoming
            );

            result
                    .computeIfAbsent(
                            date,
                            key -> new LinkedHashMap<>()
                    )
                    .put(
                            timeSlot,
                            requirement.getRequiredCount()
                    );
        }

        return result;
    }

    /**
     * 過去に保存した必要人員から、
     * 曜日・時間帯ごとの初期値を作成する。
     *
     * 戻り値の構造：
     *
     * Map<
     *     DayOfWeek,
     *     Map<String, Integer>
     * >
     *
     * 例：
     *
     * MONDAY -> {
     *     "09-14" -> 3,
     *     "14-16" -> 2,
     *     "16-18" -> 2,
     *     "NIGHT" -> 2
     * }
     *
     * 引き継ぎ対象：
     *
     * ・対象月の初日より前に保存された必要人員
     * ・対象部署と一致するデータ
     * ・対象曜日と一致するデータ
     * ・対象時間帯と一致するデータ
     *
     * 同じ曜日・時間帯に複数の登録値がある場合は、
     * 最も新しい日付の値を採用する。
     *
     * @param department 対象部署
     * @param targetMonth 対象年月
     * @return 曜日・時間帯ごとの必要人数
     */
    public Map<DayOfWeek, Map<String, Integer>> buildDefaultRequirementMap(
            String department,
            YearMonth targetMonth) {

        Map<DayOfWeek, Map<String, Integer>> defaultMap =
                new EnumMap<>(DayOfWeek.class);

        if (department == null
                || department.isBlank()
                || targetMonth == null) {

            return defaultMap;
        }

        /*
         * 例：
         *
         * 対象月が2026年9月の場合、
         * 2026年9月1日より前のデータを取得する。
         */
        LocalDate targetMonthStart = targetMonth.atDay(1);

        List<ShiftRequirement> previousRequirements =
                shiftRequirementRepository
                        .findByDepartmentAndDateBeforeOrderByDateDesc(
                                department,
                                targetMonthStart
                        );

        for (ShiftRequirement requirement : previousRequirements) {

            LocalDate date = requirement.getDate();
            String timeSlot = requirement.getTimeSlot();
            Integer requiredCount = requirement.getRequiredCount();

            /*
             * 不完全なデータは引き継ぎ対象外とする。
             */
            if (date == null
                    || timeSlot == null
                    || requiredCount == null) {

                continue;
            }

            /*
             * 現在の画面で使用している時間帯だけを対象にする。
             */
            if (!TIME_SLOTS.contains(timeSlot)) {
                continue;
            }

            /*
             * 不正な負数データは初期値に使用しない。
             */
            if (requiredCount < 0) {
                continue;
            }

            DayOfWeek dayOfWeek = date.getDayOfWeek();

            Map<String, Integer> valuesByTimeSlot =
                    defaultMap.computeIfAbsent(
                            dayOfWeek,
                            key -> new LinkedHashMap<>()
                    );

            /*
             * Repositoryは日付の降順で取得するため、
             * 最初に登録した値が最も新しい値になる。
             *
             * putIfAbsentにより、
             * 古い値による上書きを防ぐ。
             */
            valuesByTimeSlot.putIfAbsent(
                    timeSlot,
                    requiredCount
            );
        }

        return defaultMap;
    }

    /**
     * シフト生成画面用の必要人員フォームを作成する。
     *
     * 初期値の優先順位：
     *
     * 1. 対象月に保存済みの値
     * 2. 同じ部署、曜日、時間帯の直近の過去登録値
     * 3. 0
     *
     * このメソッドではDBへの保存は行わない。
     *
     * @param department 対象部署
     * @param month 対象年月
     * @return 必要人員フォーム
     */
    public ShiftRequirementForm buildForm(
            String department,
            YearMonth month) {

        /*
         * 対象月に保存済みの必要人員を取得する。
         */
        Map<LocalDate, Map<String, Integer>> requirementMap =
                getRequirementMap(
                        department,
                        month
                );

        /*
         * 過去の登録値から、
         * 曜日・時間帯ごとの初期値を作成する。
         */
        Map<DayOfWeek, Map<String, Integer>> defaultRequirementMap =
                buildDefaultRequirementMap(
                        department,
                        month
                );

        ShiftRequirementForm form = new ShiftRequirementForm();

        form.setDepartment(department);
        form.setYear(month.getYear());
        form.setMonth(month.getMonthValue());

        for (int day = 1; day <= month.lengthOfMonth(); day++) {

            LocalDate date = month.atDay(day);

            /*
             * 対象日の保存済み必要人員。
             */
            Map<String, Integer> savedValues =
                    requirementMap.getOrDefault(
                            date,
                            Map.of()
                    );

            /*
             * 対象曜日の過去登録値。
             */
            Map<String, Integer> defaultValues =
                    defaultRequirementMap.getOrDefault(
                            date.getDayOfWeek(),
                            Map.of()
                    );

            for (String timeSlot : TIME_SLOTS) {

                int requiredCount;

                /*
                 * containsKeyで判定することで、
                 * 保存済みの「0人」と未登録を区別する。
                 */
                if (savedValues.containsKey(timeSlot)) {

                    Integer savedCount = savedValues.get(timeSlot);

                    requiredCount =
                            savedCount != null ? savedCount : 0;

                } else {

                    requiredCount =
                            defaultValues.getOrDefault(
                                    timeSlot,
                                    0
                            );
                }

                String key = ShiftRequirementForm.toKey(
                        date,
                        timeSlot
                );

                form.getRequiredCounts().put(
                        key,
                        requiredCount
                );
            }
        }

        return form;
    }

    // ======================================================================
    // 保存系：Controller の action に応じて処理を分岐
    // ======================================================================

    /**
     * 必要人員をDRAFT状態で一時保存する。
     *
     * @param form 必要人員フォーム
     */
    @Transactional
    public void saveDraftFromForm(
            ShiftRequirementForm form) {

        saveFromFormWithStatus(
                form,
                ShiftRequirement.Status.DRAFT
        );
    }

    /**
     * 必要人員をCONFIRMED状態で確定保存する。
     *
     * @param form 必要人員フォーム
     */
    @Transactional
    public void saveConfirmedFromForm(
            ShiftRequirementForm form) {

        saveFromFormWithStatus(
                form,
                ShiftRequirement.Status.CONFIRMED
        );
    }

    /**
     * 対象部署・対象月の確定済み必要人員を、
     * 一括でDRAFT状態に戻す。
     *
     * @param department 対象部署
     * @param year 対象年
     * @param month 対象月
     */
    @Transactional
    public void unconfirm(
            String department,
            int year,
            int month) {

        if (department == null || department.isBlank()) {
            return;
        }

        YearMonth yearMonth = YearMonth.of(
                year,
                month
        );

        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        shiftRequirementRepository.unconfirmInMonth(
                department,
                start,
                end
        );
    }

    // ======================================================================
    // 既存互換：従来の保存処理
    // ======================================================================

    /**
     * 従来の保存処理。
     *
     * 既存の呼び出し元との互換性のため、
     * DRAFT保存へ処理を委譲する。
     *
     * @param form 必要人員フォーム
     */
    @Transactional
    public void saveFromForm(
            ShiftRequirementForm form) {

        saveDraftFromForm(form);
    }

    // ======================================================================
    // 内部共通：フォーム保存
    // ======================================================================

    /**
     * フォームの必要人員を、
     * 指定された保存状態で登録・更新する。
     *
     * @param form 必要人員フォーム
     * @param status 保存状態
     */
    @Transactional
    protected void saveFromFormWithStatus(
            ShiftRequirementForm form,
            ShiftRequirement.Status status) {

        String department = form.getDepartment();

        if (department == null || department.isBlank()) {
            return;
        }

        Map<String, Integer> counts =
                form.getRequiredCounts();

        if (counts == null || counts.isEmpty()) {
            return;
        }

        YearMonth yearMonth = YearMonth.of(
                form.getYear(),
                form.getMonth()
        );

        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {

            String key = entry.getKey();

            /*
             * キーの形式：
             *
             * yyyy-MM-dd_timeSlot
             *
             * 例：
             * 2026-09-01_09-14
             */
            if (key == null || !key.contains("_")) {
                continue;
            }

            String[] parts = key.split(
                    "_",
                    2
            );

            if (parts.length != 2) {
                continue;
            }

            LocalDate date;

            try {

                date = LocalDate.parse(parts[0]);

            } catch (Exception exception) {

                continue;
            }

            /*
             * 対象月以外の日付は保存しない。
             */
            if (date.isBefore(start) || date.isAfter(end)) {
                continue;
            }

            String timeSlot = parts[1];

            /*
             * 空欄は0人として保存する。
             */
            int requiredCount =
                    entry.getValue() == null
                            ? 0
                            : entry.getValue();

            upsertRequirement(
                    department,
                    date,
                    timeSlot,
                    requiredCount,
                    status
            );
        }
    }

    // ======================================================================
    // アップサート：必要人員の新規登録または更新
    // ======================================================================

    /**
     * 部署・日付・時間帯に一致する必要人員を、
     * 新規登録または更新する。
     *
     * @param department 対象部署
     * @param date 対象日
     * @param timeSlot 時間帯
     * @param requiredCount 必要人数
     * @param status 保存状態
     */
    @Transactional
    public void upsertRequirement(
            String department,
            LocalDate date,
            String timeSlot,
            int requiredCount,
            ShiftRequirement.Status status) {

        ShiftRequirement entity =
                shiftRequirementRepository
                        .findByDepartmentAndDateAndTimeSlot(
                                department,
                                date,
                                timeSlot
                        )
                        .orElseGet(
                                () -> new ShiftRequirement(
                                        date,
                                        department,
                                        timeSlot,
                                        requiredCount,
                                        status
                                )
                        );

        entity.setRequiredCount(requiredCount);
        entity.setStatus(status);

        shiftRequirementRepository.save(entity);
    }

    /**
     * 保存状態が指定されない場合は、
     * DRAFTとして登録する。
     *
     * @param department 対象部署
     * @param date 対象日
     * @param timeSlot 時間帯
     * @param requiredCount 必要人数
     */
    @Transactional
    public void upsertRequirement(
            String department,
            LocalDate date,
            String timeSlot,
            int requiredCount) {

        upsertRequirement(
                department,
                date,
                timeSlot,
                requiredCount,
                ShiftRequirement.Status.DRAFT
        );
    }
}