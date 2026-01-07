import React, { useState, useRef, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useNotification } from '../common/NotificationContext'
import { userService } from '../../services/userService'
import { extractErrorMessage } from '../../utils/errorHandler'
import { applyTheme, getTheme } from '../../utils/theme'

interface UserSettingsProps {
  onClose: () => void
}

const UserSettings: React.FC<UserSettingsProps> = ({ onClose }) => {
  const { user, updateUser, logout } = useAuth()
  const { showSuccess, showError } = useNotification()
  const navigate = useNavigate()
  const [isOpen, setIsOpen] = useState(false)
  const [theme, setTheme] = useState<'dark' | 'light'>('dark')
  const [username, setUsername] = useState(user?.username || '')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const dropdownRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    // Открываем модальное окно при монтировании
    setIsOpen(true)
    
    // Загружаем тему из localStorage
    const savedTheme = getTheme()
    setTheme(savedTheme)
  }, [])

  useEffect(() => {
    // Обновляем username при изменении user
    if (user) {
      setUsername(user.username || '')
    }
  }, [user])

  useEffect(() => {
    // Обработчик клика вне dropdown
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false)
        onClose()
      }
    }

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside)
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [isOpen, onClose])

  const handleThemeChange = (newTheme: 'dark' | 'light') => {
    setTheme(newTheme)
    applyTheme(newTheme)
    showSuccess('Тема изменена')
  }

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!user) return

    // Валидация
    if (!username.trim()) {
      showError('Имя пользователя не может быть пустым')
      return
    }

    if (newPassword && newPassword.length < 6) {
      showError('Пароль должен содержать минимум 6 символов')
      return
    }

    if (newPassword && newPassword !== confirmPassword) {
      showError('Пароли не совпадают')
      return
    }

    setIsLoading(true)
    try {
      const updateData: any = {
        id: user.id,
        username: username.trim(),
        role: user.role,
      }
      
      // Отправляем пароль только если он указан
      if (newPassword && newPassword.trim()) {
        updateData.passwordHash = newPassword.trim()
      }
      
      await userService.update(user.id, updateData)
      
      // Обновляем пользователя в контексте
      updateUser({
        ...user,
        username: username.trim(),
      })
      
      showSuccess('Профиль успешно обновлен')
      setNewPassword('')
      setConfirmPassword('')
    } catch (error) {
      showError(extractErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  const handleLogout = () => {
    logout()
    onClose()
    navigate('/login')
  }

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50" onClick={onClose}>
      <div 
        className="bg-dark-surface rounded-lg p-6 border w-full max-w-md shadow-xl"
        style={{ borderColor: 'var(--color-border)' }}
        onClick={(e) => e.stopPropagation()}
        ref={dropdownRef}
      >
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-2xl font-bold text-dark-text">Настройки</h2>
          <button
            onClick={onClose}
            className="text-dark-text2 hover:text-dark-text text-2xl leading-none"
            aria-label="Закрыть"
          >
            ×
          </button>
        </div>

        <form onSubmit={handleUpdateProfile} className="space-y-6">
          {/* Тема */}
          <div>
            <label className="block text-sm font-medium text-dark-text2 mb-2">
              Тема
            </label>
            <div className="flex gap-4">
              <button
                type="button"
                onClick={() => handleThemeChange('dark')}
                className={`flex-1 px-4 py-2 rounded-lg border transition-colors ${
                  theme === 'dark'
                    ? 'bg-dark-primary border-dark-primary text-white'
                    : 'bg-dark-surface2 text-dark-text'
                }`}
                style={theme !== 'dark' ? { borderColor: 'var(--color-border)' } : {}}
              >
                Темная
              </button>
              <button
                type="button"
                onClick={() => handleThemeChange('light')}
                className={`flex-1 px-4 py-2 rounded-lg border transition-colors ${
                  theme === 'light'
                    ? 'bg-dark-primary border-dark-primary text-white'
                    : 'bg-dark-surface2 text-dark-text'
                }`}
                style={theme !== 'light' ? { borderColor: 'var(--color-border)' } : {}}
              >
                Светлая
              </button>
            </div>
          </div>

          {/* Имя пользователя */}
          <div>
            <label htmlFor="username" className="block text-sm font-medium text-dark-text2 mb-2">
              Имя пользователя
            </label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full px-4 py-2 bg-dark-surface2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text"
              style={{ borderColor: 'var(--color-border)' }}
              required
            />
          </div>

          {/* Новый пароль */}
          <div>
            <label htmlFor="newPassword" className="block text-sm font-medium text-dark-text2 mb-2">
              Новый пароль (оставьте пустым, если не хотите менять)
            </label>
            <input
              id="newPassword"
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              className="w-full px-4 py-2 bg-dark-surface2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text"
              style={{ borderColor: 'var(--color-border)' }}
            />
          </div>

          {/* Подтверждение пароля */}
          {newPassword && (
            <div>
              <label htmlFor="confirmPassword" className="block text-sm font-medium text-dark-text2 mb-2">
                Подтвердите новый пароль
              </label>
              <input
                id="confirmPassword"
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="w-full px-4 py-2 bg-dark-surface2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text"
                style={{ borderColor: 'var(--color-border)' }}
              />
            </div>
          )}

          {/* Кнопки */}
          <div className="flex gap-4 pt-4">
            <button
              type="submit"
              disabled={isLoading}
              className="flex-1 px-4 py-2 bg-dark-primary hover:bg-dark-primaryHover text-white font-semibold rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-dark-primary disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoading ? 'Сохранение...' : 'Сохранить'}
            </button>
            <button
              type="button"
              onClick={handleLogout}
              className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white font-semibold rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-red-500"
            >
              Выйти
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default UserSettings

