import React, { useState, useEffect } from 'react'
import { useAuth } from '../../context/AuthContext'
import { useNotification } from '../common/NotificationContext'
import { functionService } from '../../services/functionService'
import { extractErrorMessage } from '../../utils/errorHandler'

interface CompositeModalProps {
  onClose: () => void
  onSuccess?: () => void
}

const CompositeModal: React.FC<CompositeModalProps> = ({ onClose, onSuccess }) => {
  const { user } = useAuth()
  const { showError, showSuccess } = useNotification()
  const [name, setName] = useState('')
  const [innerFunction, setInnerFunction] = useState('')
  const [outerFunction, setOuterFunction] = useState('')
  const [availableFunctions, setAvailableFunctions] = useState<string[]>([])
  const [loading, setLoading] = useState(false)
  const [xFrom, setXFrom] = useState('0')
  const [xTo, setXTo] = useState('10')
  const [count, setCount] = useState('11')
  const [factoryType, setFactoryType] = useState('ARRAY')
  const [createdFunction, setCreatedFunction] = useState<any>(null)

  // Маппинг английских названий на русские
  const mathFunctionNames: Record<string, string> = {
    'SqrFunction': 'Квадратичная функция (x²)',
    'IdentityFunction': 'Тождественная функция (x)',
    'UnitFunction': 'Единичная функция (1)',
    'ZeroFunction': 'Нулевая функция (0)',
    'ConstantFunction': 'Константная функция',
    'SinFunction': 'Синус (sin x)',
    'CosFunction': 'Косинус (cos x)',
    'LnFunction': 'Натуральный логарифм (ln x)',
    'ExpFunction': 'Экспонента (e^x)',
  }

  useEffect(() => {
    loadAvailableFunctions()
  }, [])

  const loadAvailableFunctions = async () => {
    try {
      const functions = await functionService.getAvailableMathFunctions()
      setAvailableFunctions(functions)
      if (functions.length > 0) {
        setInnerFunction(functions[0])
        setOuterFunction(functions[0])
      }
    } catch (error) {
      showError(extractErrorMessage(error))
    }
  }

  const handleCreate = async () => {
    if (!name.trim() || !user) {
      showError('Введите название функции')
      return
    }
    if (!innerFunction || !outerFunction) {
      showError('Выберите обе функции')
      return
    }

    const xFromNum = parseFloat(xFrom)
    const xToNum = parseFloat(xTo)
    const countNum = parseInt(count)

    if (isNaN(xFromNum) || isNaN(xToNum) || isNaN(countNum)) {
      showError('Введите корректные числа для интервала')
      return
    }

    if (xFromNum >= xToNum) {
      showError('Начало интервала должно быть меньше конца')
      return
    }

    if (countNum < 2 || countNum > 1000) {
      showError('Количество точек должно быть от 2 до 1000')
      return
    }

    setLoading(true)
    try {
      // Создаём компонентную функцию (метаданные) - она будет добавлена в список доступных функций
      await functionService.createCompositeFunction(name, innerFunction, outerFunction)
      
      // Теперь создаём табулированную функцию из компонентной
      // Используем имя компонентной функции как mathFunctionType
      const created = await functionService.createFromMath({
        name,
        mathFunctionType: name, // имя компонентной функции
        xFrom: xFromNum,
        xTo: xToNum,
        count: countNum,
        factoryType,
      })

      setCreatedFunction(created)
      await loadAvailableFunctions() // Обновляем список доступных функций
      
      // Отправляем событие для обновления списка функций в других компонентах
      window.dispatchEvent(new CustomEvent('compositeFunctionCreated'))
      
      showSuccess('Составная функция создана')
    } catch (error) {
      showError(extractErrorMessage(error))
    } finally {
      setLoading(false)
    }
  }

  const handleDone = () => {
    if (onSuccess) onSuccess()
    onClose()
  }

  const resetForm = () => {
    setName('')
    setCreatedFunction(null)
    setXFrom('0')
    setXTo('10')
    setCount('11')
  }

  return (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50"
      onClick={onClose}
    >
      <div 
        className="bg-dark-surface rounded-lg p-6 border w-full max-w-2xl max-h-[90vh] overflow-y-auto"
        style={{ borderColor: 'var(--color-border)' }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-2xl font-bold text-dark-text">Создание составной функции</h2>
          <button
            onClick={onClose}
            className="text-dark-text2 hover:text-dark-text text-2xl leading-none"
          >
            ×
          </button>
        </div>

        {!createdFunction ? (
          <div className="space-y-4">
            <p className="text-dark-text2">
              Составная функция (композиция) f ∘ g означает f(g(x)).
              <br />
              Функция будет сохранена в вашем списке.
            </p>

            <div>
              <label className="block text-sm font-medium text-dark-text2 mb-2">
                Название функции
              </label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Например: Квадрат синуса"
                disabled={loading}
                className="w-full px-4 py-2 bg-dark-surface2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text"
                style={{ borderColor: 'var(--color-border)' }}
              />
            </div>

            <div className="grid grid-cols-3 gap-4 items-center">
              <div>
                <label className="block text-sm font-medium text-dark-text2 mb-2">
                  Внешняя функция f
                </label>
                <select
                  value={outerFunction}
                  onChange={(e) => setOuterFunction(e.target.value)}
                  disabled={loading}
                  className="w-full px-4 py-2 bg-dark-surface2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text"
                  style={{ borderColor: 'var(--color-border)' }}
                >
                  {availableFunctions.map((func) => (
                    <option key={func} value={func}>
                      {mathFunctionNames[func] || func}
                    </option>
                  ))}
                </select>
              </div>

              <div className="text-center text-3xl font-bold text-dark-primary">
                ∘
              </div>

              <div>
                <label className="block text-sm font-medium text-dark-text2 mb-2">
                  Внутренняя функция g
                </label>
                <select
                  value={innerFunction}
                  onChange={(e) => setInnerFunction(e.target.value)}
                  disabled={loading}
                  className="w-full px-4 py-2 bg-dark-surface2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text"
                  style={{ borderColor: 'var(--color-border)' }}
                >
                  {availableFunctions.map((func) => (
                    <option key={func} value={func}>
                      {mathFunctionNames[func] || func}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {innerFunction && outerFunction && (
              <div className="bg-dark-surface2 rounded-lg p-4 text-center">
                <div className="text-sm text-dark-text2 mb-1">Результат:</div>
                <div className="text-lg font-bold text-dark-text">
                  {name || '?'}(x) = {mathFunctionNames[outerFunction] || outerFunction}({mathFunctionNames[innerFunction] || innerFunction}(x))
                </div>
              </div>
            )}

            {/* Параметры табуляции */}
            <div className="bg-dark-surface2 rounded-lg p-4">
              <h4 className="text-dark-text font-semibold mb-3">📊 Параметры табуляции</h4>
              
              <div className="grid grid-cols-3 gap-4 mb-3">
                <div>
                  <label className="block text-xs text-dark-text2 mb-1">X от:</label>
                  <input
                    type="number"
                    step="any"
                    value={xFrom}
                    onChange={(e) => setXFrom(e.target.value)}
                    disabled={loading}
                    className="w-full px-3 py-2 bg-dark-surface border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text"
                    style={{ borderColor: 'var(--color-border)' }}
                  />
                </div>
                <div>
                  <label className="block text-xs text-dark-text2 mb-1">X до:</label>
                  <input
                    type="number"
                    step="any"
                    value={xTo}
                    onChange={(e) => setXTo(e.target.value)}
                    disabled={loading}
                    className="w-full px-3 py-2 bg-dark-surface border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text"
                    style={{ borderColor: 'var(--color-border)' }}
                  />
                </div>
                <div>
                  <label className="block text-xs text-dark-text2 mb-1">Точек:</label>
                  <input
                    type="number"
                    min="2"
                    max="1000"
                    value={count}
                    onChange={(e) => setCount(e.target.value)}
                    disabled={loading}
                    className="w-full px-3 py-2 bg-dark-surface border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text"
                    style={{ borderColor: 'var(--color-border)' }}
                  />
                </div>
              </div>
              
              <div className="mb-3">
                <label className="block text-xs text-dark-text2 mb-1">Тип фабрики:</label>
                <select
                  value={factoryType}
                  onChange={(e) => setFactoryType(e.target.value)}
                  className="w-full px-3 py-2 bg-dark-surface border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text"
                  style={{ borderColor: 'var(--color-border)' }}
                >
                  <option value="ARRAY">Array</option>
                  <option value="LINKED_LIST">Linked List</option>
                </select>
              </div>
            </div>

            <button
              onClick={handleCreate}
              disabled={loading || !name.trim()}
              className="w-full px-4 py-2 bg-dark-primary hover:bg-dark-primaryHover text-white font-semibold rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Создание...' : '🔗 Создать и сохранить функцию'}
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="bg-dark-surface2 rounded-lg p-4 border-2 border-green-500">
              <h3 className="text-green-400 font-semibold mb-2">✅ Функция создана!</h3>
              <div className="font-semibold text-dark-text mb-2">{createdFunction.name}</div>
              <div className="text-sm text-dark-text2">
                <div>ID: {createdFunction.id}</div>
                <div>Точек: {createdFunction.count || createdFunction.points?.length || createdFunction.xValues?.length || 0}</div>
                {createdFunction.xValues && createdFunction.xValues.length > 0 && (
                  <div>
                    Диапазон: [{createdFunction.xValues[0].toFixed(2)}, {createdFunction.xValues[createdFunction.xValues.length - 1].toFixed(2)}]
                  </div>
                )}
              </div>
            </div>

            <p className="text-sm text-dark-text2">
              💡 Функция сохранена в вашем списке и доступна для использования.
              Также она добавлена в список математических функций для создания новых композиций.
            </p>

            <div className="flex gap-2">
              <button
                onClick={resetForm}
                className="flex-1 px-4 py-2 bg-dark-surface2 hover:bg-dark-surface text-dark-text rounded-lg transition-colors"
              >
                Создать ещё
              </button>
              <button
                onClick={handleDone}
                className="flex-1 px-4 py-2 bg-dark-primary hover:bg-dark-primaryHover text-white rounded-lg transition-colors"
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
      </div>
    </div>
  )
}

export default CompositeModal


