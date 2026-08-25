package com.example.demo.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * ユーザー情報を画面・API・Service間で受け渡すためのDTO。
 *
 * 【主な使用箇所】
 * ・ユーザー登録画面
 * ・ユーザー登録確認画面
 * ・ユーザー編集画面
 * ・ユーザー登録・更新API
 *
 * 【バリデーションの方針】
 * ・ユーザー名、氏名、所属などの共通項目は、登録・編集の両方で検証する。
 * ・パスワードは新規登録時のみ必須とする。
 * ・編集画面では既存パスワードを変更しないため、
 *   パスワード未入力をエラーとしない。
 */
public class UserProfileDto {

    /**
     * 新規ユーザー登録時専用のバリデーショングループ。
     *
     * 【使用目的】
     * 登録時と編集時で、パスワードの必須条件を切り替える。
     *
     * 【登録時】
     * Controller側で以下のように指定する。
     *
     * @Validated({
     *     Default.class,
     *     UserProfileDto.Create.class
     * })
     *
     * これにより、共通項目の検証に加えて、
     * パスワードの必須・最小文字数チェックを実行する。
     *
     * 【編集時】
     * @Valid または Default グループのみを使用する。
     * Create グループを指定しないため、
     * パスワード未入力でも編集できる。
     */
    public interface Create {
        // バリデーションの適用対象を区別するためのグループ。
        // メソッドの定義は不要。
    }

    // ユーザーID
    // 新規登録時は未設定、編集時は対象ユーザーのIDを保持する。
    private Long id;

    // ユーザー名
    // 登録・編集の両方で必須とし、最大100文字まで許可する。
    @NotBlank(message = "ユーザー名は必須です。")
    @Size(
            max = 100,
            message = "ユーザー名は100文字以内で入力してください。"
    )
    private String username;

    /**
     * ログイン用パスワード。
     *
     * 【登録時】
     * ・必須。
     * ・4文字以上。
     *
     * 【編集時】
     * ・パスワード入力欄がないため、未入力でもエラーにしない。
     * ・既存パスワードの変更は行わない。
     *
     * groups = Create.class を指定することで、
     * 新規登録時のみ下記のバリデーションが適用される。
     *
     * ※ Controller側でも登録時に Create グループを
     *    指定する必要がある。
     */
    @NotBlank(
            message = "パスワードは必須です。",
            groups = Create.class
    )
    @Size(
            min = 4,
            message = "パスワードは4文字以上で入力してください。",
            groups = Create.class
    )
    private String password;

    // ユーザーロール
    // 例：ADMIN = 管理者、USER = 一般ユーザー。
    @NotBlank(message = "ユーザーロールは必須です。")
    @Size(
            max = 50,
            message = "ユーザーロールは50文字以内で入力してください。"
    )
    private String role;

    // 姓
    // 登録・編集の両方で必須とし、最大50文字まで許可する。
    @NotBlank(message = "姓は必須です。")
    @Size(
            max = 50,
            message = "姓は50文字以内で入力してください。"
    )
    private String firstName;

    // 名
    // 登録・編集の両方で必須とし、最大50文字まで許可する。
    @NotBlank(message = "名は必須です。")
    @Size(
            max = 50,
            message = "名は50文字以内で入力してください。"
    )
    private String lastName;

    // 生年月日
    // 画面から送信される yyyy-MM-dd 形式の日付を LocalDate に変換する。
    @NotNull(message = "生年月日は必須です。")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;

    // 性別
    // 画面側の選択値に合わせ、male または female のみ許可する。
    @NotBlank(message = "性別は必須です。")
    @Pattern(
            regexp = "^(male|female)$",
            message = "性別は 'male' または 'female' のいずれかを指定してください。"
    )
    private String gender;

    // 雇用形態
    // 例：fulltime = 正社員、parttime = パート。
    @NotBlank(message = "雇用形態は必須です。")
    @Size(
            max = 20,
            message = "雇用形態は20文字以内で入力してください。"
    )
    private String employmentType;

    // 所属部署
    // 例：amami = 天美、main = 本社。
    @NotBlank(message = "所属は必須です。")
    @Size(
            max = 50,
            message = "所属は50文字以内で入力してください。"
    )
    private String department;

    // =========================================================
    // パートのみ必要
    // =========================================================

    /**
     * 各曜日の休みフラグ。
     *
     * true  : 対象曜日は固定休み。
     * false : 対象曜日は勤務可能。
     *
     * パート勤務者の曜日別固定スケジュールで使用する。
     */
    private Boolean mondayOff;
    private Boolean tuesdayOff;
    private Boolean wednesdayOff;
    private Boolean thursdayOff;
    private Boolean fridayOff;
    private Boolean saturdayOff;
    private Boolean sundayOff;

    /**
     * 各曜日の勤務時間（開始・終了）。
     *
     * 【入力例】
     * ・09:00 ～ 14:00
     * ・14:00 ～ 18:00
     *
     * 対象曜日が休みの場合は、開始・終了時間を未入力として扱う。
     */
    private String mondayStartTime;
    private String mondayEndTime;

    private String tuesdayStartTime;
    private String tuesdayEndTime;

    private String wednesdayStartTime;
    private String wednesdayEndTime;

    private String thursdayStartTime;
    private String thursdayEndTime;

    private String fridayStartTime;
    private String fridayEndTime;

    private String saturdayStartTime;
    private String saturdayEndTime;

    private String sundayStartTime;
    private String sundayEndTime;

    // =========================================================
    // コンストラクター
    // =========================================================

    /**
     * デフォルトコンストラクター。
     *
     * パート勤務フォームの初期表示では、
     * すべての曜日を「休みにしない状態」に設定する。
     *
     * Spring MVC のフォームバインドでも使用する。
     */
    public UserProfileDto() {

        // パート勤務 フォーム初期表示で
        // 「休みにしない状態」をデフォルトにする。
        this.mondayOff = false;
        this.tuesdayOff = false;
        this.wednesdayOff = false;
        this.thursdayOff = false;
        this.fridayOff = false;
        this.saturdayOff = false;
        this.sundayOff = false;
    }

    /**
     * すべてのフィールドを受け取るコンストラクター。
     *
     * テストコードや変換処理用に使用する。
     *
     * @param id ユーザーID
     * @param username ユーザー名
     * @param password ログイン用パスワード
     * @param role ユーザーロール
     * @param firstName 姓
     * @param lastName 名
     * @param birthDate 生年月日
     * @param gender 性別
     * @param employmentType 雇用形態
     * @param department 所属部署
     * @param mondayStartTime 月曜日の勤務開始時刻
     * @param mondayEndTime 月曜日の勤務終了時刻
     * @param tuesdayStartTime 火曜日の勤務開始時刻
     * @param tuesdayEndTime 火曜日の勤務終了時刻
     * @param wednesdayStartTime 水曜日の勤務開始時刻
     * @param wednesdayEndTime 水曜日の勤務終了時刻
     * @param thursdayStartTime 木曜日の勤務開始時刻
     * @param thursdayEndTime 木曜日の勤務終了時刻
     * @param fridayStartTime 金曜日の勤務開始時刻
     * @param fridayEndTime 金曜日の勤務終了時刻
     * @param saturdayStartTime 土曜日の勤務開始時刻
     * @param saturdayEndTime 土曜日の勤務終了時刻
     * @param sundayStartTime 日曜日の勤務開始時刻
     * @param sundayEndTime 日曜日の勤務終了時刻
     * @param mondayOff 月曜日の休みフラグ
     * @param tuesdayOff 火曜日の休みフラグ
     * @param wednesdayOff 水曜日の休みフラグ
     * @param thursdayOff 木曜日の休みフラグ
     * @param fridayOff 金曜日の休みフラグ
     * @param saturdayOff 土曜日の休みフラグ
     * @param sundayOff 日曜日の休みフラグ
     */
    public UserProfileDto(

            Long id,

            String username,
            String password,
            String role,
            String firstName,
            String lastName,

            LocalDate birthDate,

            String gender,
            String employmentType,
            String department,

            String mondayStartTime,
            String mondayEndTime,

            String tuesdayStartTime,
            String tuesdayEndTime,

            String wednesdayStartTime,
            String wednesdayEndTime,

            String thursdayStartTime,
            String thursdayEndTime,

            String fridayStartTime,
            String fridayEndTime,

            String saturdayStartTime,
            String saturdayEndTime,

            String sundayStartTime,
            String sundayEndTime,

            Boolean mondayOff,
            Boolean tuesdayOff,
            Boolean wednesdayOff,
            Boolean thursdayOff,
            Boolean fridayOff,
            Boolean saturdayOff,
            Boolean sundayOff
    ) {

        // ユーザー基本情報
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.gender = gender;
        this.employmentType = employmentType;
        this.department = department;

        // 各曜日の休みフラグ
        this.mondayOff = mondayOff;
        this.tuesdayOff = tuesdayOff;
        this.wednesdayOff = wednesdayOff;
        this.thursdayOff = thursdayOff;
        this.fridayOff = fridayOff;
        this.saturdayOff = saturdayOff;
        this.sundayOff = sundayOff;

        // 各曜日の勤務開始・終了時刻
        this.mondayStartTime = mondayStartTime;
        this.mondayEndTime = mondayEndTime;

        this.tuesdayStartTime = tuesdayStartTime;
        this.tuesdayEndTime = tuesdayEndTime;

        this.wednesdayStartTime = wednesdayStartTime;
        this.wednesdayEndTime = wednesdayEndTime;

        this.thursdayStartTime = thursdayStartTime;
        this.thursdayEndTime = thursdayEndTime;

        this.fridayStartTime = fridayStartTime;
        this.fridayEndTime = fridayEndTime;

        this.saturdayStartTime = saturdayStartTime;
        this.saturdayEndTime = saturdayEndTime;

        this.sundayStartTime = sundayStartTime;
        this.sundayEndTime = sundayEndTime;
    }

    // =========================================================
    // Getters and Setters：基本情報
    // =========================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    // =========================================================
    // Getters and Setters：各曜日の勤務時間
    // =========================================================

    // 月曜日の勤務開始・終了時刻
    public String getMondayStartTime() {
        return mondayStartTime;
    }

    public void setMondayStartTime(String mondayStartTime) {
        this.mondayStartTime = mondayStartTime;
    }

    public String getMondayEndTime() {
        return mondayEndTime;
    }

    public void setMondayEndTime(String mondayEndTime) {
        this.mondayEndTime = mondayEndTime;
    }

    // 火曜日の勤務開始・終了時刻
    public String getTuesdayStartTime() {
        return tuesdayStartTime;
    }

    public void setTuesdayStartTime(String tuesdayStartTime) {
        this.tuesdayStartTime = tuesdayStartTime;
    }

    public String getTuesdayEndTime() {
        return tuesdayEndTime;
    }

    public void setTuesdayEndTime(String tuesdayEndTime) {
        this.tuesdayEndTime = tuesdayEndTime;
    }

    // 水曜日の勤務開始・終了時刻
    public String getWednesdayStartTime() {
        return wednesdayStartTime;
    }

    public void setWednesdayStartTime(
            String wednesdayStartTime
    ) {
        this.wednesdayStartTime = wednesdayStartTime;
    }

    public String getWednesdayEndTime() {
        return wednesdayEndTime;
    }

    public void setWednesdayEndTime(
            String wednesdayEndTime
    ) {
        this.wednesdayEndTime = wednesdayEndTime;
    }

    // 木曜日の勤務開始・終了時刻
    public String getThursdayStartTime() {
        return thursdayStartTime;
    }

    public void setThursdayStartTime(
            String thursdayStartTime
    ) {
        this.thursdayStartTime = thursdayStartTime;
    }

    public String getThursdayEndTime() {
        return thursdayEndTime;
    }

    public void setThursdayEndTime(String thursdayEndTime) {
        this.thursdayEndTime = thursdayEndTime;
    }

    // 金曜日の勤務開始・終了時刻
    public String getFridayStartTime() {
        return fridayStartTime;
    }

    public void setFridayStartTime(String fridayStartTime) {
        this.fridayStartTime = fridayStartTime;
    }

    public String getFridayEndTime() {
        return fridayEndTime;
    }

    public void setFridayEndTime(String fridayEndTime) {
        this.fridayEndTime = fridayEndTime;
    }

    // 土曜日の勤務開始・終了時刻
    public String getSaturdayStartTime() {
        return saturdayStartTime;
    }

    public void setSaturdayStartTime(
            String saturdayStartTime
    ) {
        this.saturdayStartTime = saturdayStartTime;
    }

    public String getSaturdayEndTime() {
        return saturdayEndTime;
    }

    public void setSaturdayEndTime(
            String saturdayEndTime
    ) {
        this.saturdayEndTime = saturdayEndTime;
    }

    // 日曜日の勤務開始・終了時刻
    public String getSundayStartTime() {
        return sundayStartTime;
    }

    public void setSundayStartTime(String sundayStartTime) {
        this.sundayStartTime = sundayStartTime;
    }

    public String getSundayEndTime() {
        return sundayEndTime;
    }

    public void setSundayEndTime(String sundayEndTime) {
        this.sundayEndTime = sundayEndTime;
    }

    // =========================================================
    // Getters and Setters：各曜日の休みフラグ
    // =========================================================

    // 月曜日の休みフラグ
    public Boolean getMondayOff() {
        return mondayOff;
    }

    public void setMondayOff(Boolean mondayOff) {
        this.mondayOff = mondayOff;
    }

    // 火曜日の休みフラグ
    public Boolean getTuesdayOff() {
        return tuesdayOff;
    }

    public void setTuesdayOff(Boolean tuesdayOff) {
        this.tuesdayOff = tuesdayOff;
    }

    // 水曜日の休みフラグ
    public Boolean getWednesdayOff() {
        return wednesdayOff;
    }

    public void setWednesdayOff(Boolean wednesdayOff) {
        this.wednesdayOff = wednesdayOff;
    }

    // 木曜日の休みフラグ
    public Boolean getThursdayOff() {
        return thursdayOff;
    }

    public void setThursdayOff(Boolean thursdayOff) {
        this.thursdayOff = thursdayOff;
    }

    // 金曜日の休みフラグ
    public Boolean getFridayOff() {
        return fridayOff;
    }

    public void setFridayOff(Boolean fridayOff) {
        this.fridayOff = fridayOff;
    }

    // 土曜日の休みフラグ
    public Boolean getSaturdayOff() {
        return saturdayOff;
    }

    public void setSaturdayOff(Boolean saturdayOff) {
        this.saturdayOff = saturdayOff;
    }

    // 日曜日の休みフラグ
    public Boolean getSundayOff() {
        return sundayOff;
    }

    public void setSundayOff(Boolean sundayOff) {
        this.sundayOff = sundayOff;
    }
}