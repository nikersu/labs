import React, { useState, useMemo, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useNotification } from '../common/NotificationContext'
import { functionService } from '../../services/functionService'
import { FunctionDto, FunctionType, FunctionFormData } from '../../types'
import { validateFunctionName, validatePoints } from '../../utils/validation'
import { extractErrorMessage } from '../../utils/errorHandler'
import FunctionGraph from './FunctionGraph'
import PointTable from '../points/PointTable'

interface FunctionFormProps {
  onSuccess?: (func: FunctionDto) => void
  onCancel?: () => void
}

const FunctionForm: React.FC<FunctionFormProps> = ({ onSuccess, onCancel }) => {
  const { user } = useAuth()
  const { showSuccess, showError } = useNotification()
  const navigate = useNavigate()

  const [formData, setFormData] = useState<FunctionFormData>({
    name: '',
    type: 'math',
    expression: '',
    points: [],
    mathFromX: undefined,
    mathToX: undefined,
    mathPointCount: undefined,
  })
  const [errors, setErrors] = useState<{ [key: string]: string }>({})
  const [isLoading, setIsLoading] = useState(false)
  const [availableMathFunctions, setAvailableMathFunctions] = useState<string[]>([])
  const [selectedMathFunction, setSelectedMathFunction] = useState<string>('')
  const [factoryType, setFactoryType] = useState<string>('ARRAY')

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

  // Загружаем список доступных математических функций
  useEffect(() => {
    const loadMathFunctions = async () => {
      try {
        const functions = await functionService.getAvailableMathFunctions()
        setAvailableMathFunctions(functions)
        if (functions.length > 0) {
          setSelectedMathFunction(functions[0])
        }
      } catch (error) {
        console.error('Ошибка загрузки математических функций:', error)
      }
    }
    loadMathFunctions()
  }, [])

  const handleTypeChange = (type: FunctionType) => {
    setFormData({
      ...formData,
      type: type === 'linkedlist' ? 'array' : type, // linkedlist теперь тоже array, но с выбором фабрики
      expression: type === 'math' ? formData.expression : '',
      points: type !== 'math' ? (formData.points || []) : [],
      mathFromX: type === 'math' ? (formData.mathFromX || 0) : undefined,
      mathToX: type === 'math' ? (formData.mathToX || 10) : undefined,
      mathPointCount: type === 'math' ? (formData.mathPointCount || 50) : undefined,
    })
    setErrors({})
  }

  const handleAddPoint = () => {
    const newPoints = [...(formData.points || []), { x: 0, y: 0 }]
    setFormData({ ...formData, points: newPoints })
  }

  const handlePointChange = (index: number, field: 'x' | 'y', value: number) => {
    const newPoints = [...(formData.points || [])]
    newPoints[index] = { ...newPoints[index], [field]: value }
    // Сортируем по X
    newPoints.sort((a, b) => a.x - b.x)
    setFormData({ ...formData, points: newPoints })
  }

  const handleRemovePoint = (index: number) => {
    const newPoints = formData.points?.filter((_, i) => i !== index) || []
    setFormData({ ...formData, points: newPoints })
  }

  // Предпросмотр для математических функций - загружаем функцию с бэкенда для предпросмотра
  const [previewFunctionData, setPreviewFunctionData] = useState<FunctionDto | null>(null)

  const validateForm = (): boolean => {
    const newErrors: { [key: string]: string } = {}

    const nameError = validateFunctionName(formData.name)
    if (nameError) newErrors.name = nameError

    if (formData.type === 'math') {
      if (!selectedMathFunction) {
        newErrors.mathFunction = 'Выберите математическую функцию'
      }

      if (formData.mathFromX === undefined || formData.mathFromX === null) {
        newErrors.mathFromX = 'Введите начало интервала'
      }

      if (formData.mathToX === undefined || formData.mathToX === null) {
        newErrors.mathToX = 'Введите конец интервала'
      }

      if (formData.mathFromX !== undefined && formData.mathToX !== undefined && formData.mathFromX >= formData.mathToX) {
        newErrors.mathToX = 'Конец интервала должен быть больше начала'
      }

      if (formData.mathPointCount === undefined || formData.mathPointCount === null) {
        newErrors.mathPointCount = 'Введите количество точек'
      } else if (formData.mathPointCount < 2) {
        newErrors.mathPointCount = 'Количество точек должно быть не менее 2'
      } else if (formData.mathPointCount > 1000) {
        newErrors.mathPointCount = 'Количество точек не должно превышать 1000'
      }
    } else {
      const pointsError = validatePoints(formData.points || [])
      if (pointsError) newErrors.points = pointsError
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!validateForm() || !user) {
      return
    }

    setIsLoading(true)

    try {
      if (formData.type === 'math') {
        // Создание из математической функции
        const request: any = {
          name: formData.name,
          mathFunctionType: selectedMathFunction,
          xFrom: formData.mathFromX!,
          xTo: formData.mathToX!,
          count: formData.mathPointCount!,
          factoryType: factoryType,
        }
        
        const created = await functionService.createFromMath(request)
          showSuccess('Функция успешно создана')
          if (onSuccess) {
            onSuccess(created)
          } else {
            navigate('/functions')
          }
      } else {
        // Создание из массивов
        const points = formData.points || []
        if (points.length === 0) {
          showError('Добавьте хотя бы одну точку')
          setIsLoading(false)
          return
        }

        const xValues = points.map(p => p.x)
        const yValues = points.map(p => p.y)

        const created = await functionService.createFromArrays({
          name: formData.name,
          xValues,
          yValues,
          factoryType: factoryType,
        })
          showSuccess('Функция успешно создана')
          if (onSuccess) {
            onSuccess(created)
          } else {
            navigate('/functions')
          }
      }
    } catch (error) {
      showError(extractErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  // Функция для предпросмотра - для математических функций загружаем с бэкенда (без сохранения)
  const handlePreview = async () => {
    if (formData.type === 'math') {
      if (!selectedMathFunction || formData.mathFromX === undefined || formData.mathToX === undefined || formData.mathPointCount === undefined) {
        showError('Заполните все поля для предпросмотра')
        return
      }
      try {
        setIsLoading(true)
        const request: any = {
          name: 'preview',
          mathFunctionType: selectedMathFunction,
          xFrom: formData.mathFromX,
          xTo: formData.mathToX,
          count: formData.mathPointCount,
          factoryType: factoryType,
        }
        
        // Используем preview endpoint, который не сохраняет функцию в БД
        const preview = await functionService.previewFromMath(request)
        setPreviewFunctionData(preview)
      } catch (error) {
        showError(extractErrorMessage(error))
      } finally {
        setIsLoading(false)
      }
    }
  }

  const previewFunction: FunctionDto | null = useMemo(() => {
    if (formData.type === 'math') {
      return previewFunctionData
    } else {
      const points = formData.points || []
      if (points.length === 0) return null
      return {
        id: 0,
        name: formData.name || 'Предпросмотр',
        expression: formData.type,
        userId: user?.id || 0,
        points: points.map((p) => ({
          functionId: 0,
          xValue: p.x,
          yValue: p.y,
        })),
      }
    }
  }, [formData, previewFunctionData, user])

  const hasPreviewData = previewFunction !== null && (previewFunction.points?.length || 0) > 0

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <h1 className="text-3xl font-bold text-dark-text">Создать функцию</h1>

      <form 
        onSubmit={handleSubmit} 
        className="bg-dark-surface rounded-lg p-6 border border-gray-700 space-y-6"
        onClick={(e) => {
          // Предотвращаем submit формы при клике на график
          const target = e.target as HTMLElement
          if (target.closest('.recharts-wrapper') || target.closest('[data-graph-container]')) {
            e.preventDefault()
            e.stopPropagation()
          }
        }}
      >
        {/* Название */}
        <div>
          <label htmlFor="name" className="block text-sm font-medium text-dark-text2 mb-2">
            Название функции *
          </label>
          <input
            id="name"
            type="text"
            value={formData.name}
            onChange={(e) => {
              setFormData({ ...formData, name: e.target.value })
              if (errors.name) setErrors({ ...errors, name: '' })
            }}
            className={`w-full px-4 py-2 bg-dark-surface2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text ${
              errors.name ? 'border-red-500' : 'border-gray-600'
            }`}
            placeholder="Введите название функции"
            required
          />
          {errors.name && <p className="mt-1 text-sm text-red-500">{errors.name}</p>}
        </div>

        {/* Тип функции */}
        <div>
          <label className="block text-sm font-medium text-dark-text2 mb-2">
            Тип функции *
          </label>
          <div className="flex flex-wrap gap-4">
            <label className="flex items-center">
              <input
                type="radio"
                name="type"
                value="math"
                checked={formData.type === 'math'}
                onChange={() => handleTypeChange('math')}
                className="mr-2"
              />
              <span className="text-dark-text">Математическое выражение</span>
            </label>
            <label className="flex items-center">
              <input
                type="radio"
                name="type"
                value="array"
                checked={formData.type === 'array'}
                onChange={() => handleTypeChange('array')}
                className="mr-2"
              />
              <span className="text-dark-text">Табулированная функция (из точек)</span>
            </label>
          </div>
        </div>

        {/* Тип фабрики - показываем для всех типов функций */}
        <div>
          <label htmlFor="factoryType" className="block text-sm font-medium text-dark-text2 mb-2">
            Тип фабрики (способ хранения точек)
          </label>
          <select
            id="factoryType"
            value={factoryType}
            onChange={(e) => setFactoryType(e.target.value)}
            className="w-full px-4 py-2 bg-dark-surface2 border border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text"
          >
            <option value="ARRAY">Array Tabulated Function</option>
            <option value="LINKED_LIST">Linked List Tabulated Function</option>
          </select>
          <p className="mt-1 text-xs text-dark-text2">
            Определяет структуру данных для хранения точек функции
          </p>
        </div>

        {/* Математическое выражение с параметрами */}
        {formData.type === 'math' && (
          <>
            <div>
              <label htmlFor="mathFunction" className="block text-sm font-medium text-dark-text2 mb-2">
                Математическая функция *
              </label>
              <select
                id="mathFunction"
                value={selectedMathFunction}
                onChange={(e) => {
                  setSelectedMathFunction(e.target.value)
                  if (errors.mathFunction) setErrors({ ...errors, mathFunction: '' })
                }}
                className={`w-full px-4 py-2 bg-dark-surface2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text ${
                  errors.mathFunction ? 'border-red-500' : 'border-gray-600'
                }`}
                required
              >
                {availableMathFunctions.map((func) => (
                  <option key={func} value={func}>
                    {mathFunctionNames[func] || func}
                  </option>
                ))}
              </select>
              {errors.mathFunction && <p className="mt-1 text-sm text-red-500">{errors.mathFunction}</p>}
            </div>


            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label htmlFor="mathFromX" className="block text-sm font-medium text-dark-text2 mb-2">
                  Начало интервала (X от) *
                </label>
                <input
                  id="mathFromX"
                  type="number"
                  step="any"
                  value={formData.mathFromX ?? ''}
                  onChange={(e) => {
                    const value = e.target.value === '' ? undefined : parseFloat(e.target.value)
                    setFormData({ ...formData, mathFromX: value })
                    if (errors.mathFromX) setErrors({ ...errors, mathFromX: '' })
                  }}
                  placeholder="Введите начало интервала"
                  className={`w-full px-4 py-2 bg-dark-surface2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text placeholder:text-dark-text2 ${
                    errors.mathFromX ? 'border-red-500' : 'border-gray-600'
                  }`}
                  required
                />
                {errors.mathFromX && <p className="mt-1 text-sm text-red-500">{errors.mathFromX}</p>}
              </div>

              <div>
                <label htmlFor="mathToX" className="block text-sm font-medium text-dark-text2 mb-2">
                  Конец интервала (X до) *
                </label>
                <input
                  id="mathToX"
                  type="number"
                  step="any"
                  value={formData.mathToX ?? ''}
                  onChange={(e) => {
                    const value = e.target.value === '' ? undefined : parseFloat(e.target.value)
                    setFormData({ ...formData, mathToX: value })
                    if (errors.mathToX) setErrors({ ...errors, mathToX: '' })
                  }}
                  placeholder="Введите конец интервала"
                  className={`w-full px-4 py-2 bg-dark-surface2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text placeholder:text-dark-text2 ${
                    errors.mathToX ? 'border-red-500' : 'border-gray-600'
                  }`}
                  required
                />
                {errors.mathToX && <p className="mt-1 text-sm text-red-500">{errors.mathToX}</p>}
              </div>

              <div>
                <label htmlFor="mathPointCount" className="block text-sm font-medium text-dark-text2 mb-2">
                  Количество точек *
                </label>
                <input
                  id="mathPointCount"
                  type="number"
                  min="2"
                  value={formData.mathPointCount ?? ''}
                  onChange={(e) => {
                    const value = e.target.value === '' ? undefined : parseInt(e.target.value)
                    setFormData({ ...formData, mathPointCount: value })
                    if (errors.mathPointCount) setErrors({ ...errors, mathPointCount: '' })
                  }}
                  placeholder="Введите количество точек"
                  className={`w-full px-4 py-2 bg-dark-surface2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text placeholder:text-dark-text2 ${
                    errors.mathPointCount ? 'border-red-500' : 'border-gray-600'
                  }`}
                  required
                />
                {errors.mathPointCount && <p className="mt-1 text-sm text-red-500">{errors.mathPointCount}</p>}
              </div>
            </div>

            {/* Кнопка предпросмотра для математической функции */}
            <div>
              <button
                type="button"
                onClick={handlePreview}
                disabled={
                  isLoading || 
                  !selectedMathFunction || 
                  formData.mathFromX === undefined || 
                  formData.mathToX === undefined || 
                  formData.mathPointCount === undefined
                }
                className="px-4 py-2 bg-dark-surface2 hover:bg-dark-surface text-dark-text font-semibold rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-dark-primary disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Предпросмотр
              </button>
            </div>
          </>
        )}

        {/* Точки для табулированных функций */}
        {formData.type !== 'math' && (
          <div>
            <div className="flex justify-between items-center mb-2">
              <label className="block text-sm font-medium text-dark-text2">
                Точки функции * (минимум 2)
              </label>
              <button
                type="button"
                onClick={handleAddPoint}
                className="px-3 py-1 bg-dark-primary hover:bg-dark-primaryHover text-white text-sm rounded focus:outline-none focus:ring-2 focus:ring-dark-primary"
              >
                Добавить точку
              </button>
            </div>
            {errors.points && <p className="mb-2 text-sm text-red-500">{errors.points}</p>}
            <PointTable
              points={formData.points || []}
              onPointChange={handlePointChange}
              onPointRemove={handleRemovePoint}
            />
          </div>
        )}

        {/* Предпросмотр графика */}
        {hasPreviewData && previewFunction && (
          <div>
            <h3 className="text-lg font-semibold text-dark-text mb-4">Предпросмотр графика</h3>
            <FunctionGraph functionData={previewFunction} height={400} />
          </div>
        )}

        {/* Кнопки */}
        <div className="flex gap-4">
          <button
            type="submit"
            disabled={isLoading}
            className="px-6 py-2 bg-dark-primary hover:bg-dark-primaryHover text-white font-semibold rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-dark-primary disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isLoading ? 'Создание...' : 'Создать функцию'}
          </button>
          <button
            type="button"
            onClick={() => {
              if (onCancel) {
                onCancel()
              } else {
                navigate('/functions')
              }
            }}
            className="px-6 py-2 bg-dark-surface2 hover:bg-dark-surface text-dark-text font-semibold rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-dark-primary"
          >
            Отмена
          </button>
        </div>
      </form>
    </div>
  )
}

export default FunctionForm
