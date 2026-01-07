/**
 * Утилиты для работы с метаданными функций
 * Сохраняем метаданные математических функций в expression для возможности восстановления графика
 */

export interface MathFunctionMetadata {
  expr: string
  fromX: number
  toX: number
  count: number
}

const MATH_PREFIX = 'MATH:'

/**
 * Кодирует метаданные математической функции в строку expression
 */
export function encodeMathFunction(metadata: MathFunctionMetadata): string {
  return `${MATH_PREFIX}${metadata.expr}|${metadata.fromX}|${metadata.toX}|${metadata.count}`
}

/**
 * Декодирует метаданные из expression
 * Возвращает null, если это не математическая функция с метаданными
 */
export function decodeMathFunction(expression: string): MathFunctionMetadata | null {
  if (!expression.startsWith(MATH_PREFIX)) {
    return null
  }

  try {
    const data = expression.substring(MATH_PREFIX.length)
    const parts = data.split('|')
    
    if (parts.length !== 4) {
      return null
    }

    return {
      expr: parts[0],
      fromX: parseFloat(parts[1]),
      toX: parseFloat(parts[2]),
      count: parseInt(parts[3], 10),
    }
  } catch {
    return null
  }
}

/**
 * Проверяет, является ли expression математической функцией с метаданными
 */
export function isMathFunctionWithMetadata(expression: string): boolean {
  return expression.startsWith(MATH_PREFIX)
}

/**
 * Извлекает чистое выражение из expression (без метаданных)
 */
export function getPureExpression(expression: string): string {
  const metadata = decodeMathFunction(expression)
  if (metadata) {
    return metadata.expr
  }
  return expression
}





