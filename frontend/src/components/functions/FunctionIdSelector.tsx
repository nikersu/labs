import React, { useState, useEffect, useRef } from 'react'
import { useAuth } from '../../context/AuthContext'
import { functionService } from '../../services/functionService'
import { FunctionDto } from '../../types'

interface FunctionIdSelectorProps {
  onSelect: (id: number) => void
  disabled?: boolean
  placeholder?: string
}

const FunctionIdSelector: React.FC<FunctionIdSelectorProps> = ({
  onSelect,
  disabled = false,
  placeholder = 'ID функции',
}) => {
  const { user } = useAuth()
  const [inputValue, setInputValue] = useState('')
  const [showDropdown, setShowDropdown] = useState(false)
  const [userFunctions, setUserFunctions] = useState<FunctionDto[]>([])
  const [loading, setLoading] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  const loadUserFunctions = async () => {
    if (userFunctions.length > 0 || !user) return
    
    setLoading(true)
    try {
      const functions = await functionService.getByUserId(user.id)
      setUserFunctions(functions)
    } catch (err) {
      console.error('Error loading user functions:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setShowDropdown(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const handleToggleDropdown = () => {
    if (!showDropdown) {
      loadUserFunctions()
    }
    setShowDropdown(!showDropdown)
  }

  const handleSelectFunction = (func: FunctionDto) => {
    if (func.id) {
      setInputValue(func.id.toString())
      setShowDropdown(false)
      onSelect(func.id)
    }
  }

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setInputValue(e.target.value)
  }

  const handleLoadClick = () => {
    const id = parseInt(inputValue)
    if (!isNaN(id) && id > 0) {
      onSelect(id)
      setInputValue('')
    }
  }

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleLoadClick()
    }
  }

  return (
    <div ref={containerRef} className="relative">
      <div className="flex gap-2">
        <div className="relative flex" style={{ width: '120px' }}>
          <input
            type="text"
            inputMode="numeric"
            pattern="[0-9]*"
            value={inputValue}
            onChange={handleInputChange}
            onKeyPress={handleKeyPress}
            placeholder={placeholder}
            disabled={disabled}
            className="w-full px-2 py-2 bg-dark-surface2 border border-gray-600 rounded-l-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text text-sm"
          />
          <button
            type="button"
            onClick={handleToggleDropdown}
            disabled={disabled}
            className="px-2 py-2 bg-dark-surface2 border border-l-0 border-gray-600 rounded-r-lg text-dark-text2 hover:text-dark-text transition-colors disabled:opacity-50 text-xs"
            title="Показать список функций"
          >
            {showDropdown ? '▲' : '▼'}
          </button>
        </div>
        <button
          onClick={handleLoadClick}
          disabled={!inputValue || disabled}
          className="px-3 py-2 bg-dark-surface2 hover:bg-dark-surface border border-gray-600 rounded-lg text-dark-text transition-colors disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap text-sm"
        >
          Загрузить
        </button>
      </div>

      {showDropdown && (
        <div className="absolute top-full left-0 w-full mt-1 bg-dark-surface border border-gray-600 rounded-lg shadow-xl z-[100] max-h-64 overflow-y-auto">
          {loading ? (
            <div className="p-4 text-center text-dark-text2">Загрузка...</div>
          ) : userFunctions.length === 0 ? (
            <div className="p-4 text-center text-dark-text2">У вас нет созданных функций</div>
          ) : (
            userFunctions.map((func, index) => (
              <div
                key={func.id || index}
                onClick={() => handleSelectFunction(func)}
                className="p-3 cursor-pointer border-b border-gray-700 last:border-b-0 hover:bg-dark-surface2 transition-colors"
              >
                <div className="flex justify-between items-center">
                  <div>
                    <div className="font-semibold text-dark-text">
                      {func.name}
                      {func.id && (
                        <span className="text-sm font-normal text-dark-text2 ml-2">(ID: {func.id})</span>
                      )}
                    </div>
                    <div className="text-xs text-dark-text2 mt-1">
                      Точек: {func.count || func.points?.length || func.xValues?.length || 0} | 
                      Диапазон: [
                      {func.xValues?.[0]?.toFixed(1) || func.points?.[0]?.xValue.toFixed(1) || '?'}, 
                      {func.xValues?.[func.xValues.length - 1]?.toFixed(1) || 
                       func.points?.[func.points.length - 1]?.xValue.toFixed(1) || '?'}
                      ]
                    </div>
                  </div>
                  <div className="ml-4 px-2 py-1 bg-dark-primary text-white rounded text-xs font-bold">
                    #{func.id}
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  )
}

export default FunctionIdSelector




