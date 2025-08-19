import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.dto.UserProfileDto;
import com.example.demo.form.ShiftGenerationForm;

@GetMapping("/generate")
public String showGeneratePage(@RequestParam(required = false, defaultValue = "amami") String department,
                               @RequestParam(required = false) YearMonth month,
                               Model model) {

    System.out.println("▶ showGeneratePage 開始");

    try {
        YearMonth targetMonth = (month != null) ? month : YearMonth.now();
        System.out.println("▶ 対象月: " + targetMonth);

        List<LocalDate> dates = IntStream.rangeClosed(1, targetMonth.lengthOfMonth())
            .mapToObj(targetMonth::atDay)
            .toList();
        System.out.println("▶ 日付リスト作成完了");

        List<UserProfileDto> users = userProfileService.getAllUserProfiles().stream()
            .filter(u -> department.equalsIgnoreCase(u.getDepartment()))
            .toList();
        System.out.println("▶ ユーザー取得完了: 件数=" + users.size());

        Map<String, String> departmentDisplayMap = Map.of(
            "amami", "天美",
            "main", "本社"
        );

        model.addAttribute("users", users);
        model.addAttribute("dateList", dates);
        model.addAttribute("department", department);
        model.addAttribute("month", targetMonth);
        model.addAttribute("departments", departmentDisplayMap.keySet());
        model.addAttribute("departmentNames", departmentDisplayMap);
        model.addAttribute("selectedDepartmentName", departmentDisplayMap.get(department));
        model.addAttribute("form", new ShiftGenerationForm());

        System.out.println("▶ モデルへのデータ格納完了");

    } catch (Exception e) {
        System.out.println("❌ エラー発生: " + e.getMessage());
        e.printStackTrace();
        model.addAttribute("errorMessage", "シフト生成画面の表示中にエラーが発生しました");
        return "error"; // エラー用テンプレート（必要なら作成）
    }

    return "shift/generate";
}

@PostMapping("/generate/request/save")
public String saveShiftRequests(@ModelAttribute("form") ShiftGenerationForm form,
                                RedirectAttributes redirectAttributes) {

    System.out.println("▶ saveShiftRequests 開始");

    try {
        System.out.println("▶ 部署: " + form.getDepartment());
        System.out.println("▶ 対象月: " + form.getTargetMonth());
        System.out.println("▶ 希望休件数: " + form.getShiftRequests().size());

        shiftGenerationService.saveShiftRequests(form.getShiftRequests());

        System.out.println("▶ 希望休保存完了");
        redirectAttributes.addFlashAttribute("message", "希望休を保存しました");

    } catch (Exception e) {
        System.out.println("❌ エラー発生: " + e.getMessage());
        e.printStackTrace();
        redirectAttributes.addFlashAttribute("error", "保存中にエラーが発生しました");
    }

    return "redirect:/shift/generate?department=" + form.getDepartment()
         + "&month=" + form.getTargetMonth();
}
