import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { authService } from '../../services/authService'
import { useNotification } from '../common/NotificationContext'
import { validateUsername, validatePassword } from '../../utils/validation'
import { extractErrorMessage } from '../../utils/errorHandler'

const RegisterForm: React.FC = () => {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [errors, setErrors] = useState<{
    username?: string
    password?: string
    confirmPassword?: string
  }>({})
  const [isLoading, setIsLoading] = useState(false)

  const navigate = useNavigate()
  const { showSuccess, showError } = useNotification()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    // Валидация
    const usernameError = validateUsername(username)
    const passwordError = validatePassword(password)
    let confirmPasswordError: string | null = null

    if (password !== confirmPassword) {
      confirmPasswordError = 'Пароли не совпадают'
    }

    if (usernameError || passwordError || confirmPasswordError) {
      setErrors({
        username: usernameError || undefined,
        password: passwordError || undefined,
        confirmPassword: confirmPasswordError || undefined,
      })
      return
    }

    setIsLoading(true)
    setErrors({})

    try {
      await authService.register({ username, password })
      showSuccess('Регистрация успешна! Теперь вы можете войти.')
      navigate('/login')
    } catch (error) {
      const message = extractErrorMessage(error)
      showError(message)
      if (message.includes('уже существует')) {
        setErrors({ username: message })
      }
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-dark-bg px-4">
      <div className="w-full max-w-md">
        <div className="bg-dark-surface rounded-lg shadow-xl p-8 fade-in">
          <h1 className="text-3xl font-bold text-center mb-8 text-dark-text">
            Регистрация
          </h1>

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label htmlFor="reg-username" className="block text-sm font-medium text-dark-text2 mb-2">
                Имя пользователя
              </label>
              <input
                id="reg-username"
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
                aria-describedby={errors.username ? 'reg-username-error' : undefined}
                autoComplete="username"
                required
              />
              {errors.username && (
                <p id="reg-username-error" className="mt-1 text-sm text-red-500" role="alert">
                  {errors.username}
                </p>
              )}
            </div>

            <div>
              <label htmlFor="reg-password" className="block text-sm font-medium text-dark-text2 mb-2">
                Пароль
              </label>
              <input
                id="reg-password"
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
                aria-describedby={errors.password ? 'reg-password-error' : undefined}
                autoComplete="new-password"
                required
              />
              {errors.password && (
                <p id="reg-password-error" className="mt-1 text-sm text-red-500" role="alert">
                  {errors.password}
                </p>
              )}
            </div>

            <div>
              <label htmlFor="confirm-password" className="block text-sm font-medium text-dark-text2 mb-2">
                Подтвердите пароль
              </label>
              <input
                id="confirm-password"
                type="password"
                value={confirmPassword}
                onChange={(e) => {
                  setConfirmPassword(e.target.value)
                  if (errors.confirmPassword) setErrors({ ...errors, confirmPassword: undefined })
                }}
                className={`w-full px-4 py-2 bg-dark-surface2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text ${
                  errors.confirmPassword ? 'border-red-500' : 'border-gray-600'
                }`}
                placeholder="Подтвердите пароль"
                aria-invalid={!!errors.confirmPassword}
                aria-describedby={errors.confirmPassword ? 'confirm-password-error' : undefined}
                autoComplete="new-password"
                required
              />
              {errors.confirmPassword && (
                <p id="confirm-password-error" className="mt-1 text-sm text-red-500" role="alert">
                  {errors.confirmPassword}
                </p>
              )}
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full bg-dark-primary hover:bg-dark-primaryHover text-white font-semibold py-2 px-4 rounded-lg transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-dark-primary focus:ring-offset-2 focus:ring-offset-dark-surface disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoading ? 'Регистрация...' : 'Зарегистрироваться'}
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-dark-text2">
              Уже есть аккаунт?{' '}
              <Link
                to="/login"
                className="text-dark-primary hover:text-dark-primaryHover font-medium focus:outline-none focus:ring-2 focus:ring-dark-primary rounded"
              >
                Войти
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}

export default RegisterForm





