@echo off
REM === ���̃E�B���h�E�����v���L�V�𖳌��� ===
set HTTP_PROXY=
set HTTPS_PROXY=
set NO_PROXY=

REM === �r���ŉ�ꂽ�\���̂��� Gradle Wrapper �̓W�J�L���b�V�����폜�i���݂��Ȃ��Ă�OK�j===
rmdir /s /q "%USERPROFILE%\.gradle\wrapper\dists\gradle-8.10.2-bin" 2>nul

REM === Gradle ���擾�ł��邩�ȈՃ`�F�b�N�i�o�[�W�����\���j===
gradlew -v

REM === ��\�I�ȃ^�X�N�ꗗ���ꉞ���s�i�C�Ӂj===
gradlew tasks

echo.
echo ------------- �����B��̃��O�ɃG���[���Ȃ����OK�ł� -------------
pause
