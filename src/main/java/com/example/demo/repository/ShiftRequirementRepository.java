package com.example.demo.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.ShiftRequirement;

public interface ShiftRequirementRepository extends JpaRepository<ShiftRequirement, Integer> {

    /*
     * NOTE:
     * Entity 側の id は Integer になっているため、本来は
     * JpaRepository<ShiftRequirement, Integer> が整合します。
     * 現状 Long で運用できているなら大きな問題が顕在化していない
     * 可能性もありますが、将来的な不具合回避のため、
     * どこかのタイミングで型を揃えることを推奨します。
     */

    // ----------------------------------------------------------------------
    // 参照系（画面表示 / 集計 / バリデーション等で使用）
    // ----------------------------------------------------------------------

    /**
     * 特定の日付・部署の要件を取得する。
     * 例：ある日の必要人員（時間帯別）を取りたいときに使用。
     *
     * @param date 対象日
     * @param department 対象部署
     * @return 条件に一致する必要人員リスト
     */
    List<ShiftRequirement> findByDateAndDepartment(
            LocalDate date,
            String department
    );

    /**
     * 部署・月単位（期間）の要件一覧を取得する。
     * 例：generate/list 画面で1ヶ月分の入力値を
     * まとめて表示する際に使用。
     *
     * @param department 対象部署
     * @param startDate 対象期間開始日
     * @param endDate 対象期間終了日
     * @return 対象部署・対象期間に一致する必要人員リスト
     */
    List<ShiftRequirement> findByDepartmentAndDateBetween(
            String department,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * 日付・部署・時間帯で絞り込んで取得する。
     *
     * 設計上ユニーク制約（date, department, time_slot）が
     * 効いている前提なら、本来は 0 or 1 件のはず。
     * ただし過去データに重複が混入した場合の保険や、
     * ユニーク制約が無い環境用に List のまま残している想定。
     *
     * @param date 対象日
     * @param department 対象部署
     * @param timeSlot 時間帯
     * @return 条件に一致する必要人員リスト
     */
    List<ShiftRequirement> findByDateAndDepartmentAndTimeSlot(
            LocalDate date,
            String department,
            String timeSlot
    );

    /**
     * アップサート用：部署＋日付＋時間帯の1件を取得する。
     * 存在すれば更新、なければ新規登録する際に使用。
     *
     * Service 側で
     *
     * findByDepartmentAndDateAndTimeSlot(...)
     *     .orElseGet(() -> new ShiftRequirement(...))
     *
     * の形で利用し、上書き更新（アップサート）を実現する。
     *
     * @param department 対象部署
     * @param date 対象日
     * @param timeSlot 時間帯
     * @return 条件に一致する必要人員。
     *         存在しない場合は Optional.empty()
     */
    Optional<ShiftRequirement> findByDepartmentAndDateAndTimeSlot(
            String department,
            LocalDate date,
            String timeSlot
    );

    /**
     * 対象月より前に登録された必要人員を、
     * 同じ部署かつ新しい日付順で取得する。
     *
     * 翌月以降の必要人員初期値を作成する際に使用する。
     *
     * 例：
     * beforeDate が 2026-09-01 の場合、
     * 2026-08-31 以前の対象部署の必要人員を取得する。
     *
     * 取得結果は日付の降順となるため、
     * 同じ曜日・時間帯に複数の登録値がある場合は、
     * 最初に取得された値が直近の登録値となる。
     *
     * DRAFT / CONFIRMED のどちらも取得対象とする。
     *
     * @param department 対象部署
     * @param beforeDate この日付より前のデータを取得する基準日
     * @return 対象部署の過去の必要人員リスト。日付の降順。
     */
    List<ShiftRequirement> findByDepartmentAndDateBeforeOrderByDateDesc(
            String department,
            LocalDate beforeDate
    );

    // ----------------------------------------------------------------------
    // 更新系（確定解除など、一括で状態を変える用途）
    // ----------------------------------------------------------------------

    /**
     * 【確定解除（UNCONFIRM）用】
     * 指定部署・指定月（start〜end）の範囲で、
     * CONFIRMED のレコードを DRAFT に戻す。
     *
     * 目的：
     * - Controller の action=UNCONFIRM に対応
     * - 当月・部署の確定データを一括解除して再編集可能にする
     *
     * 注意：
     * - @Modifying を付けることで UPDATE クエリとして実行されます
     * - 呼び出し元 Service メソッドには @Transactional を
     *   付けてください
     *
     * @param department 対象部署
     * @param startDate 対象期間開始日
     * @param endDate 対象期間終了日
     * @return 更新された行数
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update ShiftRequirement r
           set r.status = com.example.demo.model.ShiftRequirement.Status.DRAFT
         where r.department = :department
           and r.date between :startDate and :endDate
           and r.status = com.example.demo.model.ShiftRequirement.Status.CONFIRMED
    """)
    int unconfirmInMonth(
            @Param("department") String department,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}