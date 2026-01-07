import React, { useState } from 'react'
import { Outlet, Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import UserSettings from '../user/UserSettings'
import FunctionOperationsModal from '../functions/FunctionOperationsModal'
import CompositeModal from '../functions/CompositeModal'

const Layout: React.FC = () => {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [showSettings, setShowSettings] = useState(false)
  const [showOperationsModal, setShowOperationsModal] = useState(false)
  const [showCompositeModal, setShowCompositeModal] = useState(false)

  return (
    <div className="min-h-screen bg-dark-bg">
      <nav className="bg-dark-surface border-b" style={{ borderColor: 'var(--color-border)' }}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <Link
                to="/functions"
                className="text-xl font-bold text-dark-text hover:text-dark-primary transition-colors"
              >
                Мои функции
              </Link>
              <div className="ml-10 flex space-x-4">
                <button
                  onClick={() => setShowOperationsModal(true)}
                  className="text-dark-text2 hover:text-dark-text px-3 py-2 rounded-md text-sm font-medium transition-colors"
                >
                  Операции над функциями
                </button>
                <button
                  onClick={() => setShowCompositeModal(true)}
                  className="text-dark-text2 hover:text-dark-text px-3 py-2 rounded-md text-sm font-medium transition-colors"
                >
                  Составные функции
                </button>
              </div>
            </div>
            <div className="flex items-center space-x-4">
              <button
                onClick={() => setShowSettings(true)}
                className="text-dark-text2 hover:text-dark-text text-sm font-medium transition-colors cursor-pointer focus:outline-none focus:ring-2 focus:ring-dark-primary rounded px-2 py-1"
              >
                {user?.username}
              </button>
            </div>
          </div>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>
      
      {showSettings && (
        <UserSettings onClose={() => setShowSettings(false)} />
      )}
      
      {showOperationsModal && (
        <FunctionOperationsModal 
          onClose={() => setShowOperationsModal(false)}
          onSuccess={() => {
            setShowOperationsModal(false)
            // Можно обновить список функций если нужно
          }}
        />
      )}
      
      {showCompositeModal && (
        <CompositeModal 
          onClose={() => setShowCompositeModal(false)}
          onSuccess={() => {
            setShowCompositeModal(false)
            // Можно обновить список функций если нужно
          }}
        />
      )}
    </div>
  )
}

export default Layout

