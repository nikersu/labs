# Настройка Postman для LabsOOP API

## Импорт коллекции и окружения

1. **Откройте Postman**

2. **Импортируйте коллекцию:**
   - Нажмите `Import` (или `Ctrl+O`)
   - Выберите файл `collections/LabsOOP.postman_collection.json`
   - Нажмите `Import`

3. **Импортируйте окружение:**
   - Нажмите `Import` (или `Ctrl+O`)
   - Выберите файл `environments/LabsOOP.postman_environment.json`
   - Нажмите `Import`

4. **Выберите окружение:**
   - В правом верхнем углу Postman нажмите на выпадающий список окружений
   - Выберите `LabsOOP Local`

## Настройка Basic Authentication

**Важно:** Вам нужно добавить Basic Authentication к каждому запросу вручную.

### Как добавить Basic Auth к запросу:

1. Откройте любой запрос в коллекции `LabsOOP API`
2. Перейдите на вкладку `Authorization`
3. В выпадающем списке `Type` выберите `Basic Auth`
4. Введите:
   - **Username:** `admin`
   - **Password:** `admin`
5. Нажмите `Save`

### Альтернативный способ (для всех запросов сразу):

1. Выберите всю коллекцию `LabsOOP API` (кликните на название коллекции)
2. Перейдите на вкладку `Authorization`
3. Выберите `Basic Auth` и введите `admin` / `admin`
4. Это применит авторизацию ко всем запросам в коллекции

**Примечание:** Убедитесь, что в базе данных существует пользователь с ролью ADMIN и учетными данными `admin:admin`. Если нет, создайте такого пользователя или используйте существующие учетные данные ADMIN.

## Запуск тестов

### Через Postman:

1. Убедитесь, что сервер запущен на `http://localhost:8080`
2. Откройте коллекцию `LabsOOP API`
3. Нажмите на три точки (⋮) рядом с коллекцией
4. Выберите `Run collection`
5. Нажмите `Run LabsOOP API`

### Через Newman (командная строка):

**Windows:**
```bash
cd postman
run-tests.bat
```

**Linux/Mac:**
```bash
cd postman
chmod +x run-tests.sh
./run-tests.sh
```

**Требования:**
- Установлен Node.js
- Установлен Newman: `npm install -g newman`
- Сервер запущен на `http://localhost:8080`

## Структура коллекции

Коллекция содержит следующие группы запросов:

- **Users** - управление пользователями (Create, Get All, Get By Id, Get By Username, Update, Delete)
- **Functions** - управление функциями (Create, Get All, Get By Id, Get By User, Search, Update, Delete)
- **Points** - управление точками (Create, Get All, Get By Function, Get By Range, Get By X, Update, Delete)
- **Search** - поиск (User By Username, Function By Id, Point By X, Functions By User, Points By Function, Hierarchy Breadth First, Hierarchy Depth First)

## Важные замечания

1. **Порядок выполнения:** Некоторые запросы зависят от результатов предыдущих (например, создание функции требует существующего userId). Рекомендуется запускать коллекцию целиком, чтобы тесты выполнялись в правильном порядке.

2. **Переменные окружения:** Коллекция автоматически сохраняет ID созданных сущностей в переменные окружения для использования в последующих запросах.

3. **Basic Auth:** Все запросы автоматически используют Basic Authentication с учетными данными из переменных окружения. **Важно:** Для создания пользователей через `POST /api/users` требуется роль ADMIN.

4. **Создание пользователей:** 
   - `POST /api/users` - создание пользователя (требуется роль ADMIN)
   - `POST /api/auth/register` - регистрация нового пользователя (не требует аутентификации, создает пользователя с ролью USER)

5. **Base URL:** По умолчанию используется `http://localhost:8080`. Если ваш сервер работает на другом порту, измените переменную `baseUrl` в окружении.

6. **Пользователь ADMIN:** Убедитесь, что в базе данных существует пользователь с ролью ADMIN и учетными данными `admin:admin` (или измените `authUsername` и `authPassword` в окружении на существующие учетные данные ADMIN).

