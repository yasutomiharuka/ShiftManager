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

    private static final Logger logger =
            LoggerFactory.getLogger(UserProfileService.class);

    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileService(
            UserProfileRepository userProfileRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * ユーザー情報を保存する。
     *
     * 新規登録時はパスワードを必ずハッシュ化する。
     *
     * @param userProfileDto 保存するユーザー情報のDTO
     * @return 保存されたユーザー情報のDTO
     */
    public UserProfileDto saveUserProfile(
            UserProfileDto userProfileDto
    ) {

        // DTOをエンティティに変換する。
        UserProfile userProfile =
                convertToEntity(userProfileDto);

        // 新規登録時のパスワードをハッシュ化する。
        userProfile.setPassword(
                passwordEncoder.encode(
                        userProfile.getPassword()
                )
        );

        // ユーザー情報を保存する。
        UserProfile savedUser =
                userProfileRepository.save(userProfile);

        // 保存結果をDTOへ変換して返す。
        return convertToDto(savedUser);
    }

    /**
     * ユーザー情報をIDで検索して取得する。
     *
     * @param id ユーザーID
     * @return 対象ユーザーのDTO
     */
    public Optional<UserProfileDto> getUserProfileById(
            Long id
    ) {

        return userProfileRepository
                .findById(id)
                .map(this::convertToDto);
    }

    /**
     * ユーザー情報を更新する。
     *
     * 通常の更新ではパスワードを変更しない。
     *
     * @param id ユーザーID
     * @param userProfileDto 更新するユーザー情報のDTO
     * @return 更新成功時はユーザー情報、対象不存在時は空
     */
    public Optional<UserProfileDto> updateUserProfile(
            Long id,
            UserProfileDto userProfileDto
    ) {

        return userProfileRepository
                .findById(id)
                .map(existingUser -> {

                    // DTOの入力内容を既存エンティティへ反映する。
                    updateEntityFromDto(
                            existingUser,
                            userProfileDto
                    );

                    // 更新後のエンティティを保存する。
                    return userProfileRepository.save(
                            existingUser
                    );
                })
                .map(this::convertToDto);
    }

    /**
     * ユーザー情報を更新する。
     *
     * 必要に応じてパスワードも更新する。
     *
     * @param id ユーザーID
     * @param userProfileDto 更新するユーザー情報のDTO
     * @param updatePassword パスワードを更新するかどうか
     * @return 更新成功時はユーザー情報、対象不存在時は空
     */
    public Optional<UserProfileDto> updateUserProfile(
            Long id,
            UserProfileDto userProfileDto,
            boolean updatePassword
    ) {

        return userProfileRepository
                .findById(id)
                .map(userProfile -> {

                    updateEntityFromDto(
                            userProfile,
                            userProfileDto,
                            updatePassword
                    );

                    UserProfile updatedUser =
                            userProfileRepository.save(
                                    userProfile
                            );

                    return convertToDto(updatedUser);
                });
    }

    /**
     * ユーザー情報を削除する。
     *
     * @param id ユーザーID
     * @return 削除成功時はtrue、対象不存在時はfalse
     */
    public boolean deleteUserProfile(Long id) {

        if (userProfileRepository.existsById(id)) {

            userProfileRepository.deleteById(id);

            logger.info(
                    "User profile deleted: ID {}",
                    id
            );

            return true;
        }

        logger.warn(
                "User profile not found for deletion: ID {}",
                id
        );

        return false;
    }

    /**
     * ユーザー一覧を取得する。
     *
     * @return ユーザー情報のDTO一覧
     */
    public List<UserProfileDto> getAllUserProfiles() {

        return userProfileRepository
                .findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // =========================================================
    // エンティティとDTOの変換処理
    // =========================================================

    /**
     * UserProfileエンティティをUserProfileDtoへ変換する。
     *
     * 一覧表示、詳細表示、編集画面表示などで使用する。
     *
     * @param entity 変換対象のエンティティ
     * @return ユーザー情報のDTO
     */
    private UserProfileDto convertToDto(
            UserProfile entity
    ) {

        UserProfileDto dto = new UserProfileDto();

        // ユーザー基本情報。
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setRole(entity.getRole());

        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());

        // 姓・名のフリガナ。
        dto.setFirstNameKana(entity.getFirstNameKana());
        dto.setLastNameKana(entity.getLastNameKana());

        dto.setBirthDate(entity.getBirthDate());
        dto.setGender(entity.getGender());
        dto.setEmploymentType(entity.getEmploymentType());
        dto.setDepartment(entity.getDepartment());

        // 曜日別の固定休。
        dto.setMondayOff(entity.getMondayOff());
        dto.setTuesdayOff(entity.getTuesdayOff());
        dto.setWednesdayOff(entity.getWednesdayOff());
        dto.setThursdayOff(entity.getThursdayOff());
        dto.setFridayOff(entity.getFridayOff());
        dto.setSaturdayOff(entity.getSaturdayOff());
        dto.setSundayOff(entity.getSundayOff());

        // 曜日別の勤務時間。
        dto.setMondayStartTime(
                entity.getMondayStartTime()
        );
        dto.setMondayEndTime(
                entity.getMondayEndTime()
        );

        dto.setTuesdayStartTime(
                entity.getTuesdayStartTime()
        );
        dto.setTuesdayEndTime(
                entity.getTuesdayEndTime()
        );

        dto.setWednesdayStartTime(
                entity.getWednesdayStartTime()
        );
        dto.setWednesdayEndTime(
                entity.getWednesdayEndTime()
        );

        dto.setThursdayStartTime(
                entity.getThursdayStartTime()
        );
        dto.setThursdayEndTime(
                entity.getThursdayEndTime()
        );

        dto.setFridayStartTime(
                entity.getFridayStartTime()
        );
        dto.setFridayEndTime(
                entity.getFridayEndTime()
        );

        dto.setSaturdayStartTime(
                entity.getSaturdayStartTime()
        );
        dto.setSaturdayEndTime(
                entity.getSaturdayEndTime()
        );

        dto.setSundayStartTime(
                entity.getSundayStartTime()
        );
        dto.setSundayEndTime(
                entity.getSundayEndTime()
        );

        return dto;
    }

    /**
     * UserProfileDtoをUserProfileエンティティへ変換する。
     *
     * 新規ユーザー登録時に使用する。
     *
     * @param dto 変換対象のDTO
     * @return ユーザー情報のエンティティ
     */
    private UserProfile convertToEntity(
            UserProfileDto dto
    ) {

        // 基本情報とフリガナはコンストラクターで設定する。
        UserProfile entity = new UserProfile(

                dto.getUsername(),
                dto.getPassword(),
                dto.getRole(),

                dto.getFirstName(),
                dto.getLastName(),

                // 姓・名のフリガナ。
                dto.getFirstNameKana(),
                dto.getLastNameKana(),

                dto.getBirthDate(),

                dto.getGender(),
                dto.getEmploymentType(),
                dto.getDepartment()
        );

        // 曜日別の固定休。
        entity.setMondayOff(dto.getMondayOff());
        entity.setTuesdayOff(dto.getTuesdayOff());
        entity.setWednesdayOff(dto.getWednesdayOff());
        entity.setThursdayOff(dto.getThursdayOff());
        entity.setFridayOff(dto.getFridayOff());
        entity.setSaturdayOff(dto.getSaturdayOff());
        entity.setSundayOff(dto.getSundayOff());

        // 曜日別の勤務時間。
        entity.setMondayStartTime(
                dto.getMondayStartTime()
        );
        entity.setMondayEndTime(
                dto.getMondayEndTime()
        );

        entity.setTuesdayStartTime(
                dto.getTuesdayStartTime()
        );
        entity.setTuesdayEndTime(
                dto.getTuesdayEndTime()
        );

        entity.setWednesdayStartTime(
                dto.getWednesdayStartTime()
        );
        entity.setWednesdayEndTime(
                dto.getWednesdayEndTime()
        );

        entity.setThursdayStartTime(
                dto.getThursdayStartTime()
        );
        entity.setThursdayEndTime(
                dto.getThursdayEndTime()
        );

        entity.setFridayStartTime(
                dto.getFridayStartTime()
        );
        entity.setFridayEndTime(
                dto.getFridayEndTime()
        );

        entity.setSaturdayStartTime(
                dto.getSaturdayStartTime()
        );
        entity.setSaturdayEndTime(
                dto.getSaturdayEndTime()
        );

        entity.setSundayStartTime(
                dto.getSundayStartTime()
        );
        entity.setSundayEndTime(
                dto.getSundayEndTime()
        );

        return entity;
    }

    /**
     * DTOの内容を既存エンティティへ反映する。
     *
     * 必要に応じてパスワードも更新する。
     *
     * @param entity 反映先のエンティティ
     * @param dto 反映するユーザー情報
     * @param updatePassword パスワードを更新するかどうか
     */
    private void updateEntityFromDto(
            UserProfile entity,
            UserProfileDto dto,
            boolean updatePassword
    ) {

        entity.setUsername(dto.getUsername());

        // パスワード更新が指定され、
        // 新しいパスワードが入力されている場合のみ変更する。
        if (
                updatePassword
                && dto.getPassword() != null
                && !dto.getPassword().isEmpty()
        ) {

            entity.setPassword(
                    passwordEncoder.encode(
                            dto.getPassword()
                    )
            );
        }

        // ユーザー基本情報。
        entity.setRole(dto.getRole());

        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());

        // 編集後の姓・名フリガナを反映する。
        entity.setFirstNameKana(
                dto.getFirstNameKana()
        );
        entity.setLastNameKana(
                dto.getLastNameKana()
        );

        entity.setBirthDate(dto.getBirthDate());
        entity.setGender(dto.getGender());
        entity.setEmploymentType(
                dto.getEmploymentType()
        );
        entity.setDepartment(dto.getDepartment());

        // 曜日別の固定休。
        entity.setMondayOff(dto.getMondayOff());
        entity.setTuesdayOff(dto.getTuesdayOff());
        entity.setWednesdayOff(dto.getWednesdayOff());
        entity.setThursdayOff(dto.getThursdayOff());
        entity.setFridayOff(dto.getFridayOff());
        entity.setSaturdayOff(dto.getSaturdayOff());
        entity.setSundayOff(dto.getSundayOff());

        // 曜日別の勤務時間。
        entity.setMondayStartTime(
                dto.getMondayStartTime()
        );
        entity.setMondayEndTime(
                dto.getMondayEndTime()
        );

        entity.setTuesdayStartTime(
                dto.getTuesdayStartTime()
        );
        entity.setTuesdayEndTime(
                dto.getTuesdayEndTime()
        );

        entity.setWednesdayStartTime(
                dto.getWednesdayStartTime()
        );
        entity.setWednesdayEndTime(
                dto.getWednesdayEndTime()
        );

        entity.setThursdayStartTime(
                dto.getThursdayStartTime()
        );
        entity.setThursdayEndTime(
                dto.getThursdayEndTime()
        );

        entity.setFridayStartTime(
                dto.getFridayStartTime()
        );
        entity.setFridayEndTime(
                dto.getFridayEndTime()
        );

        entity.setSaturdayStartTime(
                dto.getSaturdayStartTime()
        );
        entity.setSaturdayEndTime(
                dto.getSaturdayEndTime()
        );

        entity.setSundayStartTime(
                dto.getSundayStartTime()
        );
        entity.setSundayEndTime(
                dto.getSundayEndTime()
        );
    }

    /**
     * DTOの内容を既存エンティティへ反映する。
     *
     * 通常のユーザー編集ではパスワードを更新しない。
     *
     * @param entity 反映先のエンティティ
     * @param dto 反映するユーザー情報
     */
    private void updateEntityFromDto(
            UserProfile entity,
            UserProfileDto dto
    ) {

        updateEntityFromDto(
                entity,
                dto,
                false
        );
    }
}