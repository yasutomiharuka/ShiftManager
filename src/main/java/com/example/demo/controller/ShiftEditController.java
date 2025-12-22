package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam; // ← 追加：action 受け取り用
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.form.ShiftGenerationForm;
import com.example.demo.model.Shift;
import com.example.demo.service.ShiftService;

/**
 * ShiftEditController
 *
 * シフトの保存・編集処理を担当するコントローラ。
 * - 一時保存（DRAFT）
 * - 確定保存（CONFIRMED）
 * - 確定解除（CONFIRMED → DRAFT）
 * - セル削除（クリア）
 *
 * 画面は generate.html（シフト生成画面）から送られてくるフォームを処理する。
 * 保存処理が完了したら PRG パターンで再度 /shift/generate にリダイレクトする。※shiftGenerationController
 *
 * ▼変更点
 * ・フロント側のボタンはすべて /api/shift/request/save にPOSTし、name="action" の値で分岐
 * ・本クラスのルートも /api/shift/request に変更
 *
 * ▼ステータスの考え方
 * ・DRAFT      … 下書き（途中経過）
 * ・CONFIRMED  … 確定（その時点での正式なシフト）
 *   → 画面表示時も CONFIRMED を優先して採用する（ShiftService 側の statusRank で制御）
 * ・UNCONFIRM  … いったん確定されたシフトを DRAFT に戻す操作
 */
@Controller
@RequestMapping("/api/shift/request") // ★ 変更：/shift → /api/shift/request
public class ShiftEditController {

    private final ShiftService shiftService;

    public ShiftEditController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    /**
     * 共通保存エンドポイント
     * - action=DRAFT      : 一時保存（下書き）
     * - action=CONFIRMED  : 確定保存（正式シフトとして保存）
     * - action=UNCONFIRM  : 確定解除（確定→下書きに戻す）
     *
     * 例）generate.html のボタン：
     *  <button type="submit" formaction="/api/shift/request/save" name="action" value="DRAFT">一時保存</button>
     *  <button type="submit" formaction="/api/shift/request/save" name="action" value="CONFIRMED">確定</button>
     *  <button type="submit" formaction="/api/shift/request/save" name="action" value="UNCONFIRM">確定解除</button>
     */
    @PostMapping("/save")
    public String save(ShiftGenerationForm form,
                       @RequestParam(name = "action", defaultValue = "CONFIRMED") String action,
                       @RequestParam(name = "department", required = false) String departmentParam,
                       RedirectAttributes ra) {

        // ★ここで受け取った内容をログに出す
        System.out.println("DEBUG ShiftEditController.save: action=" + action
            + ", deptParam=" + departmentParam
            + ", formDept=" + (form != null ? form.getDepartment() : null)
            + ", shifts=" + (form != null ? form.getShifts() : null));

        // 部署の取得（フォーム優先／パラメータを保険に）
        String resolvedDepartment = null;
        if (form != null && StringUtils.hasText(form.getDepartment())) {
            resolvedDepartment = form.getDepartment().trim();
        } else if (StringUtils.hasText(departmentParam)) {
            resolvedDepartment = departmentParam.trim();
        }

        // 部署が取れなければ保存不可
        if (!StringUtils.hasText(resolvedDepartment)) {
            ra.addFlashAttribute("notice", "部署が選択されていません。部署を選択してから保存してください。");
            return redirectToGenerate(form, ra);
        }

        // サービス層では form.department を必ず参照するため、ここで統一
        form.setDepartment(resolvedDepartment);

        // action の大小文字・余白を吸収
        final String normalizedAction =
                (action == null ? "CONFIRMED" : action.trim().toUpperCase());

        String notice;

        switch (normalizedAction) {
        case "DRAFT":
            // ▼ 下書き保存
            // 1セル = 1レコードのまま、status=DRAFT として saveShifts() で upsert する。
            shiftService.saveShifts(form, Shift.Status.DRAFT);
            notice = "シフトを一時保存しました。";
            break;

        case "UNCONFIRM":
            // ▼ 確定解除（CONFIRMED → DRAFT）
            // 対象月・部署の CONFIRMED を DRAFT に戻す処理は ShiftService#unconfirmShifts に委譲。
            // ※基本的には「確定後に修正したくなったとき」に使用する。
            shiftService.unconfirmShifts(form);
            notice = "シフトの確定を解除しました。";
            break;

        case "CONFIRMED":
            // ▼ 確定保存
            // 画面の入力内容を status=CONFIRMED で upsert。
            // これにより、同一セルの既存 DRAFT を上書きして「正式なシフト」として扱う。
            shiftService.saveShifts(form, Shift.Status.CONFIRMED);
            notice = "シフトを確定しました。";
            break;

        default:
            // ▼ 想定外アクションは CONFIRMED と同等で扱う
            // （万が一フロントの name/action 設定が漏れても、保存自体は行われるようにしておく）
            shiftService.saveShifts(form, Shift.Status.CONFIRMED);
            notice = "シフトを確定しました。";
            break;
        }

        // 画面上部に通知を出す（Flash Attribute）
        ra.addFlashAttribute("notice", notice);

        // PRG：保存後は生成画面へ戻る
        return redirectToGenerate(form, ra);
    }


    // 共通：保存後は generate 画面にリダイレクト
    private String redirectToGenerate(ShiftGenerationForm form, RedirectAttributes ra) {
        if (form != null) {
            if (StringUtils.hasText(form.getDepartment())) {
                ra.addAttribute("department", form.getDepartment());
            }
            if (form.getTargetMonth() != null) {
                ra.addAttribute("month", form.getTargetMonth().toString());
            }
        }
        return "redirect:/shift/generate";
    }
}
