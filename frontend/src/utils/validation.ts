export function validateUsername(username: string): string | null {
  if (!username || username.trim().length === 0) {
    return 'Имя пользователя не может быть пустым'
  }
  if (username.length < 3) {
    return 'Имя пользователя должно содержать минимум 3 символа'
  }
  if (username.length > 50) {
    return 'Имя пользователя не должно превышать 50 символов'
  }
  return null
}

export function validatePassword(password: string): string | null {
  if (!password || password.length === 0) {
    return 'Пароль не может быть пустым'
  }
  if (password.length < 4) {
    return 'Пароль должен содержать минимум 4 символа'
  }
  return null
}

export function validateFunctionName(name: string): string | null {
  if (!name || name.trim().length === 0) {
    return 'Название функции не может быть пустым'
  }
  if (name.length > 100) {
    return 'Название функции не должно превышать 100 символов'
  }
  return null
}

export function validateExpression(expression: string): string | null {
  if (!expression || expression.trim().length === 0) {
    return 'Выражение не может быть пустым'
  }
  return null
}

export function validatePoint(x: number, y: number): string | null {
  if (isNaN(x) || !isFinite(x)) {
    return 'X должен быть числом'
  }
  if (isNaN(y) || !isFinite(y)) {
    return 'Y должен быть числом'
  }
  return null
}

export function validatePoints(points: Array<{ x: number; y: number }>): string | null {
  if (!points || points.length === 0) {
    return 'Добавьте хотя бы одну точку'
  }
  if (points.length < 2) {
    return 'Добавьте минимум 2 точки'
  }

  // Проверка на сортировку по X
  for (let i = 1; i < points.length; i++) {
    if (points[i].x <= points[i - 1].x) {
      return 'Точки должны быть отсортированы по X в порядке возрастания'
    }
  }

  return null
}





