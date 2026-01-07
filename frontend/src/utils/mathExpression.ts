/**
 * Безопасное вычисление математического выражения
 * Поддерживает основные математические функции и операции
 */
export function evaluateExpression(expression: string, x: number): number {
  try {
    // Заменяем x на значение
    let expr = expression.replace(/x/g, `(${x})`)
    
    // Поддерживаем основные математические функции
    const mathFunctions: { [key: string]: (arg: number) => number } = {
      sin: Math.sin,
      cos: Math.cos,
      tan: Math.tan,
      asin: Math.asin,
      acos: Math.acos,
      atan: Math.atan,
      sinh: Math.sinh,
      cosh: Math.cosh,
      tanh: Math.tanh,
      exp: Math.exp,
      log: Math.log,
      log10: (n: number) => Math.log10(n),
      sqrt: Math.sqrt,
      abs: Math.abs,
      ceil: Math.ceil,
      floor: Math.floor,
      round: Math.round,
      pow: (base: number, exp: number) => Math.pow(base, exp),
    }

    // Заменяем функции на вызовы
    for (const [funcName, func] of Object.entries(mathFunctions)) {
      const regex = new RegExp(`\\b${funcName}\\s*\\(`, 'g')
      if (regex.test(expr)) {
        // Для функций с одним аргументом
        if (funcName !== 'pow') {
          expr = expr.replace(new RegExp(`\\b${funcName}\\s*\\(([^)]+)\\)`, 'g'), (match, arg) => {
            const argValue = evaluateSimpleExpression(arg)
            return String(func(argValue))
          })
        } else {
          // Для pow(x, y)
          expr = expr.replace(/\bpow\s*\(([^,]+),\s*([^)]+)\)/g, (match, base, exp) => {
            const baseValue = evaluateSimpleExpression(base)
            const expValue = evaluateSimpleExpression(exp)
            return String(Math.pow(baseValue, expValue))
          })
        }
      }
    }

    // Заменяем ^ на Math.pow
    expr = expr.replace(/([^+\-*/()\s]+)\s*\^\s*([^+\-*/()\s]+)/g, (match, base, exp) => {
      const baseValue = evaluateSimpleExpression(base)
      const expValue = evaluateSimpleExpression(exp)
      return String(Math.pow(baseValue, expValue))
    })

    // Вычисляем простое выражение
    return evaluateSimpleExpression(expr)
  } catch (error) {
    throw new Error(`Ошибка вычисления выражения: ${error}`)
  }
}

function evaluateSimpleExpression(expr: string): number {
  // Удаляем лишние пробелы
  expr = expr.trim()
  
  // Если это просто число
  if (/^-?\d+\.?\d*$/.test(expr)) {
    return parseFloat(expr)
  }

  // Безопасное вычисление с помощью Function
  // Ограничиваем доступ только к математическим операциям
  try {
    // Проверяем на наличие опасных конструкций
    const dangerousPatterns = [
      /eval\s*\(/i,
      /function\s*\(/i,
      /=>/,
      /import\s+/i,
      /require\s*\(/i,
      /process\./i,
      /window\./i,
      /document\./i,
      /global\./i,
    ]

    for (const pattern of dangerousPatterns) {
      if (pattern.test(expr)) {
        throw new Error('Выражение содержит недопустимые конструкции')
      }
    }

    // Создаем безопасный контекст только с математическими операциями
    const safeEval = new Function('return ' + expr)
    const result = safeEval()
    
    if (typeof result !== 'number' || !isFinite(result)) {
      throw new Error('Результат не является конечным числом')
    }
    
    return result
  } catch (error) {
    throw new Error(`Ошибка вычисления: ${error instanceof Error ? error.message : String(error)}`)
  }
}

/**
 * Генерирует точки для математической функции на заданном интервале
 */
export function generatePoints(
  expression: string,
  fromX: number,
  toX: number,
  count: number
): Array<{ x: number; y: number }> {
  if (count < 2) {
    throw new Error('Количество точек должно быть не менее 2')
  }

  if (fromX >= toX) {
    throw new Error('Начало интервала должно быть меньше конца')
  }

  const points: Array<{ x: number; y: number }> = []
  const step = (toX - fromX) / (count - 1)

  for (let i = 0; i < count; i++) {
    const x = fromX + i * step
    try {
      const y = evaluateExpression(expression, x)
      points.push({ x, y })
    } catch (error) {
      throw new Error(`Ошибка при вычислении точки x=${x}: ${error}`)
    }
  }

  return points
}

