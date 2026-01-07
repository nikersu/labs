import React, { useState, useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useNotification } from '../common/NotificationContext'
import { functionService } from '../../services/functionService'
import { FunctionDto } from '../../types'
import { extractErrorMessage } from '../../utils/errorHandler'
import { saveFunctionToFile } from '../../utils/fileUtils'
import FunctionGraph from './FunctionGraph'
import LoadFunctionModal from './LoadFunctionModal'

const FunctionList: React.FC = () => {
  const { user } = useAuth()
  const { showError, showSuccess } = useNotification()
  const [functions, setFunctions] = useState<FunctionDto[]>([])
  const [filteredFunctions, setFilteredFunctions] = useState<FunctionDto[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')
  const [sortBy, setSortBy] = useState<'id' | 'name'>('id')
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('asc')
  const [expandedFunctions, setExpandedFunctions] = useState<Set<number>>(new Set())
  const [draggedIndex, setDraggedIndex] = useState<number | null>(null)
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null)
  const dragStartPos = useRef<{ x: number; y: number } | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)

  useEffect(() => {
    if (user) {
      loadFunctions()
    }
  }, [user])

  useEffect(() => {
    filterAndSortFunctions()
  }, [functions, searchTerm, sortBy, sortDir])

  const loadFunctions = async () => {
    if (!user) return

    setIsLoading(true)
    try {
      const data = await functionService.getByUserId(user.id)
      setFunctions(data)
    } catch (error) {
      showError(extractErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  const filterAndSortFunctions = () => {
    let filtered = [...functions]

    // Фильтрация по поисковому запросу
    if (searchTerm.trim()) {
      filtered = filtered.filter((func) =>
        func.name.toLowerCase().includes(searchTerm.toLowerCase())
      )
    }

    // Сортировка
    filtered.sort((a, b) => {
      let comparison = 0
      if (sortBy === 'name') {
        comparison = a.name.localeCompare(b.name)
      } else {
        comparison = (a.id || 0) - (b.id || 0)
      }
      return sortDir === 'asc' ? comparison : -comparison
    })

    setFilteredFunctions(filtered)
  }

  const handleDelete = async (id: number) => {
    if (!window.confirm('Вы уверены, что хотите удалить эту функцию?')) {
      return
    }

    try {
      await functionService.delete(id)
      showSuccess('Функция успешно удалена')
      loadFunctions()
    } catch (error) {
      showError(extractErrorMessage(error))
    }
  }

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (file) {
      setSelectedFile(file)
    }
    // Сбрасываем значение input, чтобы можно было выбрать тот же файл снова
    if (event.target) event.target.value = ''
  }

  const handleLoadSuccess = () => {
    loadFunctions()
    setSelectedFile(null)
  }

  const handleMouseDown = (e: React.MouseEvent, index: number) => {
    // Только правая кнопка мыши (button === 2)
    if (e.button !== 2) return
    
    // Предотвращаем контекстное меню
    e.preventDefault()
    e.stopPropagation()
    
    // Предотвращаем drag при клике на кнопки
    const target = e.target as HTMLElement
    if (target.tagName === 'BUTTON' || target.tagName === 'A' || target.closest('button') || target.closest('a')) {
      return
    }
    
    setDraggedIndex(index)
    dragStartPos.current = { x: e.clientX, y: e.clientY }
    
    if (e.currentTarget instanceof HTMLElement) {
      e.currentTarget.style.opacity = '0.5'
      e.currentTarget.style.cursor = 'grabbing'
    }
  }

  const handleMouseMove = (e: React.MouseEvent, index: number) => {
    if (draggedIndex === null || dragStartPos.current === null) return
    
    // Проверяем, что мышь переместилась достаточно далеко
    const deltaX = Math.abs(e.clientX - dragStartPos.current.x)
    const deltaY = Math.abs(e.clientY - dragStartPos.current.y)
    
    if (deltaX > 5 || deltaY > 5) {
      // Находим элемент под курсором
      const elementBelow = document.elementFromPoint(e.clientX, e.clientY)
      if (elementBelow) {
        const cardElement = elementBelow.closest('[data-function-index]')
        if (cardElement) {
          const targetIndex = parseInt(cardElement.getAttribute('data-function-index') || '-1')
          if (targetIndex !== -1 && targetIndex !== draggedIndex) {
            setDragOverIndex(targetIndex)
          }
        }
      }
    }
  }

  const handleMouseUp = (e: React.MouseEvent, index: number) => {
    if (draggedIndex === null) return
    
    // Восстанавливаем стили
    const draggedElement = document.querySelector(`[data-function-index="${draggedIndex}"]`) as HTMLElement
    if (draggedElement) {
      draggedElement.style.opacity = '1'
      draggedElement.style.cursor = 'move'
    }
    
    // Если мышь отпущена над другой карточкой, меняем порядок
    if (dragOverIndex !== null && draggedIndex !== dragOverIndex) {
      const newFilteredFunctions = [...filteredFunctions]
      const draggedItem = newFilteredFunctions[draggedIndex]
      newFilteredFunctions.splice(draggedIndex, 1)
      newFilteredFunctions.splice(dragOverIndex, 0, draggedItem)
      setFilteredFunctions(newFilteredFunctions)
    }
    
    setDraggedIndex(null)
    setDragOverIndex(null)
    dragStartPos.current = null
  }

  // Глобальный обработчик для отслеживания движения мыши
  useEffect(() => {
    if (draggedIndex === null) return

    const handleGlobalMouseMove = (e: MouseEvent) => {
      if (draggedIndex === null || dragStartPos.current === null) return
      
      // Находим элемент под курсором
      const elementBelow = document.elementFromPoint(e.clientX, e.clientY)
      if (elementBelow) {
        const cardElement = elementBelow.closest('[data-function-index]')
        if (cardElement) {
          const targetIndex = parseInt(cardElement.getAttribute('data-function-index') || '-1')
          if (targetIndex !== -1 && targetIndex !== draggedIndex) {
            setDragOverIndex(targetIndex)
          } else {
            setDragOverIndex(null)
          }
        } else {
          setDragOverIndex(null)
        }
      }
    }

    const handleGlobalMouseUp = () => {
      if (draggedIndex === null) return
      
      // Восстанавливаем стили
      const draggedElement = document.querySelector(`[data-function-index="${draggedIndex}"]`) as HTMLElement
      if (draggedElement) {
        draggedElement.style.opacity = '1'
        draggedElement.style.cursor = 'move'
      }
      
      // Если мышь отпущена над другой карточкой, меняем порядок
      if (dragOverIndex !== null && draggedIndex !== dragOverIndex) {
        const newFilteredFunctions = [...filteredFunctions]
        const draggedItem = newFilteredFunctions[draggedIndex]
        newFilteredFunctions.splice(draggedIndex, 1)
        newFilteredFunctions.splice(dragOverIndex, 0, draggedItem)
        setFilteredFunctions(newFilteredFunctions)
      }
      
      setDraggedIndex(null)
      setDragOverIndex(null)
      dragStartPos.current = null
    }

    document.addEventListener('mousemove', handleGlobalMouseMove)
    document.addEventListener('mouseup', handleGlobalMouseUp)
    document.addEventListener('contextmenu', (e) => e.preventDefault()) // Предотвращаем контекстное меню при перетаскивании

    return () => {
      document.removeEventListener('mousemove', handleGlobalMouseMove)
      document.removeEventListener('mouseup', handleGlobalMouseUp)
      document.removeEventListener('contextmenu', (e) => e.preventDefault())
    }
  }, [draggedIndex, dragOverIndex, filteredFunctions])

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-dark-text2">Загрузка функций...</div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <h1 className="text-3xl font-bold text-dark-text">Мои функции</h1>
        <div className="flex gap-2">
          <button
            onClick={() => fileInputRef.current?.click()}
            className="bg-dark-surface2 hover:bg-dark-surface text-dark-text px-4 py-2 rounded-lg font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-dark-primary"
          >
            Загрузить функцию
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept=".json"
            onChange={handleFileSelect}
            className="hidden"
          />
          <Link
            to="/functions/new"
            className="bg-dark-primary hover:bg-dark-primaryHover text-white px-4 py-2 rounded-lg font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-dark-primary"
          >
            Создать функцию
          </Link>
        </div>
      </div>

      {/* Поиск и фильтры */}
      <div className="bg-dark-surface rounded-lg p-4 border border-gray-700">
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="flex-1">
            <input
              type="text"
              placeholder="Поиск по названию..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full px-4 py-2 bg-dark-surface2 border border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text"
            />
          </div>
          <div className="flex gap-2">
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as 'id' | 'name')}
              className="px-4 py-2 bg-dark-surface2 border border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text"
            >
              <option value="id">Сортировать по ID</option>
              <option value="name">Сортировать по имени</option>
            </select>
            <button
              onClick={() => setSortDir(sortDir === 'asc' ? 'desc' : 'asc')}
              className="px-4 py-2 bg-dark-surface2 hover:bg-dark-surface border border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-dark-primary text-dark-text"
              aria-label={`Сортировка: ${sortDir === 'asc' ? 'по возрастанию' : 'по убыванию'}`}
            >
              {sortDir === 'asc' ? '↑' : '↓'}
            </button>
          </div>
        </div>
      </div>

      {/* Список функций */}
      {filteredFunctions.length === 0 ? (
        <div className="bg-dark-surface rounded-lg p-8 text-center border border-gray-700">
          <p className="text-dark-text2 text-lg">
            {searchTerm ? 'Функции не найдены' : 'У вас пока нет функций'}
          </p>
          {!searchTerm && (
            <Link
              to="/functions/new"
              className="inline-block mt-4 text-dark-primary hover:text-dark-primaryHover font-medium"
            >
              Создать первую функцию
            </Link>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 items-start">
          {filteredFunctions.map((func, index) => {
            const isExpanded = func.id !== undefined && expandedFunctions.has(func.id)
            const isDragging = draggedIndex === index
            const isDragOver = dragOverIndex === index
            return (
            <div
              key={func.id || `func-${Math.random()}`}
              data-function-index={index}
              onMouseDown={(e) => handleMouseDown(e, index)}
              onContextMenu={(e) => e.preventDefault()} // Предотвращаем контекстное меню
              className={`bg-dark-surface rounded-lg p-6 border transition-all fade-in ${
                isDragOver 
                  ? 'border-dark-primary border-2 scale-105 shadow-lg' 
                  : 'border-gray-700 hover:border-dark-primary'
              } ${isDragging ? 'opacity-50' : 'cursor-move'}`}
              title="Зажмите правую кнопку мыши и перетащите для изменения порядка"
            >
              <div className="flex justify-between items-center mb-4 gap-4">
                <div className="flex-1 min-w-0">
                  <h2 className="text-xl font-semibold text-dark-text truncate" title={func.name}>
                    {func.name}
                    {func.id && (
                      <span className="text-sm font-normal text-dark-text2 ml-2">(ID: {func.id})</span>
                    )}
                  </h2>
                </div>
                <div className="flex gap-2 flex-shrink-0" onClick={(e) => e.stopPropagation()}>
                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      if (!func.id) return
                      const newExpanded = new Set(expandedFunctions)
                      if (newExpanded.has(func.id)) {
                        newExpanded.delete(func.id)
                      } else {
                        newExpanded.add(func.id)
                      }
                      setExpandedFunctions(newExpanded)
                    }}
                    disabled={!func.id}
                    className="px-3 py-1 bg-dark-surface2 hover:bg-dark-surface text-dark-text text-sm rounded focus:outline-none focus:ring-2 focus:ring-dark-primary disabled:opacity-50 disabled:cursor-not-allowed"
                    aria-label={isExpanded ? 'Скрыть график' : 'Показать график'}
                  >
                    {isExpanded ? 'Скрыть' : 'График'}
                  </button>
                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      try {
                        saveFunctionToFile(func)
                        showSuccess(`Функция "${func.name}" успешно сохранена в файл`)
                      } catch (error) {
                        showError(extractErrorMessage(error))
                      }
                    }}
                    disabled={!func.id}
                    className="px-3 py-1 bg-dark-surface2 hover:bg-dark-surface text-dark-text text-sm rounded focus:outline-none focus:ring-2 focus:ring-dark-primary disabled:opacity-50 disabled:cursor-not-allowed"
                    aria-label="Сохранить функцию в файл"
                    title="Сохранить функцию в JSON файл"
                  >
                    💾
                  </button>
                  <Link
                    to={`/functions/${func.id}/edit`}
                    className="px-3 py-1 bg-dark-primary hover:bg-dark-primaryHover text-white text-sm rounded focus:outline-none focus:ring-2 focus:ring-dark-primary"
                    onClick={(e) => e.stopPropagation()}
                  >
                    Редактировать
                  </Link>
                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      if (func.id) handleDelete(func.id)
                    }}
                    className="px-3 py-1 bg-red-600 hover:bg-red-700 text-white text-sm rounded focus:outline-none focus:ring-2 focus:ring-red-500"
                    aria-label="Удалить функцию"
                  >
                    Удалить
                  </button>
                </div>
              </div>
              
              {func.expression && (
                <p className="text-sm text-dark-text2 mb-2">
                  <code className="bg-dark-surface2 px-2 py-1 rounded break-all">{func.expression}</code>
                </p>
              )}
              <p className="text-xs text-dark-text2">
                Точек: {func.count || func.points?.length || func.xValues?.length || 0}
              </p>

              {isExpanded && (
                <div className="mt-4 slide-in">
                  <FunctionGraph functionData={func} height={300} />
                </div>
              )}
            </div>
            )
          })}
        </div>
      )}

      {/* Модальное окно загрузки функции */}
      {selectedFile && (
        <LoadFunctionModal
          file={selectedFile}
          onClose={() => setSelectedFile(null)}
          onSuccess={handleLoadSuccess}
        />
      )}
    </div>
  )
}

export default FunctionList

