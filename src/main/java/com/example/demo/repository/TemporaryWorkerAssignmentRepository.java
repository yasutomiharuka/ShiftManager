package com.example.demo.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.TemporaryWorkerAssignment;
import com.example.demo.model.UserProfile;

/**
 * TemporaryWorkerAssignment エンティティのリポジトリ。
 * 臨時職員の事前出勤予定を保存・取得・削除するために使用。
 */
@Repository
public interface TemporaryWorkerAssignmentRepository extends JpaRepository<TemporaryWorkerAssignment, Long> {

    /**
     * 特定ユーザーの全臨時勤務予定を取得
     *
     * @param user 対象ユーザー
     * @return 予定リスト
     */
    List<TemporaryWorkerAssignment> findByUser(UserProfile user);

    /**
     * 特定部署・対象期間の臨時職員予定を取得
     *
     * @param department 部署名
     * @param startDate 対象期間開始日
     * @param endDate 対象期間終了日
     * @return 対象期間の予定一覧
     */
    List<TemporaryWorkerAssignment> findByDepartmentAndDateBetween(
            String department,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * 特定ユーザー・日付・時間帯の予定があるか確認
     *
     * @param user 対象ユーザー
     * @param date 対象日
     * @param timeSlot 時間帯
     * @return true = 予定あり、false = 予定なし
     */
    boolean existsByUserAndDateAndTimeSlot(
            UserProfile user,
            LocalDate date,
            String timeSlot
    );

    /**
     * 特定ユーザー・日付の予定を削除（取消処理など）
     *
     * @param user 対象ユーザー
     * @param date 対象日
     */
    void deleteByUserAndDate(UserProfile user, LocalDate date);
}