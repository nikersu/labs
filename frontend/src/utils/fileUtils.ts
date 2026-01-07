import { FunctionDto } from '../types'
import { functionService } from '../services/functionService'

/**
 * Сохранить функцию в JSON файл (скачать)
 */
export const saveFunctionToFile = (func: FunctionDto) => {
  const data = {
    name: func.name,
    expression: func.expression, // Сохраняем выражение, чтобы знать тип функции
    xValues: func.xValues || func.points?.map(p => p.xValue) || [],
    yValues: func.yValues || func.points?.map(p => p.yValue) || [],
    count: func.count || func.points?.length || func.xValues?.length || 0,
    exportedAt: new Date().toISOString(),
    // Добавляем информацию о том, является ли функция математической
    isMathFunction: func.expression && /^[A-Za-z]+\[/.test(func.expression),
  }

  const json = JSON.stringify(data, null, 2)
  const blob = new Blob([json], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  
  const link = document.createElement('a')
  link.href = url
  link.download = `${func.name.replace(/[^a-z0-9]/gi, '_')}_${Date.now()}.json`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/**
 * Загрузить функцию из JSON файла
 */
export const loadFunctionFromFile = (file: File): Promise<{
  name: string
  expression?: string
  xValues: number[]
  yValues: number[]
  count: number
  isMathFunction?: boolean
}> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    
    reader.onload = (e) => {
      try {
        const data = JSON.parse(e.target.result as string)
        
        // Валидация
        if (!data.name || !data.xValues || !data.yValues) {
          throw new Error('Некорректный формат файла')
        }
        
        if (data.xValues.length !== data.yValues.length) {
          throw new Error('Массивы X и Y разной длины')
        }
        
        resolve({
          name: data.name,
          expression: data.expression,
          xValues: Array.isArray(data.xValues) ? data.xValues.map(Number) : [],
          yValues: Array.isArray(data.yValues) ? data.yValues.map(Number) : [],
          count: data.count || data.xValues.length,
          isMathFunction: data.isMathFunction || false,
        })
      } catch (error) {
        reject(new Error(`Ошибка чтения файла: ${error instanceof Error ? error.message : 'Unknown error'}`))
      }
    }
    
    reader.onerror = () => {
      reject(new Error('Ошибка чтения файла'))
    }
    
    reader.readAsText(file)
  })
}

/**
 * Создать функцию из загруженных данных
 */
export const createFunctionFromLoadedData = async (
  data: { name: string; xValues: number[]; yValues: number[] },
  factoryType: string = 'ARRAY',
  userId: number
): Promise<FunctionDto> => {
  try {
    const xValues = Array.isArray(data.xValues) ? data.xValues.map(Number) : []
    const yValues = Array.isArray(data.yValues) ? data.yValues.map(Number) : []
    
    if (xValues.length === 0 || yValues.length === 0) {
      throw new Error('Массивы X и Y не могут быть пустыми')
    }
    
    const created = await functionService.createFromArrays({
      name: data.name,
      xValues,
      yValues,
      factoryType,
      userId,
    })
    
    return created
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unknown error'
    throw new Error(`Ошибка создания функции: ${message}`)
  }
}

