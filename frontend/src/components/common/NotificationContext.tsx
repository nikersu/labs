import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react'
import Notification from './Notification'

export type NotificationType = 'success' | 'error' | 'info' | 'warning'

export interface NotificationData {
  id: string
  message: string
  type: NotificationType
}

interface NotificationContextType {
  showNotification: (message: string, type?: NotificationType) => void
  showSuccess: (message: string) => void
  showError: (message: string) => void
  showInfo: (message: string) => void
  showWarning: (message: string) => void
}

const NotificationContext = createContext<NotificationContextType | undefined>(undefined)

export function useNotification() {
  const context = useContext(NotificationContext)
  if (!context) {
    throw new Error('useNotification must be used within NotificationProvider')
  }
  return context
}

export function NotificationProvider({ children }: { children: ReactNode }) {
  const [notifications, setNotifications] = useState<NotificationData[]>([])

  const removeNotification = useCallback((id: string) => {
    setNotifications((prev) => prev.filter((n) => n.id !== id))
  }, [])

  const showNotification = useCallback((message: string, type: NotificationType = 'info') => {
    const id = Date.now().toString() + Math.random().toString(36).substr(2, 9)
    setNotifications((prev) => [...prev, { id, message, type }])

    // Автоматическое удаление через 5 секунд
    setTimeout(() => {
      removeNotification(id)
    }, 5000)
  }, [removeNotification])

  const showSuccess = useCallback((message: string) => showNotification(message, 'success'), [showNotification])
  const showError = useCallback((message: string) => showNotification(message, 'error'), [showNotification])
  const showInfo = useCallback((message: string) => showNotification(message, 'info'), [showNotification])
  const showWarning = useCallback((message: string) => showNotification(message, 'warning'), [showNotification])

  return (
    <NotificationContext.Provider value={{ showNotification, showSuccess, showError, showInfo, showWarning }}>
      {children}
      <div className="fixed top-4 right-4 z-50 flex flex-col gap-2 max-w-md w-full">
        {notifications.map((notification) => (
          <Notification
            key={notification.id}
            message={notification.message}
            type={notification.type}
            onClose={() => removeNotification(notification.id)}
          />
        ))}
      </div>
    </NotificationContext.Provider>
  )
}





