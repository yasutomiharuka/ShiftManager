package com.example.demo.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.ShiftRequirement;

public interface ShiftRequirementRepository extends JpaRepository<ShiftRequirement, Long> {

    // 特定の日付・部署の要件を取得
    List<ShiftRequirement> findByDateAndDepartment(LocalDate date, String department);

    // 部署・月単位（期間）の要件一覧を取得
    List<ShiftRequirement> findByDepartmentAndDateBetween(String department, LocalDate startDate, LocalDate endDate);

    // 時間帯で絞り込む
    List<ShiftRequirement> findByDateAndDepartmentAndTimeSlot(LocalDate date, String department, String timeSlot);
}
