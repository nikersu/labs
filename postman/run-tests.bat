@echo off
REM Скрипт для запуска Newman тестов в Windows
REM Убедитесь, что приложение Spring Boot запущено на порту 8080

echo ==========================================
echo Запуск Newman тестов для Labs OOP API
echo ==========================================

REM Проверка наличия Newman
where newman >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Newman не установлен. Установите его командой:
    echo npm install -g newman
    exit /b 1
)

REM Проверка наличия файлов
set SCRIPT_DIR=%~dp0
set COLLECTION_FILE=%SCRIPT_DIR%collections\LabsOOP.postman_collection.json
set ENVIRONMENT_FILE=%SCRIPT_DIR%environments\LabsOOP.postman_environment.json

if not exist "%COLLECTION_FILE%" (
    echo Ошибка: файл коллекции %COLLECTION_FILE% не найден
    exit /b 1
)

if not exist "%ENVIRONMENT_FILE%" (
    echo Ошибка: файл окружения %ENVIRONMENT_FILE% не найден
    exit /b 1
)

REM Запуск тестов
echo Запуск тестов...
newman run "%COLLECTION_FILE%" ^
    -e "%ENVIRONMENT_FILE%" ^
    --reporters cli,json ^
    --reporter-json-export newman-report.json

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ==========================================
    echo Все тесты прошли успешно!
    echo ==========================================
) else (
    echo.
    echo ==========================================
    echo Некоторые тесты не прошли. Код выхода: %ERRORLEVEL%
    echo ==========================================
)

exit /b %ERRORLEVEL%


