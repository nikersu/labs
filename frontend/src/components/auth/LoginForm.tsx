import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useNotification } from '../common/NotificationContext'
import { validateUsername, validatePassword } from '../../utils/validation'
import { extractErrorMessage } from '../../utils/errorHandler'

const LoginForm: React.FC = () => {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<{ username?: string; password?: string }>({})
  const [isLoading, setIsLoading] = useState(false)

  const { login } = useAuth()
  const navigate = useNavigate()
  const { showError } = useNotification()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    // Валидация
    const usernameError = validateUsername(username)
    const passwordError = validatePassword(password)

    if (usernameError || passwordError) {
      setErrors({
        username: usernameError || undefined,
        password: passwordError || undefined,
      })
      return
    }

    setIsLoading(true)
    setErrors({})

    try {
      await login({ username, password })
      navigate('/functions')
    } catch (error) {
      const message = extractErrorMessage(error)
      showError(message)
      setErrors({ password: message })
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-dark-bg px-4">
      <div className="w-full max-w-md">
        <div className="bg-dark-surface rounded-lg shadow-xl p-8 fade-in">
          <h1 className="text-3xl font-bold text-center mb-8 text-dark-text">
            Вход в систему
          </h1>
          
          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label htmlFor="username" className="block text-sm font-medium text-dark-text2 mb-2">
                Имя пользователя
              </label>
              <input
                id="username"
                type="text"
                value={username}
                onChange={(e) => {
                  setUsername(e.target.value)
                  if (errors.username) setErrors({ ...errors, username: undefined })
                }}
                className={`w-full px-4 py-2 bg-dark-surface2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text ${
                  errors.username ? 'border-red-500' : 'border-gray-600'
                }`}
                placeholder="Введите имя пользователя"
                aria-invalid={!!errors.username}
                aria-describedby={errors.username ? 'username-error' : undefined}
                autoComplete="username"
                required
              />
              {errors.username && (
                <p id="username-error" className="mt-1 text-sm text-red-500" role="alert">
                  {errors.username}
                </p>
              )}
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-dark-text2 mb-2">
                Пароль
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value)
                  if (errors.password) setErrors({ ...errors, password: undefined })
                }}
                className={`w-full px-4 py-2 bg-dark-surface2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text ${
                  errors.password ? 'border-red-500' : 'border-gray-600'
                }`}
                placeholder="Введите пароль"
                aria-invalid={!!errors.password}
                aria-describedby={errors.password ? 'password-error' : undefined}
                autoComplete="current-password"
                required
              />
              {errors.password && (
                <p id="password-error" className="mt-1 text-sm text-red-500" role="alert">
                  {errors.password}
                </p>
              )}
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full bg-dark-primary hover:bg-dark-primaryHover text-white font-semibold py-2 px-4 rounded-lg transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-dark-primary focus:ring-offset-2 focus:ring-offset-dark-surface disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoading ? 'Вход...' : 'Войти'}
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-dark-text2">
              Нет аккаунта?{' '}
              <Link
                to="/register"
                className="text-dark-primary hover:text-dark-primaryHover font-medium focus:outline-none focus:ring-2 focus:ring-dark-primary rounded"
              >
                Зарегистрироваться
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}

export default LoginForm





