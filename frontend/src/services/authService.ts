import api, { getStoredCredentials } from './api'
import { UserDto, RegisterRequest, LoginCredentials } from '../types'

export const authService = {
  async register(data: RegisterRequest): Promise<UserDto> {
    const response = await api.post<UserDto>('/auth/register', data)
    return response.data
  },

  async login(credentials: LoginCredentials): Promise<UserDto> {
    // Используем прямой запрос к /auth/login, который возвращает пользователя
    const response = await api.post<UserDto>('/auth/login', credentials)
    return response.data
  },

  async getCurrentUser(): Promise<UserDto | null> {
    try {
      // Попытка получить информацию о текущем пользователе
      // Используем endpoint поиска по username из credentials
      const credentials = getStoredCredentials()
      if (!credentials) return null

      const response = await api.get<UserDto>(`/users/username/${credentials.username}`)
      return response.data
    } catch {
      return null
    }
  },
}

