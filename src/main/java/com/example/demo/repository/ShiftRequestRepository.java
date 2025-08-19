package com.example.demo.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.ShiftRequest;
import com.example.demo.model.UserProfile;

/**
 * ShiftRequest エンティティのリポジトリ。
 * 希望休・有給の申請情報をDBから取得・保存・削除・確認するために使用。
 */
@Repository
public interface ShiftRequestRepository extends JpaRepository<ShiftRequest, Long> {

    /**
     * 特定のユーザーの申請をすべて取得
     * @param user 対象ユーザー
     * @return 該当ユーザーの全申請一覧
     */
    List<ShiftRequest> findByUser(UserProfile user);

    /**
     * 特定の部署と月内の申請を取得
     * @param department 所属部署（例：amami）
     * @param start 月初
     * @param end 月末
     * @return 該当期間の申請リスト
     */
    List<ShiftRequest> findByDepartmentAndDateBetween(String department, LocalDate start, LocalDate end);

    /**
     * 特定ユーザー・日付の申請を取得
     * @param user 対象ユーザー
     * @param date 日付
     * @return その日の申請（存在すれば）
     */
    ShiftRequest findByUserAndDate(UserProfile user, LocalDate date);

    /**
     * 特定ユーザー・日付の申請が存在するか確認
     * @param user 対象ユーザー
     * @param date 対象日
     * @return true：申請済み、false：未申請
     */
    boolean existsByUserAndDate(UserProfile user, LocalDate date);

    /**
     * 特定ユーザー・日付の申請を削除（希望休の取消などに利用）
     * @param user 対象ユーザー
     * @param date 対象日
     */
    void deleteByUserAndDate(UserProfile user, LocalDate date);
}
