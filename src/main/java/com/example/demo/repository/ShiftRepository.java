package com.example.demo.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Shift;
import com.example.demo.model.Shift.Status;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {

    /**
     * 指定された部署と日付の「範囲」に該当するすべてのシフトを取得する（両端含む）。
     * 一覧画面の表示で使用。
     *
     * @param department 対象部署
     * @param start 開始日（通常は月初）※この日を含む
     * @param end 終了日（通常は月末）※この日を含む
     * @return 条件に一致するシフトのリスト
     */
    List<Shift> findByDepartmentAndDateBetween(String department, LocalDate start, LocalDate end);

    // ----------------------------------------------
    // 旧：IN 版（空コレクション時に IN () でSQLエラーになり得るため非推奨・廃止）
    // 残置は互換性・参照用。呼び出し側は Between 版へ移行してください。
    // List<Shift> findByDepartmentAndDateIn(String department, List<LocalDate> dates);
    // ----------------------------------------------

    /**
     * 部署＋日付の「範囲」＋状態で検索（両端含む）。
     * 例: 確定済み(CONFIRMED)のみを一覧表示したい場合。
     *
     * @param department 対象部署
     * @param start 開始日（含む）
     * @param end 終了日（含む）
     * @param status 取得したいシフトの状態
     */
    List<Shift> findByDepartmentAndDateBetweenAndStatus(
            String department, LocalDate start, LocalDate end, Status status);

    // ----------------------------------------------
    // 旧：IN＋Status 版（非推奨・廃止。Between に置換してください）
    // List<Shift> findByDepartmentAndDateInAndStatus(String department, List<LocalDate> dates, Status status);
    // ----------------------------------------------

    /**
     * 特定のユーザーと月のすべてのシフトを取得（両端含む）。
     * シフト詳細画面などで使用予定。
     *
     * @param userId ユーザーID
     * @param start 開始日（通常は月初）
     * @param end 終了日（通常は月末）
     * @return ユーザーの月間シフト一覧
     */
    List<Shift> findByUser_IdAndDateBetween(Long userId, LocalDate start, LocalDate end);

    /**
     * 指定ユーザー・日付・部署でシフトを一意に取得。
     * upsert 時に利用する。
     */
    Optional<Shift> findByUser_IdAndDateAndDepartment(Long userId, LocalDate date, String department);

    /**
     * 指定ユーザー・日付・部署のシフトを削除。
     * クリア操作で利用。
     */
    void deleteByUser_IdAndDateAndDepartment(Long userId, LocalDate date, String department);

    /**
     * シフトの重複チェックや編集用に日付指定で取得。
     */
    List<Shift> findByDate(LocalDate date);

    /**
     * ユーザーと日付でシフトを取得（既存互換用）。
     */
    Shift findByUser_IdAndDate(Long userId, LocalDate date);
}
