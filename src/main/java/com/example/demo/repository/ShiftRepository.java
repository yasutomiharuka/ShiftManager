package com.example.demo.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Shift;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {

    /**
     * 指定された部署と日付リストに該当するすべてのシフトを取得する。
     * 一覧画面の表示で使用。
     *
     * @param department 対象部署
     * @param dates 対象日付リスト
     * @return 条件に一致するシフトのリスト
     */
    List<Shift> findByDepartmentAndDateIn(String department, List<LocalDate> dates);

    /**
     * 特定のユーザーと月のすべてのシフトを取得。
     * シフト詳細画面などで使用予定。
     *
     * @param userId ユーザーID
     * @param start 開始日（通常は月初）
     * @param end 終了日（通常は月末）
     * @return ユーザーの月間シフト一覧
     */
    List<Shift> findByUser_IdAndDateBetween(Long userId, LocalDate start, LocalDate end);

    /**
     * シフトの重複チェックや編集用に1日分を取得。
     */
    List<Shift> findByDate(LocalDate date);

    /**
     * ユーザーと日付でシフト1件を取得（存在チェックや編集時に使える）
     */
    Shift findByUser_IdAndDate(Long userId, LocalDate date);
}
