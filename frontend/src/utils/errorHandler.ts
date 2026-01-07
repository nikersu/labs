export function handleError(error: unknown): string {
  if (error instanceof Error) {
    return error.message
  }
  
  if (typeof error === 'string') {
    return error
  }

  return 'Произошла неизвестная ошибка'
}

export function extractErrorMessage(error: unknown): string {
  // Если это AxiosError, пытаемся извлечь сообщение из response
  if (error && typeof error === 'object' && 'response' in error) {
    const axiosError = error as any
    if (axiosError.response?.data) {
      const data = axiosError.response.data
      
      // Если data - это строка, пытаемся распарсить как JSON
      if (typeof data === 'string') {
        try {
          const parsed = JSON.parse(data)
          if (parsed.error) {
            return parsed.error
          }
          if (parsed.message) {
            return parsed.message
          }
        } catch {
          // Если не JSON, возвращаем строку как есть
          return data
        }
      }
      
      // Если data - это объект
      if (typeof data === 'object') {
        if (data.error) {
          return data.error
        }
        if (data.message) {
          return data.message
        }
        // Если есть текст ошибки в других полях
        if (data.statusText) {
          return data.statusText
        }
      }
    }
    
    // Пытаемся извлечь сообщение из самого error
    if (axiosError.message) {
      // Проверяем, не содержит ли сообщение полезную информацию
      const errorMessage = axiosError.message
      if (errorMessage.includes('Функция с именем') || 
          errorMessage.includes('уже существует') ||
          errorMessage.includes('разное количество точек') || 
          errorMessage.includes('разные X значения')) {
        return errorMessage
      }
    }
  }
  
  const message = handleError(error)
  
  // Улучшаем сообщения об ошибках
  if (message.includes('400') || message.includes('Bad Request')) {
    // Если сообщение уже содержит полезную информацию, возвращаем его
    if (message.includes('Функция с именем') || 
        message.includes('уже существует') ||
        message.includes('разное количество точек') || 
        message.includes('разные X значения')) {
      return message
    }
    return 'Некорректный запрос. Проверьте данные и попробуйте снова.'
  }
  
  if (message.includes('401') || message.includes('Unauthorized')) {
    return 'Неверное имя пользователя или пароль'
  }
  
  if (message.includes('403') || message.includes('Forbidden')) {
    return 'Доступ запрещен'
  }
  
  if (message.includes('404') || message.includes('Not Found')) {
    return 'Ресурс не найден'
  }
  
  if (message.includes('409') || message.includes('Conflict')) {
    return 'Конфликт данных. Возможно, ресурс уже существует'
  }
  
  if (message.includes('500') || message.includes('Internal Server Error')) {
    return 'Ошибка сервера. Попробуйте позже'
  }

  return message
}

