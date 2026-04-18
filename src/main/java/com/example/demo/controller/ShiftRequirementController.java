// src/main/java/com/example/demo/controller/ShiftRequirementController.java
package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.form.ShiftRequirementForm;
import com.example.demo.service.ShiftRequirementService;

/**
 * 【必要人員の登録用コントローラ】
 *
 * ・シフト生成画面（generate.html）の「必要人員」入力フォームからのPOSTを受け取り、
 *   ShiftRequirementService を通じて必要人員（ShiftRequirement）を登録・更新する。
 *
 * ＜役割の整理＞
 * ・URL:
 *    - /shift/requirement/save（従来の保存）
 *    - /api/shift/requirement/save（Shiftと同様に保存系を /api 配下に寄せた保存）
 * ・HTTPメソッド: POST
 * ・リクエストボディ:
 *    - @ModelAttribute("requirementForm") ShiftRequirementForm
 *      （部署・年月・日付ごとの時間帯別必要人数を1ヶ月分まとめて保持）
 * ・処理内容:
 *    - フォーム内容をサービス層へ委譲し、DB上の ShiftRequirement をアップサート
 *    - action（DRAFT / CONFIRMED / UNCONFIRM）に応じて保存モードを切り替える
 *    - フラッシュメッセージを設定した上で、シフト生成画面へリダイレクト
 *
 * ※ シフト本体（シフト自動生成）とは処理を分離し、
 *   「必要人員だけを先に登録・調整したい」という運用にも対応できる構成とする。
 */
@Controller
public class ShiftRequirementController {

    private final ShiftRequirementService shiftRequirementService;

    public ShiftRequirementController(ShiftRequirementService shiftRequirementService) {
        this.shiftRequirementService = shiftRequirementService;
    }

    /**
     * シフト生成画面で入力された「必要人員」を保存する。
     *
     * ・画面側では th:object="${requirementForm}" としてバインドされたフォームをPOSTする想定。
     * ・保存後は、同じ部署・年月のシフト生成画面（/shift/generate）へリダイレクトする。
     *
     * ▼対応する画面側の仕様（generate.html）
     * ・ボタン押下で action パラメータを送る：
     *    - DRAFT     : 一時保存
     *    - CONFIRMED : 確定（生成に反映）
     *    - UNCONFIRM : 確定解除
     *
     * ※ /api/shift/requirement/save を「正」として運用しつつ、
     *    既存の /shift/requirement/save も互換用に同じ処理へ通す。
     */
    @PostMapping({"/api/shift/requirement/save", "/shift/requirement/save"})
    public String saveRequirements(
            @ModelAttribute("requirementForm") ShiftRequirementForm form,
            @RequestParam(name = "action", defaultValue = "DRAFT") String action,
            RedirectAttributes redirectAttributes) {

        // --- 1) action に応じて保存モードを切り替える ---
        // ここでは Controller 側で分岐し、実処理は Service に委譲する。
        // ※Service側がまだ action 対応していない場合は、一旦 saveFromForm(form) に寄せてもOK。
        if ("CONFIRMED".equalsIgnoreCase(action)) {
            // 確定（生成で参照する正式値として保存）
            shiftRequirementService.saveConfirmedFromForm(form);
            redirectAttributes.addFlashAttribute("notice", "必要人員を確定しました。");

        } else if ("UNCONFIRM".equalsIgnoreCase(action)) {
            // 確定解除（当月・部署の確定データを解除して下書きに戻す）
            shiftRequirementService.unconfirm(form.getDepartment(), form.getYear(), form.getMonth());
            redirectAttributes.addFlashAttribute("notice", "必要人員の確定解除を行いました。");

        } else {
            // 一時保存（下書きとして保存）
            shiftRequirementService.saveDraftFromForm(form);
            redirectAttributes.addFlashAttribute("notice", "必要人員を一時保存しました。");
        }

        // --- 2) 保存後は同じ部署・年月の generate 画面へ戻す ---
        // /shift/generate 側は month=yyyy-MM を受ける（YearMonth変換のため）
        // form.getMonth() が 1〜12 の数値なら "yyyy-MM" に整形して渡す
        String monthParam = String.format("%04d-%02d", form.getYear(), form.getMonth());

        // クエリは文字連結せず addAttribute で安全に付与する
        redirectAttributes.addAttribute("department", form.getDepartment());
        redirectAttributes.addAttribute("month", monthParam);

        return "redirect:/shift/generate";
    }

}
