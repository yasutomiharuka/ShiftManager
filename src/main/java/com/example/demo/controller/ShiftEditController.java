package com.example.demo.controller;

import org.springframework.stereotype.Controller;
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
 * 保存処理が完了したら PRG パターンで再度 /shift/generate にリダイレクトする。
 *
 * ▼変更点
 * ・フロント側のボタンはすべて /api/shift/request/save にPOSTし、name="action" の値で分岐
 * ・本クラスのルートも /api/shift/request に変更
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
     * - action=CONFIRMED  : 確定保存
     * - action=UNCONFIRM  : 確定解除（確定→下書きに戻す）
     *
     * 例）generate.html のボタン：
     *  <button type="submit" formaction="/api/shift/request/save" name="action" value="DRAFT">一時保存</button>
     */
    @PostMapping("/save")
    public String save(ShiftGenerationForm form,
                       @RequestParam(name = "action", defaultValue = "CONFIRMED") String action,
                       RedirectAttributes ra) {

        // action の大小文字・余白を吸収
        final String normalizedAction = action == null ? "CONFIRMED" : action.trim().toUpperCase();

        String notice;
        switch (normalizedAction) {
        case "DRAFT":
            // ▼ 下書き保存
            shiftService.saveShifts(form, Shift.Status.DRAFT);
            notice = "シフトを一時保存しました。";
            break;

        case "UNCONFIRM":
            // ▼ 確定解除（CONFIRMED → DRAFT）
            shiftService.unconfirmShifts(form);
            notice = "シフトの確定を解除しました。";
            break;

        case "CONFIRMED":
            // ▼ 確定保存
            shiftService.saveShifts(form, Shift.Status.CONFIRMED);
            notice = "シフトを確定しました。";
            break;

        default:
            // ▼ 想定外アクションは CONFIRMED と同等で扱う
            shiftService.saveShifts(form, Shift.Status.CONFIRMED);
            notice = "シフトを確定しました。";
            break;
        }

        // 画面上部に通知を出す（Flash Attribute）
        ra.addFlashAttribute("notice", notice);

        // PRG：保存後は生成画面へ戻る
        return redirectToGenerate(form);
    }

    /* =========================
     *  旧：個別エンドポイント群
     *  （フロントの action 集約に伴い未使用。履歴として残す）
     * =========================

    /**
     * 一時保存処理
     * - 画面で入力されたシフトを DRAFT 状態で保存
     * /
    // @PostMapping("/request/draft")
    // public String saveDraft(ShiftGenerationForm form, RedirectAttributes ra) {
    //     shiftService.saveShifts(form, Shift.Status.DRAFT);
    //     ra.addFlashAttribute("notice", "シフトを一時保存しました。");
    //     return redirectToGenerate(form);
    // }

    /**
     * 確定保存処理
     * - 画面で入力されたシフトを CONFIRMED 状態で保存
     * /
    // @PostMapping("/request/confirm")
    // public String saveConfirm(ShiftGenerationForm form, RedirectAttributes ra) {
    //     shiftService.saveShifts(form, Shift.Status.CONFIRMED);
    //     ra.addFlashAttribute("notice", "シフトを確定しました。");
    //     return redirectToGenerate(form);
    // }

    /**
     * 確定解除処理
     * - 既に CONFIRMED 状態のシフトを DRAFT に戻す
     * /
    // @PostMapping("/request/unconfirm")
    // public String unconfirm(ShiftGenerationForm form, RedirectAttributes ra) {
    //     shiftService.unconfirmShifts(form);
    //     ra.addFlashAttribute("notice", "シフトの確定を解除しました。");
    //     return redirectToGenerate(form);
    // }

    /**
     * セル削除処理
     * - 入力が "-" のセルを DB から削除する
     * /
    // @PostMapping("/request/clear")
    // public String clearShifts(ShiftGenerationForm form, RedirectAttributes ra) {
    //     shiftService.clearShifts(form);
    //     ra.addFlashAttribute("notice", "シフトを削除しました。");
    //     return redirectToGenerate(form);
    // }
    */

    // 共通：保存後は generate 画面にリダイレクト
    private String redirectToGenerate(ShiftGenerationForm form) {
        // ▼ department, targetMonth はフォームの hidden から渡ってくる想定
        String dept = (form.getDepartment() != null) ? form.getDepartment() : "amami";
        String month = (form.getTargetMonth() != null) ? form.getTargetMonth().toString() : "";
        return "redirect:/shift/generate?department=" + dept + "&month=" + month;
    }
}
