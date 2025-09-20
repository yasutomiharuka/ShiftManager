@echo off
REM === このウィンドウだけプロキシを無効化 ===
set HTTP_PROXY=
set HTTPS_PROXY=
set NO_PROXY=

REM === 途中で壊れた可能性のある Gradle Wrapper の展開キャッシュを削除（存在しなくてもOK）===
rmdir /s /q "%USERPROFILE%\.gradle\wrapper\dists\gradle-8.10.2-bin" 2>nul

REM === Gradle が取得できるか簡易チェック（バージョン表示）===
gradlew -v

REM === 代表的なタスク一覧も一応実行（任意）===
gradlew tasks

echo.
echo ------------- 完了。上のログにエラーがなければOKです -------------
pause
