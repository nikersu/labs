import React, { useState, useRef } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useNotification } from '../common/NotificationContext'
import { functionService } from '../../services/functionService'
import { FunctionDto } from '../../types'
import { extractErrorMessage } from '../../utils/errorHandler'
import { saveFunctionToFile, loadFunctionFromFile, createFunctionFromLoadedData } from '../../utils/fileUtils'
import EditableTable from './EditableTable'
import FunctionIdSelector from './FunctionIdSelector'

interface FunctionOperationsModalProps {
  onClose: () => void
  onSuccess?: () => void
}

const FunctionOperationsModal: React.FC<FunctionOperationsModalProps> = ({ onClose, onSuccess }) => {
  const { user } = useAuth()
  const { showError, showSuccess } = useNotification()
  const [function1, setFunction1] = useState<FunctionDto | null>(null)
  const [function2, setFunction2] = useState<FunctionDto | null>(null)
  const [result, setResult] = useState<FunctionDto | null>(null)
  const [resultName, setResultName] = useState('')
  const [loading, setLoading] = useState(false)
  const [factoryType, setFactoryType] = useState('ARRAY')
  const [showCreateModal1, setShowCreateModal1] = useState(false)
  const [showCreateModal2, setShowCreateModal2] = useState(false)
  
  const fileInput1Ref = useRef<HTMLInputElement>(null)
  const fileInput2Ref = useRef<HTMLInputElement>(null)

  const loadFunctionById = async (id: number) => {
    try {
      const func = await functionService.getById(id)
      return func
    } catch (error) {
      showError(extractErrorMessage(error))
      return null
    }
  }

  const handleFileLoad = async (file: File | null, setter: (func: FunctionDto) => void) => {
    if (!file || !user) return
    
    setLoading(true)
    try {
      const data = await loadFunctionFromFile(file)
      const created = await createFunctionFromLoadedData(data, factoryType, user.id)
      setter(created)
      showSuccess('Функция загружена из файла')
    } catch (error) {
      showError(extractErrorMessage(error))
    } finally {
      setLoading(false)
    }
  }

  const performOperation = async (operation: 'ADD' | 'SUBTRACT' | 'MULTIPLY' | 'DIVIDE') => {
    if (!function1 || !function2 || !user) {
      showError('Загрузите обе функции для выполнения операции')
      return
    }

    if (!resultName.trim()) {
      showError('Введите название результата')
      return
    }

    if (!function1.id || !function2.id) {
      showError('Функции должны иметь ID')
      return
    }

    setLoading(true)
    try {
      const resultFunc = await functionService.performOperation(
        function1.id,
        function2.id,
        operation,
        resultName,
        factoryType
      )
      setResult(resultFunc)
      showSuccess('Операция выполнена успешно')
    } catch (error) {
      showError(extractErrorMessage(error))
    } finally {
      setLoading(false)
    }
  }


  const handleFunctionCreated1 = (func: FunctionDto) => {
    setFunction1(func)
    setShowCreateModal1(false)
  }

  const handleFunctionCreated2 = (func: FunctionDto) => {
    setFunction2(func)
    setShowCreateModal2(false)
  }

  return (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50"
      onClick={onClose}
    >
      <div 
        className="bg-dark-surface rounded-lg p-6 border w-full max-w-6xl max-h-[90vh] overflow-y-auto relative"
        style={{ borderColor: 'var(--color-border)' }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-2xl font-bold text-dark-text">Операции над функциями</h2>
          <button
            onClick={onClose}
            className="text-dark-text2 hover:text-dark-text text-2xl leading-none"
          >
            ×
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
          {/* Функция 1 */}
          <div className="bg-dark-surface2 rounded-lg p-4 border" style={{ borderColor: 'var(--color-border)' }}>
            <h3 className="text-lg font-semibold text-dark-text mb-3">Функция 1</h3>
            <FunctionPanel
              func={function1}
              setFunc={setFunction1}
              onLoadById={async (id) => {
                const func = await loadFunctionById(id)
                if (func) setFunction1(func)
              }}
              onLoadFromFile={(file) => handleFileLoad(file, setFunction1)}
              onCreate={() => setShowCreateModal1(true)}
              fileInputRef={fileInput1Ref}
              loading={loading}
            />
          </div>

          {/* Операции */}
          <div className="bg-dark-surface2 rounded-lg p-4 border" style={{ borderColor: 'var(--color-border)' }}>
            <h3 className="text-lg font-semibold text-dark-text mb-3">Операции</h3>
            <div className="space-y-2 mb-4">
              <button
                onClick={() => performOperation('ADD')}
                disabled={loading || !function1 || !function2}
                className="w-full px-4 py-2 bg-dark-primary hover:bg-dark-primaryHover text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                ➕ Сложение
              </button>
              <button
                onClick={() => performOperation('SUBTRACT')}
                disabled={loading || !function1 || !function2}
                className="w-full px-4 py-2 bg-dark-primary hover:bg-dark-primaryHover text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                ➖ Вычитание
              </button>
              <button
                onClick={() => performOperation('MULTIPLY')}
                disabled={loading || !function1 || !function2}
                className="w-full px-4 py-2 bg-dark-primary hover:bg-dark-primaryHover text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                ✖️ Умножение
              </button>
              <button
                onClick={() => performOperation('DIVIDE')}
                disabled={loading || !function1 || !function2}
                className="w-full px-4 py-2 bg-dark-primary hover:bg-dark-primaryHover text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                ➗ Деление
              </button>
            </div>
            
            <div className="mb-4">
              <label className="block text-sm font-medium text-dark-text2 mb-2">
                Название результата
              </label>
              <input
                type="text"
                value={resultName}
                onChange={(e) => setResultName(e.target.value)}
                placeholder="Например: Сумма функций"
                disabled={loading}
                className="w-full px-4 py-2 bg-dark-surface border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text"
                style={{ borderColor: 'var(--color-border)' }}
              />
            </div>

            <div className="mb-4">
              <label className="block text-sm font-medium text-dark-text2 mb-2">
                Тип фабрики
              </label>
              <select
                value={factoryType}
                onChange={(e) => setFactoryType(e.target.value)}
                className="w-full px-4 py-2 bg-dark-surface border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text"
                style={{ borderColor: 'var(--color-border)' }}
              >
                <option value="ARRAY">Array</option>
                <option value="LINKED_LIST">Linked List</option>
              </select>
            </div>
          </div>

          {/* Функция 2 */}
          <div className="bg-dark-surface2 rounded-lg p-4 border" style={{ borderColor: 'var(--color-border)' }}>
            <h3 className="text-lg font-semibold text-dark-text mb-3">Функция 2</h3>
            <FunctionPanel
              func={function2}
              setFunc={setFunction2}
              onLoadById={async (id) => {
                const func = await loadFunctionById(id)
                if (func) setFunction2(func)
              }}
              onLoadFromFile={(file) => handleFileLoad(file, setFunction2)}
              onCreate={() => setShowCreateModal2(true)}
              fileInputRef={fileInput2Ref}
              loading={loading}
            />
          </div>
        </div>

        {/* Результат */}
        {result && (
          <div className="mt-4 bg-dark-surface2 rounded-lg p-4 border" style={{ borderColor: 'var(--color-border)' }}>
            <h3 className="text-lg font-semibold text-dark-text mb-3">✅ Результат: {result.name}</h3>
            <div className="mb-3 p-3 bg-green-600/20 border border-green-600 rounded-lg">
              <p className="text-green-400 text-sm">
                ✓ Функция сохранена в "Мои функции" (ID: {result.id})
              </p>
            </div>
            <EditableTable func={result} editable={false} />
            <div className="flex gap-2 mt-4">
              <button
                onClick={() => saveFunctionToFile(result)}
                className="px-4 py-2 bg-dark-surface hover:bg-dark-surface2 text-dark-text rounded-lg transition-colors"
              >
                💾 Сохранить в файл
              </button>
              <button
                onClick={() => {
                  if (onSuccess) onSuccess()
                  onClose()
                }}
                className="px-4 py-2 bg-dark-primary hover:bg-dark-primaryHover text-white rounded-lg transition-colors"
              >
                Готово
              </button>
            </div>
          </div>
        )}

        <div className="mt-4 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-dark-surface2 hover:bg-dark-surface text-dark-text rounded-lg transition-colors"
          >
            Закрыть
          </button>
        </div>

        {/* Модалки создания - используем навигацию вместо вложенных модалок */}
        {showCreateModal1 && (
          <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black bg-opacity-50" onClick={() => setShowCreateModal1(false)}>
            <div 
              className="bg-dark-surface rounded-lg p-6 border w-full max-w-2xl max-h-[90vh] overflow-y-auto" 
              style={{ borderColor: 'var(--color-border)' }}
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-xl font-bold text-dark-text">Создать функцию 1</h3>
                <button onClick={() => setShowCreateModal1(false)} className="text-dark-text2 hover:text-dark-text text-2xl">×</button>
              </div>
              <p className="text-dark-text2 mb-4">Перейдите на страницу создания функции, создайте её, затем вернитесь сюда.</p>
              <Link
                to="/functions/new"
                className="px-4 py-2 bg-dark-primary hover:bg-dark-primaryHover text-white rounded-lg"
                onClick={() => setShowCreateModal1(false)}
              >
                Создать функцию
              </Link>
            </div>
          </div>
        )}

        {showCreateModal2 && (
          <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black bg-opacity-50" onClick={() => setShowCreateModal2(false)}>
            <div 
              className="bg-dark-surface rounded-lg p-6 border w-full max-w-2xl max-h-[90vh] overflow-y-auto" 
              style={{ borderColor: 'var(--color-border)' }}
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-xl font-bold text-dark-text">Создать функцию 2</h3>
                <button onClick={() => setShowCreateModal2(false)} className="text-dark-text2 hover:text-dark-text text-2xl">×</button>
              </div>
              <p className="text-dark-text2 mb-4">Перейдите на страницу создания функции, создайте её, затем вернитесь сюда.</p>
              <Link
                to="/functions/new"
                className="px-4 py-2 bg-dark-primary hover:bg-dark-primaryHover text-white rounded-lg"
                onClick={() => setShowCreateModal2(false)}
              >
                Создать функцию
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

interface FunctionPanelProps {
  func: FunctionDto | null
  setFunc: (func: FunctionDto) => void
  onLoadById: (id: number) => void
  onLoadFromFile: (file: File | null) => void
  onCreate: () => void
  fileInputRef: React.RefObject<HTMLInputElement>
  loading: boolean
}

const FunctionPanel: React.FC<FunctionPanelProps> = ({
  func,
  setFunc,
  onLoadById,
  onLoadFromFile,
  onCreate,
  fileInputRef,
  loading,
}) => {
  return (
    <div className="space-y-3">
      <div className="flex gap-2 flex-wrap">
        <button
          onClick={onCreate}
          disabled={loading}
          className="px-3 py-1 bg-dark-primary hover:bg-dark-primaryHover text-white text-sm rounded transition-colors disabled:opacity-50"
        >
          📊 Создать
        </button>
        <button
          onClick={() => fileInputRef.current?.click()}
          disabled={loading}
          className="px-3 py-1 bg-dark-surface hover:bg-dark-surface2 text-dark-text text-sm rounded transition-colors disabled:opacity-50"
        >
          📁 Из файла
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept=".json"
          onChange={(e) => {
            onLoadFromFile(e.target.files?.[0] || null)
            if (e.target) e.target.value = ''
          }}
          className="hidden"
        />
      </div>

      <FunctionIdSelector
        onSelect={onLoadById}
        disabled={loading}
        placeholder="ID функции"
      />

      {func && (
        <div className="mt-3">
          <div className="bg-dark-surface rounded p-2 mb-2 flex justify-between items-center">
            <div>
              <div className="font-semibold text-dark-text">
                {func.name}
                {func.id && (
                  <span className="text-sm font-normal text-dark-text2 ml-2">(ID: {func.id})</span>
                )}
              </div>
              <div className="text-xs text-dark-text2">
                Точек: {func.count || func.points?.length || func.xValues?.length || 0}
              </div>
            </div>
            <button
              onClick={() => saveFunctionToFile(func)}
              className="px-2 py-1 bg-dark-surface2 hover:bg-dark-surface text-dark-text text-sm rounded"
            >
              💾
            </button>
          </div>
          <EditableTable 
            func={func} 
            editable={true} 
            onUpdate={setFunc}
            isInsertable={func.isInsertable ?? true}
            isRemovable={func.isRemovable ?? true}
          />
        </div>
      )}
    </div>
  )
}

export default FunctionOperationsModal

