import React, { useMemo, useState, useRef, useEffect } from 'react'
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts'
import { FunctionDto } from '../../types'

interface FunctionGraphProps {
  functionData: FunctionDto
  height?: number
  showPoints?: boolean
}

const FunctionGraph: React.FC<FunctionGraphProps> = ({
  functionData,
  height = 400,
  showPoints = true,
}) => {
  const [zoom, setZoom] = useState(1)
  const [pan, setPan] = useState({ x: 0, y: 0 })
  const [isDragging, setIsDragging] = useState(false)
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 })
  const containerRef = useRef<HTMLDivElement>(null)

  // Защита от отсутствующих данных
  if (!functionData) {
    return (
      <div
        className="flex items-center justify-center bg-dark-surface rounded-lg border border-gray-700"
        style={{ height }}
      >
        <p className="text-dark-text2">Данные функции не загружены</p>
      </div>
    )
  }

  // Обработка колесика мыши для масштабирования
  useEffect(() => {
    const container = containerRef.current
    if (!container) return

    const handleWheel = (e: WheelEvent) => {
      e.preventDefault()
      e.stopPropagation() // Останавливаем всплытие события до формы
      const delta = e.deltaY > 0 ? 0.9 : 1.1
      setZoom((prev) => Math.max(0.5, Math.min(3, prev * delta)))
    }

    container.addEventListener('wheel', handleWheel, { passive: false })
    return () => container.removeEventListener('wheel', handleWheel)
  }, [])

  // Глобальный обработчик для mouseup, чтобы правильно обрабатывать перетаскивание
  useEffect(() => {
    if (!isDragging) return

    const handleGlobalMouseUp = (e: MouseEvent) => {
      e.preventDefault()
      e.stopPropagation()
      setIsDragging(false)
    }

    document.addEventListener('mouseup', handleGlobalMouseUp, { capture: true })
    return () => document.removeEventListener('mouseup', handleGlobalMouseUp, { capture: true })
  }, [isDragging])

  // Обработка перетаскивания для панорамирования
  const handleMouseDown = (e: React.MouseEvent) => {
    if (e.button !== 0) return // Только левая кнопка мыши
    e.preventDefault() // Предотвращаем выделение текста
    e.stopPropagation() // Останавливаем всплытие события до формы
    setIsDragging(true)
    setDragStart({ x: e.clientX - pan.x, y: e.clientY - pan.y })
  }

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isDragging) return
    e.preventDefault()
    e.stopPropagation() // Останавливаем всплытие события до формы
    setPan({
      x: e.clientX - dragStart.x,
      y: e.clientY - dragStart.y,
    })
  }

  const handleMouseUp = (e?: React.MouseEvent) => {
    if (e) {
      e.preventDefault()
      e.stopPropagation() // Останавливаем всплытие события до формы
    }
    setIsDragging(false)
  }

  // Сброс масштаба и панорамирования
  const handleReset = () => {
    setZoom(1)
    setPan({ x: 0, y: 0 })
  }

  const chartData = useMemo(() => {
    // Используем xValues и yValues если они есть (как в LAB2), иначе points
    let chartData: Array<{ x: number; y: number }> = []
    
    if (functionData.xValues && functionData.yValues && functionData.xValues.length > 0) {
      // Используем массивы xValues и yValues (формат LAB2)
      chartData = functionData.xValues.map((x, index) => ({
        x: x,
        y: functionData.yValues![index],
      }))
    } else if (functionData.points && functionData.points.length > 0) {
      // Используем points (старый формат)
      chartData = functionData.points.map((p) => ({
        x: p.xValue,
        y: p.yValue,
      }))
    }

    if (chartData.length === 0) {
      return []
    }

    // Проверяем на валидность значений
    const validPoints = chartData.filter(p => 
      p.x != null && p.y != null && 
      !isNaN(p.x) && !isNaN(p.y) && 
      isFinite(p.x) && isFinite(p.y)
    )

    if (validPoints.length === 0) {
      return []
    }

    // Сортируем точки по X
    const sortedPoints = [...validPoints].sort((a, b) => a.x - b.x)

    return sortedPoints.map((point) => ({
      x: point.x,
      y: point.y,
      name: `(${point.x.toFixed(2)}, ${point.y.toFixed(2)})`,
    }))
  }, [functionData.points, functionData.xValues, functionData.yValues])

  if (chartData.length === 0) {
    return (
      <div
        className="flex items-center justify-center bg-dark-surface rounded-lg border border-gray-700"
        style={{ height }}
      >
        <p className="text-dark-text2">Нет данных для отображения</p>
      </div>
    )
  }

  // Вычисляем диапазоны для осей
  const xValues = chartData.map((d) => d.x).filter(v => v != null && !isNaN(v) && isFinite(v))
  const yValues = chartData.map((d) => d.y).filter(v => v != null && !isNaN(v) && isFinite(v))
  
  if (xValues.length === 0 || yValues.length === 0) {
    return (
      <div
        className="flex items-center justify-center bg-dark-surface rounded-lg border border-gray-700"
        style={{ height }}
      >
        <p className="text-dark-text2">Нет валидных данных для отображения</p>
      </div>
    )
  }

  const xMin = Math.min(...xValues)
  const xMax = Math.max(...xValues)
  const yMin = Math.min(...yValues)
  const yMax = Math.max(...yValues)

  // Функция для округления до красивого числа
  const niceNumber = (value: number, round: boolean): number => {
    const exponent = Math.floor(Math.log10(Math.abs(value)))
    const fraction = value / Math.pow(10, exponent)
    let niceFraction: number
    
    if (round) {
      if (fraction < 1.5) niceFraction = 1
      else if (fraction < 3) niceFraction = 2
      else if (fraction < 7) niceFraction = 5
      else niceFraction = 10
    } else {
      if (fraction <= 1) niceFraction = 1
      else if (fraction <= 2) niceFraction = 2
      else if (fraction <= 5) niceFraction = 5
      else niceFraction = 10
    }
    
    return niceFraction * Math.pow(10, exponent)
  }

  // Функция для генерации красивых делений
  const generateNiceTicks = (min: number, max: number, maxTicks: number = 5): number[] => {
    const range = niceNumber(max - min, false)
    const tickSpacing = niceNumber(range / (maxTicks - 1), true)
    const niceMin = Math.floor(min / tickSpacing) * tickSpacing
    const niceMax = Math.ceil(max / tickSpacing) * tickSpacing
    
    const ticks: number[] = []
    for (let tick = niceMin; tick <= niceMax + tickSpacing * 0.5; tick += tickSpacing) {
      ticks.push(tick)
    }
    
    return ticks
  }

  // Генерируем красивые деления для осей
  const xTicks = generateNiceTicks(xMin, xMax, 6)
  const yTicks = generateNiceTicks(yMin, yMax, 6)
  
  const xDomainMin = xTicks[0]
  const xDomainMax = xTicks[xTicks.length - 1]
  const yDomainMin = yTicks[0]
  const yDomainMax = yTicks[yTicks.length - 1]

  // Функция для форматирования значений на осях - показываем целые числа или разумное количество знаков
  const formatAxisValue = (value: number): string => {
    // Если число целое или очень близко к целому, показываем как целое
    if (Math.abs(value - Math.round(value)) < 0.0001) {
      return Math.round(value).toString()
    }
    
    // Для десятичных чисел определяем количество знаков
    const absValue = Math.abs(value)
    if (absValue >= 100) {
      return value.toFixed(0)
    } else if (absValue >= 10) {
      return value.toFixed(1)
    } else if (absValue >= 1) {
      return value.toFixed(2)
    } else if (absValue >= 0.1) {
      return value.toFixed(3)
    } else {
      return value.toFixed(4)
    }
  }

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length && payload[0]?.payload) {
      const data = payload[0].payload
      try {
        return (
          <div className="bg-dark-surface2 border border-gray-600 rounded-lg p-3 shadow-lg">
            <p className="text-dark-text font-semibold">{data.name || 'Точка'}</p>
            <p className="text-dark-text2 text-sm">
              X: {typeof data.x === 'number' ? data.x.toFixed(4) : 'N/A'}
            </p>
            <p className="text-dark-text2 text-sm">
              Y: {typeof data.y === 'number' ? data.y.toFixed(4) : 'N/A'}
            </p>
          </div>
        )
      } catch (error) {
        console.error('Ошибка в CustomTooltip:', error)
        return null
      }
    }
    return null
  }

  return (
    <div className="w-full bg-dark-surface rounded-lg p-4 border border-gray-700" data-graph-container>
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-lg font-semibold text-dark-text">
          {functionData.name}
          {functionData.id && (
            <span className="text-sm font-normal text-dark-text2 ml-2">(ID: {functionData.id})</span>
          )}
        </h3>
        <div className="flex gap-2 items-center">
          <div className="flex gap-1">
            <button
              type="button"
              onClick={(e) => {
                e.preventDefault()
                e.stopPropagation()
                setZoom((prev) => Math.max(0.5, prev - 0.1))
              }}
              className="px-2 py-1 bg-dark-surface2 hover:bg-dark-surface text-dark-text rounded text-sm"
              title="Уменьшить"
            >
              −
            </button>
            <span className="px-2 py-1 text-dark-text2 text-sm min-w-[60px] text-center">
              {Math.round(zoom * 100)}%
            </span>
            <button
              type="button"
              onClick={(e) => {
                e.preventDefault()
                e.stopPropagation()
                setZoom((prev) => Math.min(3, prev + 0.1))
              }}
              className="px-2 py-1 bg-dark-surface2 hover:bg-dark-surface text-dark-text rounded text-sm"
              title="Увеличить"
            >
              +
            </button>
          </div>
          <button
            type="button"
            onClick={(e) => {
              e.preventDefault()
              e.stopPropagation()
              handleReset()
            }}
            className="px-2 py-1 bg-dark-surface2 hover:bg-dark-surface text-dark-text rounded text-sm"
            title="Сбросить масштаб и позицию"
          >
            ⟲
          </button>
        </div>
      </div>
      <div
        ref={containerRef}
        className="relative overflow-hidden"
        style={{ height, cursor: isDragging ? 'grabbing' : 'grab' }}
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
        onClick={(e) => {
          // Предотвращаем клики на графике от триггера submit формы
          e.stopPropagation()
        }}
      >
        <div
          style={{
            transform: `scale(${zoom}) translate(${pan.x / zoom}px, ${pan.y / zoom}px)`,
            transformOrigin: 'center center',
            transition: isDragging ? 'none' : 'transform 0.1s',
            width: '100%',
            height: '100%',
          }}
        >
          <ResponsiveContainer width="100%" height={height}>
            <LineChart
              data={chartData}
              margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
            >
              <CartesianGrid strokeDasharray="3 3" stroke="#475569" />
              <XAxis
                dataKey="x"
                type="number"
                domain={[xDomainMin, xDomainMax]}
                ticks={xTicks}
                stroke="#cbd5e1"
                tick={{ fill: '#cbd5e1', fontSize: 12 }}
                tickFormatter={formatAxisValue}
                allowDecimals={false}
                label={{ value: 'X', position: 'insideBottom', offset: -5, fill: '#cbd5e1' }}
              />
              <YAxis
                type="number"
                domain={[yDomainMin, yDomainMax]}
                ticks={yTicks}
                stroke="#cbd5e1"
                tick={{ fill: '#cbd5e1', fontSize: 12 }}
                tickFormatter={formatAxisValue}
                allowDecimals={false}
                label={{ value: 'Y', angle: -90, position: 'insideLeft', fill: '#cbd5e1' }}
              />
              <Tooltip content={<CustomTooltip />} />
              <Legend wrapperStyle={{ color: '#cbd5e1' }} />
              <Line
                type="monotone"
                dataKey="y"
                stroke="#3b82f6"
                strokeWidth={2}
                dot={showPoints ? { r: 4, fill: '#3b82f6' } : false}
                activeDot={{ r: 6 }}
                name={functionData.name}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>
      <p className="text-xs text-dark-text2 mt-2">
        Используйте колесико мыши для масштабирования, зажмите левую кнопку мыши для перемещения
      </p>
      {functionData.expression && (
        <p className="text-sm text-dark-text2 mt-2">
          Выражение: <code className="bg-dark-surface2 px-2 py-1 rounded">
            {functionData.expression}
          </code>
        </p>
      )}
    </div>
  )
}

export default FunctionGraph

