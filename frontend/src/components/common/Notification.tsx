import React from 'react'
import { NotificationType } from './NotificationContext'

interface NotificationProps {
  message: string
  type: NotificationType
  onClose: () => void
}

const Notification: React.FC<NotificationProps> = ({ message, type, onClose }) => {
  const getStyles = () => {
    switch (type) {
      case 'success':
        return 'bg-green-600 border-green-500'
      case 'error':
        return 'bg-red-600 border-red-500'
      case 'warning':
        return 'bg-yellow-600 border-yellow-500'
      default:
        return 'bg-blue-600 border-blue-500'
    }
  }

  const getIcon = () => {
    switch (type) {
      case 'success':
        return '✓'
      case 'error':
        return '✕'
      case 'warning':
        return '⚠'
      default:
        return 'ℹ'
    }
  }

  return (
    <div
      className={`${getStyles()} border-l-4 text-white px-4 py-3 rounded shadow-lg fade-in flex items-center justify-between min-w-[300px] max-w-full`}
      role="alert"
      aria-live="polite"
    >
      <div className="flex items-center gap-2">
        <span className="font-bold text-lg">{getIcon()}</span>
        <p className="text-sm font-medium">{message}</p>
      </div>
      <button
        onClick={onClose}
        className="ml-4 text-white hover:text-gray-200 focus:outline-none focus:ring-2 focus:ring-white rounded"
        aria-label="Закрыть уведомление"
      >
        <span className="text-xl">&times;</span>
      </button>
    </div>
  )
}

export default Notification





