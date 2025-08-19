package com.example.demo.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.dto.UserProfileDto;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.UserProfileRepository;

@Service
public class UserProfileService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileService.class);
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileService(UserProfileRepository userProfileRepository, PasswordEncoder passwordEncoder) {
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * ユーザー情報を保存 (新規登録時はパスワードを必ずハッシュ化)
     * @param userProfileDto - 保存するユーザー情報のDTO
     * @return 保存されたユーザー情報のDTO
     * UserProfileDto → UserProfile
     */
    public UserProfileDto saveUserProfile(UserProfileDto userProfileDto) {
        UserProfile userProfile = convertToEntity(userProfileDto); // DTOをエンティティに変換
        userProfile.setPassword(passwordEncoder.encode(userProfile.getPassword())); // パスワードをハッシュ化
        UserProfile savedUser = userProfileRepository.save(userProfile);
        return convertToDto(savedUser); // DTOに変換して返す
    }
    
    /**
     * ユーザー情報をidで検索して取得
     * @param id - ユーザーID
     * @return Optional<UserProfileDto>
     * UserProfile → UserProfileDto
     */
    public Optional<UserProfileDto> getUserProfileById(Long id) {
        return userProfileRepository.findById(id).map(this::convertToDto);
    }
    

    /**
     * ユーザー情報を更新
     * @param id - ユーザーID
     * @param userProfileDto - 更新するユーザー情報のDTO
     * @return 更新成功時は Optional<UserProfileDto>、失敗時は Optional.empty()
     * UserProfileDto → UserProfile
     */
    public Optional<UserProfileDto> updateUserProfile(Long id, UserProfileDto userProfileDto) {
        return userProfileRepository.findById(id)
                .map(existingUser -> {
                    updateEntityFromDto(existingUser, userProfileDto); // DTOの内容を適用
                    return userProfileRepository.save(existingUser); // 更新後のエンティティを保存
                })
                .map(this::convertToDto); // 更新後のデータをDTOに変換して返す
    }


    /**
     * ユーザー情報を更新 (パスワード更新のオプション付き)
     * @param id - ユーザーID
     * @param userProfileDto - 更新するユーザー情報のDTO
     * @param updatePassword - パスワードを更新するかどうか
     * @return 更新成功時は Optional<UserProfileDto>、失敗時は Optional.empty()
     */ 
    public Optional<UserProfileDto> updateUserProfile(Long id, UserProfileDto userProfileDto, boolean updatePassword) {
        return userProfileRepository.findById(id)
            .map(userProfile -> {
                updateEntityFromDto(userProfile, userProfileDto, updatePassword);
                return convertToDto(userProfileRepository.save(userProfile));
            });
    }
    
    
    /**
     * ユーザー情報を削除
     * @param id - ユーザーID
     * @return 削除成功時は true、失敗時は false
     */
    public boolean deleteUserProfile(Long id) {
        if (userProfileRepository.existsById(id)) {
            userProfileRepository.deleteById(id);
            logger.info("User profile deleted: ID {}", id);
            return true;
        }
        logger.warn("User profile not found for deletion: ID {}", id);
        return false;
    }

    /**
     * ユーザー一覧を取得
     * @return ユーザー情報のリスト
     * List<UserProfile> → List<UserProfileDto>
     */
    public List<UserProfileDto> getAllUserProfiles() {
        return userProfileRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    // ========== 変換処理 ==========
    // サービスクラス (UserProfileService) の各メソッドで直接変換処理は行わない

    /**
     * UserProfile → UserProfileDto
     * @param userProfile - 変換対象のエンティティ
     * @return UserProfileDto
     */
    private UserProfileDto convertToDto(UserProfile entity) {
        UserProfileDto dto = new UserProfileDto();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setRole(entity.getRole());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setBirthDate(entity.getBirthDate());
        dto.setGender(entity.getGender());
        dto.setEmploymentType(entity.getEmploymentType());
        dto.setDepartment(entity.getDepartment());

        dto.setMondayOff(entity.getMondayOff());
        dto.setTuesdayOff(entity.getTuesdayOff());
        dto.setWednesdayOff(entity.getWednesdayOff());
        dto.setThursdayOff(entity.getThursdayOff());
        dto.setFridayOff(entity.getFridayOff());
        dto.setSaturdayOff(entity.getSaturdayOff());
        dto.setSundayOff(entity.getSundayOff());

        dto.setMondayStartTime(entity.getMondayStartTime());
        dto.setMondayEndTime(entity.getMondayEndTime());
        dto.setTuesdayStartTime(entity.getTuesdayStartTime());
        dto.setTuesdayEndTime(entity.getTuesdayEndTime());
        dto.setWednesdayStartTime(entity.getWednesdayStartTime());
        dto.setWednesdayEndTime(entity.getWednesdayEndTime());
        dto.setThursdayStartTime(entity.getThursdayStartTime());
        dto.setThursdayEndTime(entity.getThursdayEndTime());
        dto.setFridayStartTime(entity.getFridayStartTime());
        dto.setFridayEndTime(entity.getFridayEndTime());
        dto.setSaturdayStartTime(entity.getSaturdayStartTime());
        dto.setSaturdayEndTime(entity.getSaturdayEndTime());
        dto.setSundayStartTime(entity.getSundayStartTime());
        dto.setSundayEndTime(entity.getSundayEndTime());

        return dto;
     }

    /**
     * UserProfileDto → UserProfile
     * @param userProfileDto - 変換対象のDTO
     * @param updatePassword - パスワードをハッシュ化するかどうか
     * @return UserProfile
     */
    private UserProfile convertToEntity(UserProfileDto dto) {
        UserProfile entity = new UserProfile(
            dto.getUsername(),
            dto.getPassword(),
            dto.getRole(),
            dto.getFirstName(),
            dto.getLastName(),
            dto.getBirthDate(),
            dto.getGender(),
            dto.getEmploymentType(),
            dto.getDepartment()
        );
        
        entity.setMondayOff(dto.getMondayOff());
        entity.setTuesdayOff(dto.getTuesdayOff());
        entity.setWednesdayOff(dto.getWednesdayOff());
        entity.setThursdayOff(dto.getThursdayOff());
        entity.setFridayOff(dto.getFridayOff());
        entity.setSaturdayOff(dto.getSaturdayOff());
        entity.setSundayOff(dto.getSundayOff());

        entity.setMondayStartTime(dto.getMondayStartTime());
        entity.setMondayEndTime(dto.getMondayEndTime());
        entity.setTuesdayStartTime(dto.getTuesdayStartTime());
        entity.setTuesdayEndTime(dto.getTuesdayEndTime());
        entity.setWednesdayStartTime(dto.getWednesdayStartTime());
        entity.setWednesdayEndTime(dto.getWednesdayEndTime());
        entity.setThursdayStartTime(dto.getThursdayStartTime());
        entity.setThursdayEndTime(dto.getThursdayEndTime());
        entity.setFridayStartTime(dto.getFridayStartTime());
        entity.setFridayEndTime(dto.getFridayEndTime());
        entity.setSaturdayStartTime(dto.getSaturdayStartTime());
        entity.setSaturdayEndTime(dto.getSaturdayEndTime());
        entity.setSundayStartTime(dto.getSundayStartTime());
        entity.setSundayEndTime(dto.getSundayEndTime());     

        return entity;
    }
      

    /**
     * DTO の内容をエンティティに適用 (更新用・パスワード更新オプションつき)
     * @param entity - 反映先のエンティティ
     * @param dto - 反映するデータ
     * @param updatePassword - パスワードを更新するかどうか
     */
    private void updateEntityFromDto(UserProfile entity, UserProfileDto dto, boolean updatePassword) {
        entity.setUsername(dto.getUsername());
        if (updatePassword && dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            entity.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        entity.setRole(dto.getRole());
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setBirthDate(dto.getBirthDate());
        entity.setGender(dto.getGender());
        entity.setEmploymentType(dto.getEmploymentType());
        entity.setDepartment(dto.getDepartment());
        
        entity.setMondayOff(dto.getMondayOff());
        entity.setTuesdayOff(dto.getTuesdayOff());
        entity.setWednesdayOff(dto.getWednesdayOff());
        entity.setThursdayOff(dto.getThursdayOff());
        entity.setFridayOff(dto.getFridayOff());
        entity.setSaturdayOff(dto.getSaturdayOff());
        entity.setSundayOff(dto.getSundayOff());

        entity.setMondayStartTime(dto.getMondayStartTime());
        entity.setMondayEndTime(dto.getMondayEndTime());
        entity.setTuesdayStartTime(dto.getTuesdayStartTime());
        entity.setTuesdayEndTime(dto.getTuesdayEndTime());
        entity.setWednesdayStartTime(dto.getWednesdayStartTime());
        entity.setWednesdayEndTime(dto.getWednesdayEndTime());
        entity.setThursdayStartTime(dto.getThursdayStartTime());
        entity.setThursdayEndTime(dto.getThursdayEndTime());
        entity.setFridayStartTime(dto.getFridayStartTime());
        entity.setFridayEndTime(dto.getFridayEndTime());
        entity.setSaturdayStartTime(dto.getSaturdayStartTime());
        entity.setSaturdayEndTime(dto.getSaturdayEndTime());
        entity.setSundayStartTime(dto.getSundayStartTime());
        entity.setSundayEndTime(dto.getSundayEndTime());    
    }
    
    /**
     * DTO の内容をエンティティに適用 (更新用, デフォルトはパスワードを更新しない)
     */
    private void updateEntityFromDto(UserProfile entity, UserProfileDto dto) {
        updateEntityFromDto(entity, dto, false); // デフォルトでパスワードを更新しない
    }

}



