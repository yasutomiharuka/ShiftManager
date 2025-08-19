package com.example.demo.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

public class UserProfileDto {

    private Long id;

    @NotBlank(message = "ユーザー名は必須です。")
    @Size(max = 100, message = "ユーザー名は100文字以内で入力してください。")
    private String username;

    @NotBlank(message = "パスワードは必須です。")
    @Size(min = 4, message = "パスワードは4文字以上で入力してください。")
    private String password;

    @NotBlank(message = "ユーザーロールは必須です。")
    @Size(max = 50, message = "ユーザーロールは50文字以内で入力してください。")
    private String role;

    @NotBlank(message = "姓は必須です。")
    @Size(max = 50, message = "姓は50文字以内で入力してください。")
    private String firstName;

    @NotBlank(message = "名は必須です。")
    @Size(max = 50, message = "名は50文字以内で入力してください。")
    private String lastName;

    @NotNull(message = "生年月日は必須です。")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;

    @NotBlank(message = "性別は必須です。")
    @Pattern(regexp = "^(male|female)$", message = "性別は 'male' または 'female' のいずれかを指定してください。")
    private String gender;

    @NotBlank(message = "雇用形態は必須です。")
    @Size(max = 20, message = "雇用形態は20文字以内で入力してください。")
    private String employmentType;

    @NotBlank(message = "所属は必須です。")
    @Size(max = 50, message = "所属は50文字以内で入力してください。")
    private String department;
    
    // パートのみ必要
    
    // 各曜日の休みフラグ
    private Boolean mondayOff;
    private Boolean tuesdayOff;
    private Boolean wednesdayOff;
    private Boolean thursdayOff;
    private Boolean fridayOff;
    private Boolean saturdayOff;
    private Boolean sundayOff;

    // 各曜日の勤務時間（開始・終了）
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


    // デフォルトコンストラクター
    public UserProfileDto() {
        // パート勤務 フォーム初期表示で「休みにしない状態」をデフォルトに
        this.mondayOff = false;
        this.tuesdayOff = false;
        this.wednesdayOff = false;
        this.thursdayOff = false;
        this.fridayOff = false;
        this.saturdayOff = false;
        this.sundayOff = false;
    }

    // すべてのフィールドを受け取るコンストラクター テストコードや変換処理用に使われる
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
            String mondayStartTime, String mondayEndTime,
            String tuesdayStartTime, String tuesdayEndTime,
            String wednesdayStartTime, String wednesdayEndTime,
            String thursdayStartTime, String thursdayEndTime,
            String fridayStartTime, String fridayEndTime,
            String saturdayStartTime, String saturdayEndTime,
            String sundayStartTime, String sundayEndTime,
            Boolean mondayOff, Boolean tuesdayOff, Boolean wednesdayOff,
            Boolean thursdayOff, Boolean fridayOff, Boolean saturdayOff, Boolean sundayOff
    ) {
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
        
        this.mondayOff = mondayOff;
        this.tuesdayOff = tuesdayOff;
        this.wednesdayOff = wednesdayOff;
        this.thursdayOff = thursdayOff;
        this.fridayOff = fridayOff;
        this.saturdayOff = saturdayOff;
        this.sundayOff = sundayOff;
        
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

    // Getters and Setters
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
    
    public String getMondayStartTime() { return mondayStartTime; }
    public void setMondayStartTime(String mondayStartTime) { this.mondayStartTime = mondayStartTime; }

    public String getMondayEndTime() { return mondayEndTime; }
    public void setMondayEndTime(String mondayEndTime) { this.mondayEndTime = mondayEndTime; }

    public String getTuesdayStartTime() { return tuesdayStartTime; }
    public void setTuesdayStartTime(String tuesdayStartTime) { this.tuesdayStartTime = tuesdayStartTime; }

    public String getTuesdayEndTime() { return tuesdayEndTime; }
    public void setTuesdayEndTime(String tuesdayEndTime) { this.tuesdayEndTime = tuesdayEndTime; }

    public String getWednesdayStartTime() { return wednesdayStartTime; }
    public void setWednesdayStartTime(String wednesdayStartTime) { this.wednesdayStartTime = wednesdayStartTime; }

    public String getWednesdayEndTime() { return wednesdayEndTime; }
    public void setWednesdayEndTime(String wednesdayEndTime) { this.wednesdayEndTime = wednesdayEndTime; }

    public String getThursdayStartTime() { return thursdayStartTime; }
    public void setThursdayStartTime(String thursdayStartTime) { this.thursdayStartTime = thursdayStartTime; }

    public String getThursdayEndTime() { return thursdayEndTime; }
    public void setThursdayEndTime(String thursdayEndTime) { this.thursdayEndTime = thursdayEndTime; }

    public String getFridayStartTime() { return fridayStartTime; }
    public void setFridayStartTime(String fridayStartTime) { this.fridayStartTime = fridayStartTime; }

    public String getFridayEndTime() { return fridayEndTime; }
    public void setFridayEndTime(String fridayEndTime) { this.fridayEndTime = fridayEndTime; }

    public String getSaturdayStartTime() { return saturdayStartTime; }
    public void setSaturdayStartTime(String saturdayStartTime) { this.saturdayStartTime = saturdayStartTime; }

    public String getSaturdayEndTime() { return saturdayEndTime; }
    public void setSaturdayEndTime(String saturdayEndTime) { this.saturdayEndTime = saturdayEndTime; }

    public String getSundayStartTime() { return sundayStartTime; }
    public void setSundayStartTime(String sundayStartTime) { this.sundayStartTime = sundayStartTime; }

    public String getSundayEndTime() { return sundayEndTime; }
    public void setSundayEndTime(String sundayEndTime) { this.sundayEndTime = sundayEndTime; }
    
    public Boolean getMondayOff() { return mondayOff; }
    public void setMondayOff(Boolean mondayOff) { this.mondayOff = mondayOff; }

    public Boolean getTuesdayOff() { return tuesdayOff; }
    public void setTuesdayOff(Boolean tuesdayOff) { this.tuesdayOff = tuesdayOff; }

    public Boolean getWednesdayOff() { return wednesdayOff; }
    public void setWednesdayOff(Boolean wednesdayOff) { this.wednesdayOff = wednesdayOff; }

    public Boolean getThursdayOff() { return thursdayOff; }
    public void setThursdayOff(Boolean thursdayOff) { this.thursdayOff = thursdayOff; }

    public Boolean getFridayOff() { return fridayOff; }
    public void setFridayOff(Boolean fridayOff) { this.fridayOff = fridayOff; }

    public Boolean getSaturdayOff() { return saturdayOff; }
    public void setSaturdayOff(Boolean saturdayOff) { this.saturdayOff = saturdayOff; }

    public Boolean getSundayOff() { return sundayOff; }
    public void setSundayOff(Boolean sundayOff) { this.sundayOff = sundayOff; }

}
