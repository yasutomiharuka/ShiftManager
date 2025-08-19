package com.example.demo.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "role", nullable = false, length = 50)
    @ColumnDefault("'USER'") // デフォルトで一般ユーザーの役割
    private String role = "USER";

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "gender", length = 10)
    @ColumnDefault("'未設定'") // デフォルト値
    private String gender = "未設定";

    @Column(name = "employment_type", length = 20, nullable = false)
    @ColumnDefault("'未設定'") // デフォルト値
    private String employmentType = "未設定";

    @Column(name = "department", length = 50)
    private String department;
    
    // 各曜日の「休み」フラグ（デフォルト：false）
    @Column(name = "monday_off")
    private Boolean mondayOff = false;

    @Column(name = "tuesday_off")
    private Boolean tuesdayOff = false;

    @Column(name = "wednesday_off")
    private Boolean wednesdayOff = false;

    @Column(name = "thursday_off")
    private Boolean thursdayOff = false;

    @Column(name = "friday_off")
    private Boolean fridayOff = false;

    @Column(name = "saturday_off")
    private Boolean saturdayOff = false;

    @Column(name = "sunday_off")
    private Boolean sundayOff = false;

    
    // 各曜日の勤務時間をフィールドとして追加
    @Column(name = "monday_start_time")
    private String mondayStartTime;

    @Column(name = "monday_end_time")
    private String mondayEndTime;

    @Column(name = "tuesday_start_time")
    private String tuesdayStartTime;

    @Column(name = "tuesday_end_time")
    private String tuesdayEndTime;

    @Column(name = "wednesday_start_time")
    private String wednesdayStartTime;

    @Column(name = "wednesday_end_time")
    private String wednesdayEndTime;

    @Column(name = "thursday_start_time")
    private String thursdayStartTime;

    @Column(name = "thursday_end_time")
    private String thursdayEndTime;

    @Column(name = "friday_start_time")
    private String fridayStartTime;

    @Column(name = "friday_end_time")
    private String fridayEndTime;

    @Column(name = "saturday_start_time")
    private String saturdayStartTime;

    @Column(name = "saturday_end_time")
    private String saturdayEndTime;

    @Column(name = "sunday_start_time")
    private String sundayStartTime;

    @Column(name = "sunday_end_time")
    private String sundayEndTime;

    // デフォルトコンストラクタ（JPA用）
    public UserProfile() {
    }

    // 全フィールド（id以外）を含むコンストラクタ
    public UserProfile(String username, String password, String role, String firstName, String lastName,
                       LocalDate birthDate, String gender, String employmentType, String department) {
        this.username = username;
        this.password = password;
        this.role = role != null ? role : "USER"; // null の場合デフォルト値
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.gender = gender != null ? gender : "未設定"; // null の場合デフォルト値
        this.employmentType = employmentType != null ? employmentType : "未設定"; // null の場合デフォルト値
        this.department = department;
    }

    // エンティティが保存される前にデフォルト値をセット
    @PrePersist
    public void setDefaultValues() {
        if (this.role == null) {
            this.role = "USER";
        }
        if (this.gender == null) {
            this.gender = "未設定";
        }
        if (this.employmentType == null) {
            this.employmentType = "未設定";
        }
    }

    // Getter / Setter

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
    
    public Boolean getMondayOff() {
        return mondayOff;
    }
    public void setMondayOff(Boolean mondayOff) {
        this.mondayOff = mondayOff;
    }

    public Boolean getTuesdayOff() {
        return tuesdayOff;
    }
    public void setTuesdayOff(Boolean tuesdayOff) {
        this.tuesdayOff = tuesdayOff;
    }

    public Boolean getWednesdayOff() {
        return wednesdayOff;
    }
    public void setWednesdayOff(Boolean wednesdayOff) {
        this.wednesdayOff = wednesdayOff;
    }

    public Boolean getThursdayOff() {
        return thursdayOff;
    }
    public void setThursdayOff(Boolean thursdayOff) {
        this.thursdayOff = thursdayOff;
    }

    public Boolean getFridayOff() {
        return fridayOff;
    }
    public void setFridayOff(Boolean fridayOff) {
        this.fridayOff = fridayOff;
    }

    public Boolean getSaturdayOff() {
        return saturdayOff;
    }
    public void setSaturdayOff(Boolean saturdayOff) {
        this.saturdayOff = saturdayOff;
    }

    public Boolean getSundayOff() {
        return sundayOff;
    }
    public void setSundayOff(Boolean sundayOff) {
        this.sundayOff = sundayOff;
    }

    
    // 月曜日
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

    // 火曜日
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

    // 水曜日
    public String getWednesdayStartTime() {
        return wednesdayStartTime;
    }
    public void setWednesdayStartTime(String wednesdayStartTime) {
        this.wednesdayStartTime = wednesdayStartTime;
    }

    public String getWednesdayEndTime() {
        return wednesdayEndTime;
    }
    public void setWednesdayEndTime(String wednesdayEndTime) {
        this.wednesdayEndTime = wednesdayEndTime;
    }

    // 木曜日
    public String getThursdayStartTime() {
        return thursdayStartTime;
    }
    public void setThursdayStartTime(String thursdayStartTime) {
        this.thursdayStartTime = thursdayStartTime;
    }

    public String getThursdayEndTime() {
        return thursdayEndTime;
    }
    public void setThursdayEndTime(String thursdayEndTime) {
        this.thursdayEndTime = thursdayEndTime;
    }

    // 金曜日
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

    // 土曜日
    public String getSaturdayStartTime() {
        return saturdayStartTime;
    }
    public void setSaturdayStartTime(String saturdayStartTime) {
        this.saturdayStartTime = saturdayStartTime;
    }

    public String getSaturdayEndTime() {
        return saturdayEndTime;
    }
    public void setSaturdayEndTime(String saturdayEndTime) {
        this.saturdayEndTime = saturdayEndTime;
    }

    // 日曜日
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
    
    


    // ログ出力用
    @Override
    public String toString() {
        return "UserProfile{" +
               "id=" + id +
               ", username='" + username + '\'' +
               ", role='" + role + '\'' +
               ", firstName='" + firstName + '\'' +
               ", lastName='" + lastName + '\'' +
               ", birthDate=" + birthDate +
               ", gender='" + gender + '\'' +
               ", employmentType='" + employmentType + '\'' +
               ", department='" + department + '\'' +
               ", mondayOff=" + mondayOff +
               ", tuesdayOff=" + tuesdayOff +
               ", wednesdayOff=" + wednesdayOff +
               ", thursdayOff=" + thursdayOff +
               ", fridayOff=" + fridayOff +
               ", saturdayOff=" + saturdayOff +
               ", sundayOff=" + sundayOff +
               ", monday=" + mondayStartTime + "〜" + mondayEndTime +
               ", tuesday=" + tuesdayStartTime + "〜" + tuesdayEndTime +
               ", wednesday=" + wednesdayStartTime + "〜" + wednesdayEndTime +
               ", thursday=" + thursdayStartTime + "〜" + thursdayEndTime +
               ", friday=" + fridayStartTime + "〜" + fridayEndTime +
               ", saturday=" + saturdayStartTime + "〜" + saturdayEndTime +
               ", sunday=" + sundayStartTime + "〜" + sundayEndTime +
               '}';
    }
}
