import React, { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useNotification } from '../common/NotificationContext'
import { functionService } from '../../services/functionService'
import { FunctionDto } from '../../types'
import { validateFunctionName } from '../../utils/validation'
import { extractErrorMessage } from '../../utils/errorHandler'
import FunctionGraph from './FunctionGraph'
import EditableTable from './EditableTable'

const FunctionEditForm: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const { showSuccess, showError } = useNotification()
  const navigate = useNavigate()

  const [functionData, setFunctionData] = useState<FunctionDto | null>(null)
  const [name, setName] = useState('')
  const [functionType, setFunctionType] = useState<'math' | 'tabulated'>('tabulated')
  const [factoryType, setFactoryType] = useState<string>('ARRAY')
  const [selectedMathFunction, setSelectedMathFunction] = useState<string>('')
  const [mathFromX, setMathFromX] = useState<number>(0)
  const [mathToX, setMathToX] = useState<number>(10)
  const [mathPointCount, setMathPointCount] = useState<number>(11)
  const [availableMathFunctions, setAvailableMathFunctions] = useState<string[]>([])
  const [errors, setErrors] = useState<{ [key: string]: string }>({})
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)

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
    const loadData = async () => {
      // Сначала загружаем список доступных функций
      await loadAvailableMathFunctions()
      // Затем загружаем саму функцию (чтобы правильно установить selectedMathFunction)
      if (id) {
        await loadFunction()
      }
    }
    loadData()
  }, [id])

  // Слушаем событие создания новой компонентной функции для обновления списка
  useEffect(() => {
    const handleCompositeFunctionCreated = () => {
      loadAvailableMathFunctions()
    }
    window.addEventListener('compositeFunctionCreated', handleCompositeFunctionCreated)

    // Обновляем список при фокусе на окне
    const handleFocus = () => {
      loadAvailableMathFunctions()
    }
    window.addEventListener('focus', handleFocus)

    return () => {
      window.removeEventListener('compositeFunctionCreated', handleCompositeFunctionCreated)
      window.removeEventListener('focus', handleFocus)
    }
  }, [])

  const loadAvailableMathFunctions = async () => {
    try {
      const functions = await functionService.getAvailableMathFunctions()
      setAvailableMathFunctions(functions)
      // Не устанавливаем значение по умолчанию здесь, так как loadFunction установит правильное значение
      // Значение по умолчанию нужно только при создании новой функции
    } catch (error) {
      console.error('Ошибка загрузки математических функций:', error)
    }
  }

  const loadFunction = async () => {
    if (!id) return

    setIsLoading(true)
    try {
      // Сначала убедимся, что список доступных функций загружен
      if (availableMathFunctions.length === 0) {
        await loadAvailableMathFunctions()
      }
      
      const func = await functionService.getById(parseInt(id))
      setFunctionData(func)
      setName(func.name)

      // Определяем тип функции
      const isTabulated = func.expression === 'ArrayTabulatedFunction' || 
                         func.expression === 'LinkedListTabulatedFunction' ||
                         func.expression === 'ARRAY' ||
                         func.expression === 'LINKED_LIST'
      
      setFunctionType(isTabulated ? 'tabulated' : 'math')

      // Определяем тип фабрики из expression
      if (func.expression === 'LinkedListTabulatedFunction' || func.expression === 'LINKED_LIST') {
        setFactoryType('LINKED_LIST')
      } else {
        setFactoryType('ARRAY')
      }

      // Если это математическая функция, пытаемся извлечь параметры из expression
      if (!isTabulated && func.expression) {
        // Формат: FunctionType[xFrom,xTo,count] или компонентная функция с " ∘ "
        if (func.expression.includes(" ∘ ")) {
          // Это компонентная функция - имя функции = имя компонентной функции
          const funcName = func.name
          // Получаем актуальный список функций на случай, если он еще не загружен
          const currentAvailableFunctions = availableMathFunctions.length > 0 
            ? availableMathFunctions 
            : await functionService.getAvailableMathFunctions()
          
          if (currentAvailableFunctions.includes(funcName)) {
            setSelectedMathFunction(funcName)
          }
          // Для компонентных функций параметры могут быть в другом формате или отсутствовать
          // Пытаемся извлечь из expression, если есть формат [xFrom,xTo,count]
          const match = func.expression.match(/\[([\d.-]+),([\d.-]+),(\d+)\]/)
          if (match) {
            setMathFromX(parseFloat(match[1]))
            setMathToX(parseFloat(match[2]))
            setMathPointCount(parseInt(match[3]))
          }
        } else {
          // Обычная математическая функция
          // Улучшенное регулярное выражение для извлечения типа функции и параметров
          // Формат: FunctionType[xFrom,xTo,count] или FunctionType [xFrom,xTo,count]
          const match = func.expression.match(/^(.+?)\s*\[\s*([\d.-]+)\s*,\s*([\d.-]+)\s*,\s*(\d+)\s*\]$/)
          if (match) {
            setMathFromX(parseFloat(match[2]))
            setMathToX(parseFloat(match[3]))
            setMathPointCount(parseInt(match[4]))
            // Пытаемся определить тип математической функции
            const funcType = match[1].trim()
            
            // Получаем актуальный список функций на случай, если он еще не загружен
            const currentAvailableFunctions = availableMathFunctions.length > 0 
              ? availableMathFunctions 
              : await functionService.getAvailableMathFunctions()
            
            if (currentAvailableFunctions.includes(funcType)) {
              setSelectedMathFunction(funcType)
            } else {
              // Если функция не найдена в списке, устанавливаем первую доступную как fallback
              // но логируем предупреждение
              console.warn(`Функция ${funcType} не найдена в списке доступных функций. Expression: ${func.expression}`)
              if (currentAvailableFunctions.length > 0) {
                setSelectedMathFunction(currentAvailableFunctions[0])
              }
            }
          } else {
            // Если не удалось распарсить expression, логируем для отладки
            console.warn(`Не удалось распарсить expression: ${func.expression}`)
          }
        }
      }

      // Для табулированных функций просто устанавливаем functionData
      // EditableTable сам загрузит точки из functionData
    } catch (error) {
      showError(extractErrorMessage(error))
      navigate('/functions')
    } finally {
      setIsLoading(false)
    }
  }

  const validateForm = (): boolean => {
    const newErrors: { [key: string]: string } = {}

    const nameError = validateFunctionName(name)
    if (nameError) newErrors.name = nameError

    if (functionType === 'math') {
      // Валидация для математических функций
      if (!selectedMathFunction) {
        newErrors.mathFunction = 'Выберите математическую функцию'
      }
      if (mathFromX >= mathToX) {
        newErrors.mathToX = 'Конец интервала должен быть больше начала'
      }
      if (mathPointCount < 2 || mathPointCount > 1000) {
        newErrors.mathPointCount = 'Количество точек должно быть от 2 до 1000'
      }
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleFunctionUpdate = async (updatedFunction: FunctionDto) => {
    // Обновляем локальное состояние после изменений в EditableTable
    setFunctionData(updatedFunction)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!validateForm() || !functionData?.id) {
      return
    }

    setIsSaving(true)

    try {
      // Если тип функции изменился, нужно пересоздать функцию
      const wasTabulated = functionData.expression === 'ArrayTabulatedFunction' || 
                          functionData.expression === 'LinkedListTabulatedFunction' ||
                          functionData.expression === 'ARRAY' ||
                          functionData.expression === 'LINKED_LIST'
      const isNowTabulated = functionType === 'tabulated'

      if (wasTabulated !== isNowTabulated) {
        // Тип изменился - нужно удалить старую функцию и создать новую
        await functionService.delete(functionData.id)

        if (isNowTabulated) {
          // Создаем табулированную функцию из массивов
          // Используем существующие точки из математической функции, если они есть
          const existingXValues = functionData.xValues || []
          const existingYValues = functionData.yValues || []
          
          // Используем существующие точки, если их достаточно (минимум 2), иначе создаем минимальные
          const xValues = existingXValues.length >= 2 ? existingXValues : [0, 1]
          const yValues = existingYValues.length >= 2 ? existingYValues : [0, 0]
          
          // ВАЖНО: Сохраняем результат создания новой функции
          const newFunction = await functionService.createFromArrays({
            name,
            xValues,
            yValues,
            factoryType,
          })
          
          // КРИТИЧНО: Обновляем functionData с новой функцией (включая новый ID)
          setFunctionData(newFunction)
          
          // Обновляем URL с новым ID
          navigate(`/functions/${newFunction.id}/edit`, { replace: true })
          
          showSuccess('Функция успешно преобразована в табулированную')
        } else {
          // Создаем математическую функцию
          const newFunction = await functionService.createFromMath({
            name,
            mathFunctionType: selectedMathFunction,
            xFrom: mathFromX,
            xTo: mathToX,
            count: mathPointCount,
            factoryType,
          })
          
          showSuccess('Функция успешно преобразована в математическую')
          navigate(`/functions/${newFunction.id}/edit`, { replace: true })
        }
      } else {
        // Тип не изменился - просто обновляем имя и expression
        if (isNowTabulated) {
          // Обновляем табулированную функцию - только имя и expression
          // Точки редактируются через EditableTable
          const newExpression = factoryType === 'LINKED_LIST' ? 'LINKED_LIST' : 'ARRAY'
          const updatedFunction: FunctionDto = {
            ...functionData,
            name,
            expression: newExpression,
            userId: functionData.userId,
          }
          await functionService.update(functionData.id, updatedFunction)
          // Обновляем локальное состояние
          const updated = await functionService.getById(functionData.id)
          setFunctionData(updated)
          showSuccess('Название функции обновлено')
        } else {
          // Обновляем математическую функцию
          // Проверяем, изменились ли параметры (кроме имени)
          const oldExpression = functionData.expression || ''
          const oldMatch = oldExpression.match(/(.+?)\[([\d.-]+),([\d.-]+),(\d+)\]/)
          const paramsChanged = !oldMatch || 
            parseFloat(oldMatch[2]) !== mathFromX ||
            parseFloat(oldMatch[3]) !== mathToX ||
            parseInt(oldMatch[4]) !== mathPointCount ||
            oldMatch[1] !== selectedMathFunction
          
          if (!paramsChanged && name === functionData.name) {
            // Ничего не изменилось
            showSuccess('Изменений не обнаружено')
            return
          }
          
          if (!paramsChanged) {
            // Изменилось только имя - просто обновляем имя и expression
            const newExpression = `${selectedMathFunction}[${mathFromX},${mathToX},${mathPointCount}]`
            const updatedFunction: FunctionDto = {
              ...functionData,
              name,
              expression: newExpression,
              userId: functionData.userId,
            }
            await functionService.update(functionData.id, updatedFunction)
            const updated = await functionService.getById(functionData.id)
            setFunctionData(updated)
            showSuccess('Название функции обновлено')
          } else {
            // Параметры изменились - обновляем существующую функцию
            // ВАЖНО: Используем оригинальное имя для поиска функции в createFromMath
            // чтобы backend нашел существующую функцию и обновил её, а не создал новую
            const originalName = functionData.name
            
            // Пересоздаем точки через createFromMath (он обновит существующую функцию по оригинальному имени)
            const updated = await functionService.createFromMath({
              name: originalName, // Используем оригинальное имя для поиска существующей функции
              mathFunctionType: selectedMathFunction,
              xFrom: mathFromX,
              xTo: mathToX,
              count: mathPointCount,
              factoryType,
            })
            
            // Если имя изменилось, обновляем его отдельно
            if (name !== originalName) {
              const updatedFunction: FunctionDto = {
                ...updated,
                name,
                userId: updated.userId,
              }
              await functionService.update(updated.id, updatedFunction)
              // Загружаем обновленную функцию
              const finalUpdated = await functionService.getById(updated.id)
              setFunctionData(finalUpdated)
            } else {
              setFunctionData(updated)
            }
            
            showSuccess('Функция успешно обновлена')
          }
        }
      }

      // Для табулированных функций не перенаправляем, так как точки редактируются через EditableTable
      if (!isNowTabulated) {
        navigate('/functions')
      }
    } catch (error) {
      showError(extractErrorMessage(error))
    } finally {
      setIsSaving(false)
    }
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-dark-text2">Загрузка функции...</div>
      </div>
    )
  }

  if (!functionData) {
    return (
      <div className="text-center">
        <p className="text-dark-text2">Функция не найдена</p>
      </div>
    )
  }


  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <h1 className="text-3xl font-bold text-dark-text">Редактировать функцию</h1>

      <form onSubmit={handleSubmit} className="bg-dark-surface rounded-lg p-6 border border-gray-700 space-y-6">
        {/* Название */}
        <div>
          <label htmlFor="edit-name" className="block text-sm font-medium text-dark-text2 mb-2">
            Название функции *
          </label>
          <input
            id="edit-name"
            type="text"
            value={name}
            onChange={(e) => {
              setName(e.target.value)
              if (errors.name) setErrors({ ...errors, name: '' })
            }}
            className={`w-full px-4 py-2 bg-dark-surface2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text ${
              errors.name ? 'border-red-500' : 'border-gray-600'
            }`}
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
                name="functionType"
                value="tabulated"
                checked={functionType === 'tabulated'}
                onChange={() => setFunctionType('tabulated')}
                className="mr-2"
              />
              <span className="text-dark-text">Табулированная функция (из точек)</span>
            </label>
            <label className="flex items-center">
              <input
                type="radio"
                name="functionType"
                value="math"
                checked={functionType === 'math'}
                onChange={() => setFunctionType('math')}
                className="mr-2"
              />
              <span className="text-dark-text">Математическое выражение</span>
            </label>
          </div>
        </div>

        {/* Тип фабрики */}
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
        </div>

        {/* Параметры для математических функций */}
        {functionType === 'math' && (
          <div className="bg-dark-surface2 rounded-lg p-4 space-y-4">
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
                className={`w-full px-4 py-2 bg-dark-surface border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text ${
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

            <div className="grid grid-cols-3 gap-4">
              <div>
                <label htmlFor="mathFromX" className="block text-sm font-medium text-dark-text2 mb-2">
                  X от *
                </label>
                <input
                  id="mathFromX"
                  type="number"
                  step="any"
                  value={mathFromX}
                  onChange={(e) => {
                    setMathFromX(parseFloat(e.target.value) || 0)
                    if (errors.mathFromX) setErrors({ ...errors, mathFromX: '' })
                  }}
                  className={`w-full px-4 py-2 bg-dark-surface border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text ${
                    errors.mathFromX ? 'border-red-500' : 'border-gray-600'
                  }`}
                  required
                />
                {errors.mathFromX && <p className="mt-1 text-sm text-red-500">{errors.mathFromX}</p>}
              </div>

              <div>
                <label htmlFor="mathToX" className="block text-sm font-medium text-dark-text2 mb-2">
                  X до *
                </label>
                <input
                  id="mathToX"
                  type="number"
                  step="any"
                  value={mathToX}
                  onChange={(e) => {
                    setMathToX(parseFloat(e.target.value) || 0)
                    if (errors.mathToX) setErrors({ ...errors, mathToX: '' })
                  }}
                  className={`w-full px-4 py-2 bg-dark-surface border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text ${
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
                  max="1000"
                  value={mathPointCount}
                  onChange={(e) => {
                    setMathPointCount(parseInt(e.target.value) || 2)
                    if (errors.mathPointCount) setErrors({ ...errors, mathPointCount: '' })
                  }}
                  className={`w-full px-4 py-2 bg-dark-surface border rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text ${
                    errors.mathPointCount ? 'border-red-500' : 'border-gray-600'
                  }`}
                  required
                />
                {errors.mathPointCount && <p className="mt-1 text-sm text-red-500">{errors.mathPointCount}</p>}
              </div>
            </div>
          </div>
        )}

        {/* Точки для табулированных функций */}
        {functionType === 'tabulated' && functionData && (
          <div>
            <label className="block text-sm font-medium text-dark-text2 mb-2">
              Точки функции
            </label>
            <EditableTable
              func={functionData}
              editable={true}
              isInsertable={true}
              isRemovable={true}
              onUpdate={handleFunctionUpdate}
            />
          </div>
        )}

        {/* Предпросмотр */}
        {functionType === 'tabulated' && functionData && (
          <div>
            <h3 className="text-lg font-semibold text-dark-text mb-4">Предпросмотр графика</h3>
            <FunctionGraph functionData={functionData} height={300} />
          </div>
        )}

        {/* Кнопки */}
        <div className="flex gap-4">
          {functionType === 'tabulated' ? (
            // Для табулированных функций сохраняем только имя (точки сохраняются через EditableTable)
            <>
              <button
                type="submit"
                disabled={isSaving}
                className="px-6 py-2 bg-dark-primary hover:bg-dark-primaryHover text-white font-semibold rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-dark-primary disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isSaving ? 'Сохранение...' : 'Сохранить название'}
              </button>
              <button
                type="button"
                onClick={() => navigate('/functions')}
                className="px-6 py-2 bg-dark-surface2 hover:bg-dark-surface text-dark-text font-semibold rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-dark-primary"
              >
                Закрыть
              </button>
            </>
          ) : (
            // Для математических функций сохраняем все
            <>
              <button
                type="submit"
                disabled={isSaving}
                className="px-6 py-2 bg-dark-primary hover:bg-dark-primaryHover text-white font-semibold rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-dark-primary disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isSaving ? 'Сохранение...' : 'Сохранить изменения'}
              </button>
              <button
                type="button"
                onClick={() => navigate('/functions')}
                className="px-6 py-2 bg-dark-surface2 hover:bg-dark-surface text-dark-text font-semibold rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-dark-primary"
              >
                Отмена
              </button>
            </>
          )}
        </div>
      </form>
    </div>
  )
}

export default FunctionEditForm

