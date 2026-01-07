-- Миграция: добавление полей для хранения функций целиком как JSON
-- Это позволяет сохранять функции не поточечно, а целиком

ALTER TABLE functions 
ADD COLUMN IF NOT EXISTS x_values TEXT,
ADD COLUMN IF NOT EXISTS y_values TEXT,
ADD COLUMN IF NOT EXISTS count INTEGER;

-- Комментарии для документации
COMMENT ON COLUMN functions.x_values IS 'JSON массив X значений функции (сохранение целиком, не поточечно)';
COMMENT ON COLUMN functions.y_values IS 'JSON массив Y значений функции (сохранение целиком, не поточечно)';
COMMENT ON COLUMN functions.count IS 'Количество точек в функции';


