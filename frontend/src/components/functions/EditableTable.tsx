import React, { useState } from 'react'
import { FunctionDto } from '../../types'
import { functionService } from '../../services/functionService'
import { useNotification } from '../common/NotificationContext'
import { extractErrorMessage } from '../../utils/errorHandler'

interface EditableTableProps {
  func: FunctionDto | null
  editable?: boolean
  onUpdate?: (func: FunctionDto) => void
  isInsertable?: boolean
  isRemovable?: boolean
}

const EditableTable: React.FC<EditableTableProps> = ({ 
  func, 
  editable = false, 
  onUpdate,
  isInsertable = false,
  isRemovable = false
}) => {
  const { showError, showSuccess } = useNotification()
  const [editedYValues, setEditedYValues] = useState<number[]>([])
  const [hasChanges, setHasChanges] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [insertX, setInsertX] = useState('')
  const [insertY, setInsertY] = useState('')
  const [showInsertForm, setShowInsertForm] = useState(false)

  React.useEffect(() => {
    if (func) {
      const yValues = func.yValues || func.points?.map(p => p.yValue) || []
      setEditedYValues([...yValues])
      setHasChanges(false)
    }
  }, [func])

  if (!func) {
    return <div className="p-5 text-center text-dark-text2">Данные функции не загружены</div>
  }

  const xValues = func.xValues || func.points?.map(p => p.xValue) || []
  const yValues = func.yValues || func.points?.map(p => p.yValue) || []

  if (xValues.length === 0 || yValues.length === 0) {
    return <div className="p-5 text-center text-dark-text2">Нет данных для отображения</div>
  }

  const handleYChange = (index: number, value: string) => {
    const newYValues = [...editedYValues]
    newYValues[index] = parseFloat(value) || 0
    setEditedYValues(newYValues)
    setHasChanges(true)
  }

  const saveChanges = async () => {
    if (!func.id) return

    setSaving(true)
    try {
      const updated = await functionService.updateYValues(func.id, editedYValues)
      setHasChanges(false)
      showSuccess('Изменения сохранены')
      if (onUpdate) {
        onUpdate(updated)
      }
    } catch (error) {
      showError(extractErrorMessage(error))
    } finally {
      setSaving(false)
    }
  }

  const cancelChanges = () => {
    const yValues = func.yValues || func.points?.map(p => p.yValue) || []
    setEditedYValues([...yValues])
    setHasChanges(false)
    setError('')
  }

  const handleInsert = async () => {
    if (!insertX || !insertY || !func?.id) {
      setError('Введите значения X и Y для вставки')
      return
    }

    setSaving(true)
    setError('')

    try {
      const updated = await functionService.insertPoint(
        func.id,
        parseFloat(insertX),
        parseFloat(insertY)
      )
      setShowInsertForm(false)
      setInsertX('')
      setInsertY('')
      showSuccess('Точка успешно добавлена')
      if (onUpdate) {
        onUpdate(updated)
      }
    } catch (err) {
      const errorMsg = extractErrorMessage(err)
      setError(errorMsg)
      showError(errorMsg)
    } finally {
      setSaving(false)
    }
  }

  const handleRemove = async (index: number) => {
    if (!func?.id) return
    
    if (!window.confirm(`Удалить точку #${index + 1}?`)) {
      return
    }

    setSaving(true)
    setError('')

    try {
      const updated = await functionService.removePoint(func.id, index)
      showSuccess('Точка успешно удалена')
      if (onUpdate) {
        onUpdate(updated)
      }
    } catch (err) {
      const errorMsg = extractErrorMessage(err)
      setError(errorMsg)
      showError(errorMsg)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-4">
      {error && (
        <div className="p-3 bg-red-900/20 border border-red-600 rounded text-red-400 text-sm">
          {error}
        </div>
      )}
      
      <div className="overflow-x-auto max-h-96 overflow-y-auto">
        <table className="w-full border-collapse">
          <thead className="bg-dark-surface2 sticky top-0">
            <tr>
              <th className="px-4 py-2 text-left text-dark-text border border-gray-600">#</th>
              <th className="px-4 py-2 text-left text-dark-text border border-gray-600">X</th>
              <th className="px-4 py-2 text-left text-dark-text border border-gray-600">Y</th>
              {editable && isRemovable && (
                <th className="px-4 py-2 text-left text-dark-text border border-gray-600">Действия</th>
              )}
            </tr>
          </thead>
          <tbody>
            {xValues.map((x, index) => (
              <tr key={index} className="hover:bg-dark-surface2">
                <td className="px-4 py-2 text-dark-text2 border border-gray-600">{index + 1}</td>
                <td className="px-4 py-2 text-dark-text2 border border-gray-600">{x.toFixed(6)}</td>
                <td className="px-4 py-2 border border-gray-600">
                  {editable ? (
                    <input
                      type="number"
                      step="any"
                      value={editedYValues[index] ?? yValues[index]}
                      onChange={(e) => handleYChange(index, e.target.value)}
                      disabled={saving}
                      className="w-16 px-2 py-1 bg-dark-surface2 border border-gray-600 rounded text-dark-text focus:outline-none focus:ring-2 focus:ring-dark-primary"
                    />
                  ) : (
                    <span className="text-dark-text2">{(yValues[index] || 0).toFixed(6)}</span>
                  )}
                </td>
                {editable && isRemovable && (
                  <td className="px-4 py-2 border border-gray-600">
                    <button
                      onClick={() => handleRemove(index)}
                      disabled={saving}
                      className="px-3 py-1 bg-red-600 hover:bg-red-700 text-white text-sm rounded focus:outline-none focus:ring-2 focus:ring-red-500 disabled:opacity-50"
                      title="Удалить точку"
                    >
                      ✕
                    </button>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {editable && hasChanges && (
        <div className="flex gap-2">
          <button
            onClick={saveChanges}
            disabled={saving}
            className="flex-1 px-4 py-2 bg-dark-primary hover:bg-dark-primaryHover text-white rounded-lg transition-colors disabled:opacity-50"
          >
            {saving ? 'Сохранение...' : 'Сохранить изменения'}
          </button>
          <button
            onClick={cancelChanges}
            disabled={saving}
            className="flex-1 px-4 py-2 bg-dark-surface2 hover:bg-dark-surface text-dark-text rounded-lg transition-colors disabled:opacity-50"
          >
            Отменить
          </button>
        </div>
      )}

      {editable && isInsertable && (
        <div className="mt-4 p-4 bg-dark-surface2 rounded-lg border border-gray-600">
          {!showInsertForm ? (
            <button
              onClick={() => setShowInsertForm(true)}
              disabled={saving}
              className="px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg transition-colors disabled:opacity-50 focus:outline-none border-0"
              style={{ border: 'none', outline: 'none' }}
            >
              + Вставить точку
            </button>
          ) : (
            <div>
              <h4 className="text-lg font-semibold text-dark-text mb-3">Вставка новой точки</h4>
              <div className="grid grid-cols-2 gap-4 mb-4">
                <div>
                  <label className="block text-sm font-medium text-dark-text2 mb-2">X:</label>
                  <input
                    type="number"
                    step="any"
                    value={insertX}
                    onChange={(e) => setInsertX(e.target.value)}
                    placeholder="X координата"
                    disabled={saving}
                    className="w-full px-3 py-2 bg-dark-surface border border-gray-600 rounded text-dark-text focus:outline-none focus:ring-2 focus:ring-dark-primary"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-dark-text2 mb-2">Y:</label>
                  <input
                    type="number"
                    step="any"
                    value={insertY}
                    onChange={(e) => setInsertY(e.target.value)}
                    placeholder="Y координата"
                    disabled={saving}
                    className="w-full px-3 py-2 bg-dark-surface border border-gray-600 rounded text-dark-text focus:outline-none focus:ring-2 focus:ring-dark-primary"
                  />
                </div>
              </div>
              <div className="flex gap-2">
                <button
                  onClick={handleInsert}
                  disabled={saving}
                  className="flex-1 px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg transition-colors disabled:opacity-50"
                >
                  {saving ? 'Вставка...' : 'Вставить'}
                </button>
                <button
                  onClick={() => {
                    setShowInsertForm(false)
                    setInsertX('')
                    setInsertY('')
                    setError('')
                  }}
                  disabled={saving}
                  className="flex-1 px-4 py-2 bg-dark-surface2 hover:bg-dark-surface text-dark-text rounded-lg transition-colors disabled:opacity-50"
                >
                  Отмена
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default EditableTable




