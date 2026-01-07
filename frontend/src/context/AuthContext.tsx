import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { UserDto, LoginCredentials } from '../types'
import { authService } from '../services/authService'
import { setStoredCredentials, clearStoredCredentials, getStoredCredentials } from '../services/api'

interface AuthContextType {
  user: UserDto | null
  isLoading: boolean
  login: (credentials: LoginCredentials) => Promise<void>
  logout: () => void
  updateUser: (user: UserDto) => void
  isAuthenticated: boolean
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserDto | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    // Попытка загрузить пользователя из сохраненных credentials
    const loadUser = async () => {
      try {
        const stored = getStoredCredentials()
        if (stored) {
          const currentUser = await authService.getCurrentUser()
          if (currentUser) {
            setUser(currentUser)
          } else {
            // Credentials недействительны
            clearStoredCredentials()
          }
        }
      } catch (error) {
        console.error('Failed to load user:', error)
        clearStoredCredentials()
      } finally {
        setIsLoading(false)
      }
    }

    loadUser()
  }, [])

  const login = async (credentials: LoginCredentials) => {
    try {
      // Сохраняем credentials для последующих запросов
      setStoredCredentials(credentials.username, credentials.password)
      
      // Получаем информацию о пользователе
      const currentUser = await authService.getCurrentUser()
      if (!currentUser) {
        throw new Error('Не удалось получить информацию о пользователе')
      }
      
      setUser(currentUser)
    } catch (error) {
      clearStoredCredentials()
      throw error
    }
  }

  const logout = () => {
    clearStoredCredentials()
    setUser(null)
  }

  const updateUser = (updatedUser: UserDto) => {
    setUser(updatedUser)
    // Обновляем credentials если изменилось имя пользователя
    const stored = getStoredCredentials()
    if (stored && stored.username !== updatedUser.username) {
      // Имя пользователя изменилось, но пароль мы не знаем, поэтому просто обновляем user в state
      // Пользователю нужно будет перелогиниться если пароль изменился
    }
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        isLoading,
        login,
        logout,
        updateUser,
        isAuthenticated: !!user,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

