package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.UserProfile;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
	
	// 特定のユーザー1人を取得
    Optional<UserProfile> findByUsername(String username);
    
    // 全ユーザーを取得（必要であれば独自メソッド追加）
    List<UserProfile> findAll();

    // 特定の雇用形態だけ取得（例: 正社員）
    List<UserProfile> findByEmploymentType(String employmentType);

    // 所属部署で絞る
    List<UserProfile> findByDepartment(String department);

    // 所属部署＋雇用形態で絞る（例: 天美のパート）
    List<UserProfile> findByDepartmentAndEmploymentType(String department, String employmentType);

}
