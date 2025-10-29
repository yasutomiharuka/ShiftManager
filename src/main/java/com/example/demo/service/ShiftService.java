package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;            // ★ 追加：min/max 用
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

        // ★ ガード：dates が null/空なら即返す（IN () 問題の根絶 & 無駄クエリ抑止）
        if (dates == null || dates.isEmpty()) {
            System.out.println("[getShiftMap] dates is empty -> return empty map");
            return Map.of();
        }

        // ★ ここがポイント：IN をやめて BETWEEN（両端含む）で取得する
        //    dates がソート済みでない可能性に備えて min/max を取る
        LocalDate start = Collections.min(dates); // 月初など
        LocalDate end   = Collections.max(dates); // 月末など

        // --- 表示対象全体の日付＋部署のシフト情報をまとめて取得 ---
        // 旧）IN 版：findByDepartmentAndDateIn(department, dates);
        // 新）BETWEEN 版（両端含む）
        //
        // 2024 対応: 一時保存（DRAFT）の内容が画面に戻らないとの報告があった。
        // 原因は、表示側で CONFIRMED のみを拾う実装に依存していたため、
        // DRAFT で保存されたレコードが無視されてしまっていたこと。
        // ここではステータスを問わず取得した上で、優先ルールに従って 1 セル 1 件に正規化する。
        List<Shift> shifts = shiftRepository.findByDepartmentAndDateBetween(department, start, end);

        Set<Long> targetUserIds = users == null ? Set.of() : users.stream()
                .map(UserProfileDto::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<LocalDate> targetDates = new HashSet<>(dates);

        // ▼ 同一セルに複数レコード（DRAFT と CONFIRMED）が存在する場合に備えて
        //    最新の状態（優先度: DRAFT > CONFIRMED、同一優先度では更新日時が新しい方）を採用する。
        Map<String, Shift> latestByCell = new HashMap<>();
        for (Shift shift : shifts) {
            if (shift == null || shift.getUser() == null || shift.getDate() == null) {
                continue;
            }

            Long userId = shift.getUser().getId();
            LocalDate date = shift.getDate();

            if (!targetUserIds.isEmpty() && (userId == null || !targetUserIds.contains(userId))) {
                continue;
            }
            if (!targetDates.contains(date)) {
                continue;
            }

            String key = userId + "_" + date;
            Shift current = latestByCell.get(key);
            if (current == null || isPreferred(shift, current)) {
                latestByCell.put(key, shift);
            }
        }

        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, Shift> entry : latestByCell.entrySet()) {
            Shift shift = entry.getValue();
            if (shift == null) {
                continue;
            }

            String shiftType = shift.getShiftType();
            if (!StringUtils.hasText(shiftType)) {
                // 空文字や null は画面に表示しない（既存の値を上書きしない）
                continue;
            }

            map.put(entry.getKey(), shiftType.trim());
        }

        return map;
    }

    /**
     * 2つのシフトのうち、画面表示として優先すべき方を判定する。
     * 優先順位:
     *  1. Status が DRAFT の方を優先
     *  2. Status が同じ場合は updatedAt が新しい方を優先
     */
    private boolean isPreferred(Shift candidate, Shift current) {
        Status candidateStatus = candidate.getStatus();
        Status currentStatus = current.getStatus();

        if (candidateStatus == Status.DRAFT && currentStatus != Status.DRAFT) {
            return true;
        }
        if (candidateStatus != Status.DRAFT && currentStatus == Status.DRAFT) {
            return false;
        }

        if (candidateStatus == null && currentStatus != null) {
            return false;
        }
        if (candidateStatus != null && currentStatus == null) {
            return true;
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

    // =====================================================
    // ▼ 追加：シフトの保存・更新関連メソッド
    // =====================================================

    /**
     * 画面の入力データを一括で保存。
     * - status = DRAFT（一時保存）または CONFIRMED（確定）
     * - 値が "-" または "" のセルは削除（=クリア扱い）
     */
    @Transactional
    public void saveShifts(ShiftGenerationForm form, Status status) {
        if (form == null || form.getShifts() == null) return;

        if (!StringUtils.hasText(form.getDepartment())) {
            throw new IllegalArgumentException("Department must not be null or empty when saving shifts");
        }

        final String department = form.getDepartment().trim();
        final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Map.Entry<String, String> entry : form.getShifts().entrySet()) {
            String key = entry.getKey();       // "userId_YYYY-MM-DD"
            String value = entry.getValue() == null ? "" : entry.getValue().trim();

            // --- キーを分解 ---
            int underscore = key.indexOf('_');
            if (underscore <= 0) continue;
            Long userId;
            LocalDate date;
            try {
                userId = Long.valueOf(key.substring(0, underscore));
                date = LocalDate.parse(key.substring(underscore + 1), DF);
            } catch (Exception e) {
                continue; // フォーマット不正はスキップ
            }

            // --- クリア処理（"-" or 空文字） ---
            if (value.isEmpty() || "-".equals(value)) {
                shiftRepository.deleteByUser_IdAndDateAndDepartment(userId, date, department);
                continue;
            }

            // --- Upsert処理 ---
            var opt = shiftRepository.findByUser_IdAndDateAndDepartment(userId, date, department);
            Shift shift = opt.orElseGet(Shift::new);

            if (shift.getId() == null) {
                var user = userProfileRepository.findById(userId).orElse(null);
                if (user == null) continue;
                shift.setUser(user);
                shift.setDate(date);
                shift.setDepartment(department);
            }

            shift.setShiftType(value); // "日","夜","明","休","有","臨(確)","臨(自)"
            shift.setStatus(status);   // DRAFT or CONFIRMED

            shiftRepository.save(shift);
        }
    }

    /**
     * 確定解除：指定範囲のシフトをすべて DRAFT に戻す。
     */
    @Transactional
    public void unconfirmShifts(ShiftGenerationForm form) {
        if (form == null || form.getShifts() == null) return;

        if (!StringUtils.hasText(form.getDepartment())) {
            throw new IllegalArgumentException("Department must not be null or empty when unconfirming shifts");
        }

        final String department = form.getDepartment().trim();
        final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (String key : form.getShifts().keySet()) {
            int underscore = key.indexOf('_');
            if (underscore <= 0) continue;
            try {
                Long userId = Long.valueOf(key.substring(0, underscore));
                LocalDate date = LocalDate.parse(key.substring(underscore + 1), DF);

                var opt = shiftRepository.findByUser_IdAndDateAndDepartment(userId, date, department);
                if (opt.isPresent()) {
                    Shift shift = opt.get();
                    shift.setStatus(Status.DRAFT);
                    shiftRepository.save(shift);
                }
            } catch (Exception e) {
                continue;
            }
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
