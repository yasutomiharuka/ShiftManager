package com.example.demo.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Shift;
import com.example.demo.model.ShiftRequest;
import com.example.demo.model.ShiftRequirement;
import com.example.demo.model.TemporaryWorkerAssignment;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.ShiftRepository;
import com.example.demo.repository.ShiftRequestRepository;
import com.example.demo.repository.ShiftRequirementRepository;
import com.example.demo.repository.TemporaryWorkerAssignmentRepository;
import com.example.demo.repository.UserProfileRepository;

@Service
public class ShiftGenerationService {

    private final UserProfileRepository userProfileRepository;
    private final ShiftRequirementRepository shiftRequirementRepository;
    private final ShiftRequestRepository shiftRequestRepository;
    private final TemporaryWorkerAssignmentRepository temporaryWorkerAssignmentRepository;
    private final ShiftRepository shiftRepository;
    
    // コンストラクタ　Spring がリポジトリを渡し、フィールドに代入
    public ShiftGenerationService(UserProfileRepository userProfileRepository,
                                  ShiftRequirementRepository shiftRequirementRepository,
                                  ShiftRequestRepository shiftRequestRepository,
                                  TemporaryWorkerAssignmentRepository temporaryWorkerAssignmentRepository,
                                  ShiftRepository shiftRepository) {
        this.userProfileRepository = userProfileRepository;
        this.shiftRequirementRepository = shiftRequirementRepository;
        this.shiftRequestRepository = shiftRequestRepository;
        this.temporaryWorkerAssignmentRepository = temporaryWorkerAssignmentRepository;
        this.shiftRepository = shiftRepository;
    }

    /**
     * 指定された年月と部署に対してシフトを自動生成する
     * ポイント:
     *  - LocalDate#datesUntil で「日付ストリーム」を作成し、ラムダ内で date が実質finalとなるようにする
     *  - 1日単位で生成した Shift をまとめて saveAll することで DB I/O を削減（マイクロ最適化）
     *  - 事前臨時 → 正/パ の順に割当（今は単純な割当。将来はルール拡張想定）
     */
    @Transactional
    public void generateShifts(int year, int month, String department) {
        // ① 対象月の開始日と終了日（末日）を算出
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        // ② 対象部署のユーザー一覧を取得（正社員・パート・臨時すべて含む想定）
        //    ※要件に応じて employmentType などで事前に絞ることも可能
        List<UserProfile> users = userProfileRepository.findByDepartment(department);

        // ③ 希望休・有給、必要人員、臨時事前指定を月範囲でまとめて取得 → 日付でグルーピング（N+1 クエリ抑制）
        Map<LocalDate, List<ShiftRequest>> requestMap =
                shiftRequestRepository.findByDepartmentAndDateBetween(department, start, end)
                        .stream()
                        .collect(Collectors.groupingBy(ShiftRequest::getDate));

        Map<LocalDate, List<ShiftRequirement>> requirementMap =
                shiftRequirementRepository.findByDepartmentAndDateBetween(department, start, end)
                        .stream()
                        .collect(Collectors.groupingBy(ShiftRequirement::getDate));

        Map<LocalDate, List<TemporaryWorkerAssignment>> tempMap =
                temporaryWorkerAssignmentRepository.findByDepartmentAndDateBetween(department, start, end)
                        .stream()
                        .collect(Collectors.groupingBy(TemporaryWorkerAssignment::getDate));

        // ④ start〜end（含む）をストリームで走査（datesUntil は上限非包含なので end.plusDays(1)）
        start.datesUntil(end.plusDays(1)).forEach(date -> {

            // ④-1 当日分の必要人員（時間帯別）を取得。なければ空リスト
            List<ShiftRequirement> requirements = requirementMap.getOrDefault(date, List.of());

            // ④-2 当日分の希望休・有給リストを一度だけ参照して使い回し（Map 参照の繰り返し回避）
            List<ShiftRequest> dailyRequests = requestMap.getOrDefault(date, List.of());

            // ④-3 本日作成する Shift を一括確保（1日分を saveAll でまとめて保存）
            List<Shift> shiftsToSaveToday = new ArrayList<>();

            // ④-4 各時間帯の必要人員要件を処理
            for (ShiftRequirement req : requirements) {
                // ④-4-1 時間帯・必要人数を取得
                String slot = req.getTimeSlot();
                int requiredCount = req.getRequiredCount();

                // ④-4-2 事前割当の臨時職員を当時間帯に絞り込む
                List<TemporaryWorkerAssignment> tempsForSlot =
                        tempMap.getOrDefault(date, List.of()).stream()
                                .filter(t -> slot.equals(t.getTimeSlot()))
                                .limit(requiredCount) // 必要数を超えないように制限
                                .toList();

                // ④-4-3 事前臨時を Shift 化（「臨(確)」として保存キューに追加）
                tempsForSlot.forEach(temp -> {
                    Shift shift = new Shift();
                    shift.setUser(temp.getUser());       // 担当者 = 事前指定の臨時
                    shift.setDate(date);                 // 当日
                    shift.setDepartment(department);     // 部署
                    shift.setTimeSlot(slot);             // 時間帯
                    shift.setShiftType("臨(確)");        // 勤務種別 = 事前確定の臨時
                    shift.setTemporary(true);            // 臨時フラグ
                    shift.setFixed(true);                // 事前指定フラグ
                    shiftsToSaveToday.add(shift);        // 後でまとめて保存
                });

                // ④-4-4 残りの必要数を算出
                int remaining = requiredCount - tempsForSlot.size();

                // ④-4-5 残りがあれば、正社員/パートから「勤務可能」な人を抽出して割り当て
                if (remaining > 0) {
                    // ここでは簡易に空いている人を上から採用
                    List<UserProfile> assignCandidates = users.stream()
                            .filter(u -> isAvailable(u, date, slot, dailyRequests))
                            .limit(remaining)
                            .toList();

                    // ④-4-6 候補者を Shift 化（とりあえず「日」勤として割当。将来の拡張ポイント）
                    assignCandidates.forEach(u -> {
                        Shift shift = new Shift();
                        shift.setUser(u);                   // 担当者 = 空いている正/パ
                        shift.setDate(date);                // 当日
                        shift.setDepartment(department);    // 部署
                        shift.setTimeSlot(slot);            // 時間帯
                        shift.setShiftType("日");           // 仮ロジック：日勤
                        shift.setTemporary(false);          // 臨時ではない
                        shift.setFixed(false);              // 事前指定ではない
                        shiftsToSaveToday.add(shift);       // 後でまとめて保存
                    });
                }
            }

            // ④-5 1日分をまとめて保存（DB I/O を削減）
            if (!shiftsToSaveToday.isEmpty()) {
                shiftRepository.saveAll(shiftsToSaveToday);
            }
        });
    }

    /**
     * 指定ユーザーが指定日・時間帯に勤務可能かを判定
     * 現状ロジック:
     *  - 曜日固定休（UserProfile の *Off フラグ）を優先的にブロック
     *  - 当日の希望休・有給（ShiftRequest）に含まれている場合はブロック
     *  - ※時間帯の整合や「既に他の時間帯で割当済みか」のチェックは今後の拡張ポイント
     */
    private boolean isAvailable(UserProfile user, LocalDate date, String timeSlot, List<ShiftRequest> requests) {
        // ① 曜日固定休の判定（true なら不可）
        switch (date.getDayOfWeek()) {
            case MONDAY    -> { if (Boolean.TRUE.equals(user.getMondayOff()))    return false; }
            case TUESDAY   -> { if (Boolean.TRUE.equals(user.getTuesdayOff()))   return false; }
            case WEDNESDAY -> { if (Boolean.TRUE.equals(user.getWednesdayOff())) return false; }
            case THURSDAY  -> { if (Boolean.TRUE.equals(user.getThursdayOff()))  return false; }
            case FRIDAY    -> { if (Boolean.TRUE.equals(user.getFridayOff()))    return false; }
            case SATURDAY  -> { if (Boolean.TRUE.equals(user.getSaturdayOff()))  return false; }
            case SUNDAY    -> { if (Boolean.TRUE.equals(user.getSundayOff()))    return false; }
        }

        // ② 希望休・有給の判定（当日そのユーザーが何らかの申請をしていれば不可）
        //    ※将来的に timeSlot 別の有給/半休 等が入る場合は、timeSlot での突合に変更してください
        boolean requestedOff = requests.stream()
                .anyMatch(r -> r.getUser().getId().equals(user.getId()));
        if (requestedOff) return false;

        // ③ ここで他の制約（例: 連勤上限、夜勤明けなど）を考慮する拡張が可能
        //    - 例: 「夜勤含め最大５連勤」「月9休」などの集計ベース制約は、別途当月シフトの集計が必要

        // ④ いずれにも該当しなければ勤務可能
        return true;
    }
}
