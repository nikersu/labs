#!/bin/bash

# Скрипт для запуска Newman тестов
# Убедитесь, что приложение Spring Boot запущено на порту 8080

echo "=========================================="
echo "Запуск Newman тестов для Labs OOP API"
echo "=========================================="

# Проверка наличия Newman
if ! command -v newman &> /dev/null
then
    echo "Newman не установлен. Установите его командой:"
    echo "npm install -g newman"
    exit 1
fi

# Проверка наличия файлов
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COLLECTION_FILE="${SCRIPT_DIR}/LabsOOP.postman_collection.json"
ENVIRONMENT_FILE="${SCRIPT_DIR}/LabsOOP.postman_environment.json"

if [ ! -f "$COLLECTION_FILE" ]; then
    echo "Ошибка: файл коллекции $COLLECTION_FILE не найден"
    exit 1
fi

if [ ! -f "$ENVIRONMENT_FILE" ]; then
    echo "Ошибка: файл окружения $ENVIRONMENT_FILE не найден"
    exit 1
fi

# Запуск тестов
echo "Запуск тестов..."
newman run "$COLLECTION_FILE" \
    -e "$ENVIRONMENT_FILE" \
    --reporters cli,json \
    --reporter-json-export newman-report.json

EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "Все тесты прошли успешно!"
    echo "=========================================="
else
    echo ""
    echo "=========================================="
    echo "Некоторые тесты не прошли. Код выхода: $EXIT_CODE"
    echo "=========================================="
fi

exit $EXIT_CODE



