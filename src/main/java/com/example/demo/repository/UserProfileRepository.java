package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.UserProfile;

@Repository
public interface UserProfileRepository
        extends JpaRepository<UserProfile, Long> {

    // ----------------------------------------------------------------
    // 有効・無効を問わず取得
    // ----------------------------------------------------------------

    /**
     * ユーザー名からユーザーを取得する。
     *
     * ユーザー登録時の重複チェックなど、
     * 無効化済みユーザーも含めて確認する場合に使用する。
     *
     * @param username ユーザー名
     * @return 該当するユーザー。存在しない場合は空
     */
    Optional<UserProfile> findByUsername(
            String username
    );

    /**
     * 有効・無効を問わず、全ユーザーを取得する。
     *
     * 無効化済みユーザーを含むため、
     * 管理者向けの確認処理などで使用する。
     *
     * @return 全ユーザーの一覧
     */
    @Override
    List<UserProfile> findAll();


    // ----------------------------------------------------------------
    // 有効なユーザーだけを取得
    // ----------------------------------------------------------------

    /**
     * 有効なユーザーをユーザー名から取得する。
     *
     * activeがtrueのユーザーだけが取得対象になる。
     * 主にログイン認証で使用する。
     *
     * @param username ユーザー名
     * @return 該当する有効なユーザー。存在しない場合は空
     */
    Optional<UserProfile> findByUsernameAndActiveTrue(
            String username
    );

    /**
     * 有効なユーザーをすべて取得する。
     *
     * activeがtrueのユーザーだけが取得対象になる。
     * 通常のユーザー一覧やシフト生成対象の取得で使用する。
     *
     * @return 有効なユーザーの一覧
     */
    List<UserProfile> findAllByActiveTrue();

    /**
     * 有効なユーザーを雇用形態で絞り込む。
     *
     * 例：有効な正社員だけを取得する。
     *
     * @param employmentType 雇用形態
     * @return 条件に該当する有効なユーザーの一覧
     */
    List<UserProfile> findByEmploymentTypeAndActiveTrue(
            String employmentType
    );

    /**
     * 有効なユーザーを所属部署で絞り込む。
     *
     * シフト一覧やシフト生成対象の取得で使用する。
     *
     * @param department 所属部署
     * @return 指定した部署に所属する有効なユーザーの一覧
     */
    List<UserProfile> findByDepartmentAndActiveTrue(
            String department
    );

    /**
     * 有効なユーザーを所属部署と雇用形態で絞り込む。
     *
     * 例：天美に所属する有効なパート職員を取得する。
     *
     * @param department 所属部署
     * @param employmentType 雇用形態
     * @return 条件に該当する有効なユーザーの一覧
     */
    List<UserProfile>
            findByDepartmentAndEmploymentTypeAndActiveTrue(
                    String department,
                    String employmentType
            );


    // ----------------------------------------------------------------
    // 無効化されたユーザーを取得
    // ----------------------------------------------------------------

    /**
     * 無効化されたユーザーをすべて取得する。
     *
     * activeがfalseのユーザーだけが取得対象になる。
     * ユーザー管理画面の「履歴」タブで使用する。
     *
     * @return 無効化されたユーザーの一覧
     */
    List<UserProfile> findAllByActiveFalse();
}