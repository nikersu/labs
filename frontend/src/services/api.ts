import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios'

const API_BASE_URL = (import.meta as any).env?.VITE_API_URL || 'http://localhost:8080/api'

export const api: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Interceptor для добавления Basic Auth
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const credentials = getStoredCredentials()
    if (credentials) {
      const auth = btoa(`${credentials.username}:${credentials.password}`)
      config.headers.Authorization = `Basic ${auth}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Interceptor для обработки ошибок
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response) {
      const status = error.response.status
      const data = error.response.data as any

      if (status === 401) {
        // Неавторизован - очистить credentials
        clearStoredCredentials()
        window.location.href = '/login'
      }

      // Извлекаем сообщение об ошибке
      let message = 'Произошла ошибка'
      
      if (typeof data === 'string') {
        try {
          const parsed = JSON.parse(data)
          message = parsed.error || parsed.message || data
        } catch {
          message = data
        }
      } else if (data && typeof data === 'object') {
        message = data.error || data.message || JSON.stringify(data)
      }

      // Сохраняем оригинальный error с response для дальнейшей обработки
      const enhancedError = new Error(message) as any
      enhancedError.response = error.response
      enhancedError.originalError = error
      return Promise.reject(enhancedError)
    }

    if (error.request) {
      return Promise.reject(new Error('Сервер не отвечает. Проверьте подключение.'))
    }

    return Promise.reject(error)
  }
)

interface StoredCredentials {
  username: string
  password: string
}

export function getStoredCredentials(): StoredCredentials | null {
  const stored = localStorage.getItem('credentials')
  if (!stored) return null
  try {
    return JSON.parse(stored)
  } catch {
    return null
  }
}

export function setStoredCredentials(username: string, password: string): void {
  localStorage.setItem('credentials', JSON.stringify({ username, password }))
}

export function clearStoredCredentials(): void {
  localStorage.removeItem('credentials')
}

export default api


