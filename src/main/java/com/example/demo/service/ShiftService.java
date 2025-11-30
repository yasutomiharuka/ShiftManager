package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;             // ★ 追加：対象月の範囲計算用
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.demo.dto.UserProfileDto;
import com.example.demo.form.ShiftGenerationForm;
import com.example.demo.model.Shift;
import com.example.demo.model.Shift.Status;
import com.example.demo.repository.ShiftRepository;
import com.example.demo.repository.UserProfileRepository;

@Service
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final UserProfileRepository userProfileRepository;

    public ShiftService(ShiftRepository shiftRepository,
                        UserProfileRepository userProfileRepository) {
        this.shiftRepository = shiftRepository;
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * 指定したユーザーリスト・日付リスト・部署に対するシフト情報を取得し、
     * Map<"userId_日付", 勤務種別>の形式で返却する。
     *
     * @param users ユーザー一覧（表示対象）
     * @param dates 表示対象月の日付一覧
     * @param department 所属部署名
     * @return Map形式の勤務情報（セル表示用）
     */
    public Map<String, String> getShiftMap(List<UserProfileDto> users, List<LocalDate> dates, String department) {

        if (dates == null || dates.isEmpty()) {
            return Collections.emptyMap();
        }

        if (users == null || users.stream().allMatch(Objects::isNull)) {
            return Collections.emptyMap();
        }

        List<LocalDate> normalizedDates = dates.stream()
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        if (normalizedDates.isEmpty()) {
            return Collections.emptyMap();
        }

        LocalDate start = normalizedDates.get(0);
        LocalDate end = normalizedDates.get(normalizedDates.size() - 1);

        // --- 表示対象全体の日付＋部署のシフト情報をまとめて取得 ---
        // 旧）IN 版：findByDepartmentAndDateIn(department, dates);
        // 新）BETWEEN 版（両端含む）
        //
        // DRAFT と CONFIRMED の両方を取得し、
        // 1セルに複数レコードがある場合は
        //   優先順位: CONFIRMED > DRAFT
        //   同じステータス同士なら updatedAt が新しい方
        // で 1件に絞って表示に使う。
        List<Shift> shiftsDraft = shiftRepository.findByDepartmentAndDateBetweenAndStatus(
                department, start, end, Status.DRAFT);
        List<Shift> shiftsConfirmed = shiftRepository.findByDepartmentAndDateBetweenAndStatus(
                department, start, end, Status.CONFIRMED);

        Set<Long> targetUserIds = users == null ? Set.of() : users.stream()
                .filter(Objects::nonNull)
                .map(UserProfileDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<LocalDate> targetDates = new HashSet<>(normalizedDates);

        // ▼ 同一セルに複数レコード（DRAFT と CONFIRMED）が存在する場合に備えて
        //    最新の状態（優先度: CONFIRMED > DRAFT、同一優先度では更新日時が新しい方）を採用する。
        Map<String, Shift> latestByCell = new HashMap<>();
        for (Shift shift : shiftsDraft) {
            mergeShiftIfNecessary(latestByCell, targetUserIds, targetDates, shift);
        }
        for (Shift shift : shiftsConfirmed) {
            mergeShiftIfNecessary(latestByCell, targetUserIds, targetDates, shift);
        }

        Map<String, String> map = new HashMap<>();

        for (Shift shift : latestByCell.values()) {
            Long userId = shift.getUser().getId();
            LocalDate date = shift.getDate();
            String shiftType = shift.getShiftType();

            if (!StringUtils.hasText(shiftType)) {
                // 空文字や null は画面に表示しない（既存の値を上書きしない）
                continue;
            }

            String key = userId + "_" + date;
            map.put(key, shiftType.trim());
        }

        return map;
    }

    private void mergeShiftIfNecessary(Map<String, Shift> latestByCell,
                                       Set<Long> targetUserIds,
                                       Set<LocalDate> targetDates,
                                       Shift shift) {
        if (shift == null || shift.getUser() == null || shift.getDate() == null) {
            return;
        }

        Long userId = shift.getUser().getId();
        LocalDate date = shift.getDate();

        if (!targetUserIds.isEmpty() && (userId == null || !targetUserIds.contains(userId))) {
            return;
        }
        if (!targetDates.contains(date)) {
            return;
        }

        String key = userId + "_" + date;
        Shift current = latestByCell.get(key);
        if (current == null || isPreferred(shift, current)) {
            latestByCell.put(key, shift);
        }
    }

    /**
     * 2つのシフトのうち、画面表示として優先すべき方を判定する。
     * 優先順位:
     *  1. Status が CONFIRMED の方を優先
     *  2. Status が DRAFT の方を次点で優先
     *  3. Status が同じ場合は updatedAt が新しい方を優先
     */
    private boolean isPreferred(Shift candidate, Shift current) {
        Status candidateStatus = candidate.getStatus();
        Status currentStatus = current.getStatus();

        int candidatePriority = statusPriority(candidateStatus);
        int currentPriority = statusPriority(currentStatus);

        if (candidatePriority != currentPriority) {
            return candidatePriority > currentPriority;
        }

        LocalDateTime candidateUpdated = candidate.getUpdatedAt();
        LocalDateTime currentUpdated = current.getUpdatedAt();

        if (candidateUpdated == null) {
            return false;
        }
        if (currentUpdated == null) {
            return true;
        }

        return candidateUpdated.isAfter(currentUpdated);
    }

    /**
     * ステータスごとの優先度
     * CONFIRMED(2) > DRAFT(1) > その他(0)
     */
    private int statusPriority(Status status) {
        if (status == Status.CONFIRMED) {
            return 2;
        }
        if (status == Status.DRAFT) {
            return 1;
        }
        return 0;
    }

    // =====================================================
    // シフトの保存・更新関連メソッド
    // =====================================================

    /**
     * 画面の入力データを一括で保存する。
     *
     * - status = DRAFT（一時保存） または CONFIRMED（確定）
     * - Map の key は "userId_YYYY-MM-DD" というフォーマットを想定
     * - 値が "-" または 空文字("") のセルは「シフトなし」とみなして DELETE
     * - それ以外の値は INSERT or UPDATE（Upsert）
     */
    @Transactional
    public void saveShifts(ShiftGenerationForm form, Status status) {

        // --- デバッグログ（null 安全） ---
        System.out.println("DEBUG saveShifts: status=" + status
                + ", dept=" + (form != null ? form.getDepartment() : null)
                + ", shifts=" + (form != null ? form.getShifts() : null));

        // フォーム or shifts が null / 空なら何もしないで終了
        if (form == null || form.getShifts() == null || form.getShifts().isEmpty()) {
            return;
        }

        // 部署は必須
        if (!StringUtils.hasText(form.getDepartment())) {
            throw new IllegalArgumentException(
                    "Department must not be null or empty when saving shifts");
        }

        final String department = form.getDepartment().trim();
        final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // --- 1セルずつ処理 ---
        for (Map.Entry<String, String> entry : form.getShifts().entrySet()) {

            String key = entry.getKey(); // 例: "6_2025-11-01"
            String value = entry.getValue() == null ? "" : entry.getValue().trim();

            // ===== キーを userId と LocalDate に分解 =====
            // ※ "userId_日付" という形式でないものはスキップ
            int underscore = key.indexOf('_');
            if (underscore <= 0) {
                // 不正フォーマットなのでこのセルは無視
                continue;
            }

            Long userId;
            LocalDate date;
            try {
                userId = Long.valueOf(key.substring(0, underscore));
                date = LocalDate.parse(key.substring(underscore + 1), DF);
            } catch (Exception e) {
                // パースに失敗した場合もそのセルだけスキップ
                continue;
            }

            // ===== クリア処理 =====
            // value が 空 or "-" は「シフトなし」と解釈し、既存レコードを削除
            if (value.isEmpty() || "-".equals(value)) {

                // デバッグログ：削除対象のキー情報
                System.out.println("DEBUG saveShifts: delete userId=" + userId
                        + ", date=" + date
                        + ", dept=" + department);

                // 戻り値（削除件数）は利用しないので受け取らない
                shiftRepository.deleteByUser_IdAndDateAndDepartment(userId, date, department);
                continue;
            }

            // ===== Upsert 処理 =====
            // 既存レコードがあれば取得、なければ new Shift()
            Shift shift = shiftRepository
                    .findByUser_IdAndDateAndDepartment(userId, date, department)
                    .orElseGet(Shift::new);

            // 新規（id == null）の場合は主キーとなる情報をセット
            if (shift.getId() == null) {
                // userProfiles テーブルからユーザーを取得（なければこのセルはスキップ）
                var user = userProfileRepository.findById(userId).orElse(null);
                if (user == null) {
                    // 想定外だが、念のため NPE 回避のためスキップ
                    System.out.println("DEBUG saveShifts: user not found. skip. userId=" + userId);
                    continue;
                }
                shift.setUser(user);
                shift.setDate(date);
                shift.setDepartment(department);

                // デバッグログ：新規作成
                System.out.println("DEBUG saveShifts: create new entity userId=" + userId
                        + ", date=" + date
                        + ", dept=" + department);
            } else {
                // デバッグログ：既存レコード更新
                System.out.println("DEBUG saveShifts: update existing entity id=" + shift.getId()
                        + ", userId=" + userId
                        + ", date=" + date
                        + ", dept=" + department);
            }

            // 画面のセル値をそのまま shiftType に保存
            // （"日","夜","明","休","有","臨(確)","臨(自)" など）
            shift.setShiftType(value);

            // 一時保存 or 確定のステータスをセット
            shift.setStatus(status);

            // デバッグログ：保存直前の状態確認
            System.out.println("DEBUG saveShifts: before save entity id=" + shift.getId()
                    + ", userId=" + userId
                    + ", date=" + date
                    + ", dept=" + department
                    + ", type=" + value
                    + ", status=" + status);

            // 新規なら INSERT、既存なら UPDATE が発行される
            shiftRepository.save(shift);
        }

        // ループの一番最後あたり（for の外）に追加
        System.out.println("DEBUG saveShifts: completed. total=" + form.getShifts().size());

        // ※ すぐに SQL を発行させたい場合はここで flush も可能（任意）
        // shiftRepository.flush();
    }

    /**
     * 確定解除：指定範囲のシフトをすべて DRAFT に戻す。
     *
     * ▼仕様
     * - 対象：フォームの「部署」＋「対象月」に属するシフト
     * - 現在 CONFIRMED のものだけを DRAFT に変更する
     * - DRAFT の行はそのまま（変更しない）
     *
     * 「一度確定した月を、もう一度編集可能な下書き状態に戻す」ための操作。
     */
    @Transactional
    public void unconfirmShifts(ShiftGenerationForm form) {

        // フォームが null なら何もしない
        if (form == null) {
            return;
        }

        // 部署必須チェック
        if (!StringUtils.hasText(form.getDepartment())) {
            throw new IllegalArgumentException("Department must not be null or empty when unconfirming shifts");
        }
        final String department = form.getDepartment().trim();

        // 対象月が設定されている前提で処理
        YearMonth targetMonth = form.getTargetMonth();
        if (targetMonth == null) {
            // 万が一 targetMonth が入っていない場合は、従来どおり
            // form.shifts のキーから日付を1件ずつ unconfirm する方法にフォールバックしてもよいが、
            // ここでは明示的に例外にしておく（運用上、必ず targetMonth がセットされる想定）
            throw new IllegalArgumentException("TargetMonth must not be null when unconfirming shifts");
        }

        LocalDate start = targetMonth.atDay(1);
        LocalDate end   = targetMonth.atEndOfMonth();

        // 対象部署・対象月の CONFIRMED シフトを一括取得
        List<Shift> confirmedList = shiftRepository.findByDepartmentAndDateBetweenAndStatus(
                department, start, end, Status.CONFIRMED);

        System.out.println("DEBUG unconfirmShifts: dept=" + department
                + ", month=" + targetMonth
                + ", confirmedCount=" + confirmedList.size());

        // CONFIRMED → DRAFT に変更して保存
        for (Shift shift : confirmedList) {
            shift.setStatus(Status.DRAFT);
            shiftRepository.save(shift);
        }
    }

    // -----------------------------------------------------
    // （参考）状態で絞りたい場合の BETWEEN 版サンプル（必要になったら有効化）
    // public Map<String, String> getShiftMapByStatus(
    //         List<UserProfileDto> users, List<LocalDate> dates, String department, Status status) {
    //     if (dates == null || dates.isEmpty()) return Map.of();
    //     LocalDate start = Collections.min(dates);
    //     LocalDate end   = Collections.max(dates);
    //     List<Shift> shifts = shiftRepository.findByDepartmentAndDateBetweenAndStatus(department, start, end, status);
    //     Map<String, String> map = new HashMap<>();
    //     for (Shift shift : shifts) {
    //         String key = shift.getUser().getId() + "_" + shift.getDate();
    //         map.put(key, shift.getShiftType());
    //     }
    //     return map;
    // }
    // -----------------------------------------------------
}
