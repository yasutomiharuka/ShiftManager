package com.example.demo.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.dto.UserProfileDto;
import com.example.demo.model.Shift;
import com.example.demo.repository.ShiftRepository;

@Service
public class ShiftService {

    private final ShiftRepository shiftRepository;

    public ShiftService(ShiftRepository shiftRepository) {
        this.shiftRepository = shiftRepository;
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
        // 表示対象全体の日付＋部署のシフト情報をまとめて取得
        List<Shift> shifts = shiftRepository.findByDepartmentAndDateIn(department, dates);

        Map<String, String> map = new HashMap<>();

        for (Shift shift : shifts) {
            Long userId = shift.getUser().getId();
            LocalDate date = shift.getDate();
            String key = userId + "_" + date;
            map.put(key, shift.getShiftType());
        }

        return map;
    }

    // 必要であれば今後ここに：
    // - シフト登録メソッド
    // - 自動シフト生成ロジック呼び出し
    // - 時間帯ごとの過不足数算出
    // などを追加していく予定
} 
