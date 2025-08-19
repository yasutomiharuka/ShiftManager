package com.example.demo.service;

import java.time.LocalDate;
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
     */
    @Transactional
    public void generateShifts(int year, int month, String department) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        // 対象の正社員・パート・臨時を全て取得
        List<UserProfile> users = userProfileRepository.findByDepartment(department);

        // 日付ごとの希望休・有給、臨時割当、必要人員数を事前にMap化
        Map<LocalDate, List<ShiftRequest>> requestMap = shiftRequestRepository.findByDepartmentAndDateBetween(department, start, end)
                .stream().collect(Collectors.groupingBy(ShiftRequest::getDate));

        Map<LocalDate, List<ShiftRequirement>> requirementMap = shiftRequirementRepository.findByDepartmentAndDateBetween(department, start, end)
                .stream().collect(Collectors.groupingBy(ShiftRequirement::getDate));

        Map<LocalDate, List<TemporaryWorkerAssignment>> tempMap = temporaryWorkerAssignmentRepository.findByDepartmentAndDateBetween(department, start, end)
                .stream().collect(Collectors.groupingBy(TemporaryWorkerAssignment::getDate));

        // 各日付に対して処理
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {

            List<ShiftRequirement> requirements = requirementMap.getOrDefault(date, List.of());

            for (ShiftRequirement req : requirements) {
                String slot = req.getTimeSlot();
                int count = req.getRequiredCount();

                // まず事前割り当ての臨時職員を使用
                List<TemporaryWorkerAssignment> temps = tempMap.getOrDefault(date, List.of())
                        .stream().filter(t -> t.getTimeSlot().equals(slot)).limit(count).toList();

                for (TemporaryWorkerAssignment temp : temps) {
                    Shift shift = new Shift();
                    shift.setUser(temp.getUser());
                    shift.setDate(date);
                    shift.setDepartment(department);
                    shift.setTimeSlot(slot);
                    shift.setShiftType("臨(確)");
                    shift.setTemporary(true);
                    shift.setFixed(true);
                    shiftRepository.save(shift);
                }

                int remaining = count - temps.size();

                // 残りを正社員／パートから割り当て（簡易的に空いてる人を選定）
                if (remaining > 0) {
                    List<UserProfile> available = users.stream()
                            .filter(u -> isAvailable(u, date, slot, requestMap.getOrDefault(date, List.of())))
                            .limit(remaining)
                            .toList();

                    for (UserProfile u : available) {
                        Shift shift = new Shift();
                        shift.setUser(u);
                        shift.setDate(date);
                        shift.setDepartment(department);
                        shift.setTimeSlot(slot);
                        shift.setShiftType("日"); // 仮に日勤
                        shift.setTemporary(false);
                        shift.setFixed(false);
                        shiftRepository.save(shift);
                    }
                }
            }
        }
    }

    /**
     * 指定ユーザーが指定日・時間帯に勤務可能かを判定（希望休や固定休日を考慮）
     */
    private boolean isAvailable(UserProfile user, LocalDate date, String timeSlot, List<ShiftRequest> requests) {
        // 曜日固定休の判定
        switch (date.getDayOfWeek()) {
            case MONDAY -> { if (Boolean.TRUE.equals(user.getMondayOff())) return false; }
            case TUESDAY -> { if (Boolean.TRUE.equals(user.getTuesdayOff())) return false; }
            case WEDNESDAY -> { if (Boolean.TRUE.equals(user.getWednesdayOff())) return false; }
            case THURSDAY -> { if (Boolean.TRUE.equals(user.getThursdayOff())) return false; }
            case FRIDAY -> { if (Boolean.TRUE.equals(user.getFridayOff())) return false; }
            case SATURDAY -> { if (Boolean.TRUE.equals(user.getSaturdayOff())) return false; }
            case SUNDAY -> { if (Boolean.TRUE.equals(user.getSundayOff())) return false; }
        }

        // 希望休・有給
        return requests.stream().noneMatch(r -> r.getUser().getId().equals(user.getId()));
    }
}
