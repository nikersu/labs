import React, { useState, useEffect } from 'react'
import { useAuth } from '../../context/AuthContext'
import { useNotification } from '../common/NotificationContext'
import { loadFunctionFromFile, createFunctionFromLoadedData } from '../../utils/fileUtils'
import { extractErrorMessage } from '../../utils/errorHandler'

interface LoadFunctionModalProps {
  onClose: () => void
  onSuccess: () => void
  file: File
}

const LoadFunctionModal: React.FC<LoadFunctionModalProps> = ({ onClose, onSuccess, file }) => {
  const { user } = useAuth()
  const { showError, showSuccess } = useNotification()
  const [functionName, setFunctionName] = useState('')
  const [factoryType, setFactoryType] = useState('ARRAY')
  const [loading, setLoading] = useState(false)
  const [fileData, setFileData] = useState<{
    name: string
    expression?: string
    xValues: number[]
    yValues: number[]
    count: number
    isMathFunction?: boolean
  } | null>(null)

  useEffect(() => {
    const loadFile = async () => {
      try {
        const data = await loadFunctionFromFile(file)
        setFileData(data)
        setFunctionName(data.name) // Устанавливаем название из файла по умолчанию
      } catch (error) {
        showError(extractErrorMessage(error))
        onClose()
      }
    }
    loadFile()
  }, [file, showError, onClose])

  const handleLoad = async () => {
    if (!functionName.trim() || !user || !fileData) {
      showError('Введите название функции')
      return
    }

    setLoading(true)
    try {
      await createFunctionFromLoadedData(
        {
          name: functionName.trim(),
          xValues: fileData.xValues,
          yValues: fileData.yValues,
        },
        factoryType,
        user.id
      )
      showSuccess('Функция успешно загружена')
      onSuccess()
      onClose()
    } catch (error) {
      showError(extractErrorMessage(error))
    } finally {
      setLoading(false)
    }
  }

  if (!fileData) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
        <div className="bg-dark-surface rounded-lg p-6 border" style={{ borderColor: 'var(--color-border)' }}>
          <div className="text-dark-text">Загрузка файла...</div>
        </div>
      </div>
    )
  }

  return (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50"
      onClick={onClose}
    >
      <div 
        className="bg-dark-surface rounded-lg p-6 border w-full max-w-md"
        style={{ borderColor: 'var(--color-border)' }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-2xl font-bold text-dark-text">Загрузить функцию</h2>
          <button
            onClick={onClose}
            className="text-dark-text2 hover:text-dark-text text-2xl leading-none"
          >
            ×
          </button>
        </div>

        <div className="space-y-4">
          {/* Информация о файле */}
          <div className="bg-dark-surface2 rounded-lg p-4">
            <div className="text-sm text-dark-text2 mb-2">Информация о файле:</div>
            <div className="text-dark-text">
              <div><strong>Имя файла:</strong> {file.name}</div>
              <div><strong>Точек:</strong> {fileData.count}</div>
              {fileData.isMathFunction && (
                <div className="text-xs text-dark-text2 mt-1">
                  ⚠️ Это математическая функция, сохраненная как набор точек
                </div>
              )}
              {fileData.expression && (
                <div className="text-xs text-dark-text2 mt-1">
                  <strong>Выражение:</strong> <code className="bg-dark-surface px-2 py-1 rounded">{fileData.expression}</code>
                </div>
              )}
            </div>
          </div>

          {/* Название функции */}
          <div>
            <label className="block text-sm font-medium text-dark-text2 mb-2">
              Название функции *
            </label>
            <input
              type="text"
              value={functionName}
              onChange={(e) => setFunctionName(e.target.value)}
              placeholder="Введите название функции"
              disabled={loading}
              className="w-full px-4 py-2 bg-dark-surface2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text"
              style={{ borderColor: 'var(--color-border)' }}
              autoFocus
            />
          </div>

          {/* Тип фабрики */}
          <div>
            <label className="block text-sm font-medium text-dark-text2 mb-2">
              Тип фабрики
            </label>
            <select
              value={factoryType}
              onChange={(e) => setFactoryType(e.target.value)}
              disabled={loading}
              className="w-full px-4 py-2 bg-dark-surface2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text"
              style={{ borderColor: 'var(--color-border)' }}
            >
              <option value="ARRAY">Array</option>
              <option value="LINKED_LIST">Linked List</option>
            </select>
          </div>

          {/* Кнопки */}
          <div className="flex gap-2 pt-4">
            <button
              onClick={handleLoad}
              disabled={loading || !functionName.trim()}
              className="flex-1 px-4 py-2 bg-dark-primary hover:bg-dark-primaryHover text-white font-semibold rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Загрузка...' : 'Загрузить'}
            </button>
            <button
              onClick={onClose}
              disabled={loading}
              className="px-4 py-2 bg-dark-surface2 hover:bg-dark-surface text-dark-text rounded-lg transition-colors disabled:opacity-50"
            >
              Отмена
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default LoadFunctionModal





