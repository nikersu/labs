import React from 'react'

interface Point {
  x: number
  y: number
}

interface PointTableProps {
  points: Point[]
  onPointChange: (index: number, field: 'x' | 'y', value: number) => void
  onPointRemove: (index: number) => void
  readOnly?: boolean
}

const PointTable: React.FC<PointTableProps> = ({ points, onPointChange, onPointRemove, readOnly = false }) => {
  if (points.length === 0) {
    return (
      <div className="bg-dark-surface2 rounded-lg p-4 text-center text-dark-text2 border border-gray-600">
        Добавьте точки функции
      </div>
    )
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse bg-dark-surface2 rounded-lg border border-gray-600">
        <thead>
          <tr className="bg-dark-surface">
            <th className="px-4 py-2 text-left text-sm font-medium text-dark-text border-b border-gray-600">
              X
            </th>
            <th className="px-4 py-2 text-left text-sm font-medium text-dark-text border-b border-gray-600">
              Y
            </th>
            <th className="px-4 py-2 text-left text-sm font-medium text-dark-text border-b border-gray-600">
              Действия
            </th>
          </tr>
        </thead>
        <tbody>
          {points.map((point, index) => (
            <tr key={index} className="hover:bg-dark-surface transition-colors">
              <td className="px-4 py-2 border-b border-gray-600">
                {readOnly ? (
                  <span className="text-dark-text">
                    {typeof point.x === 'number' && !isNaN(point.x) ? point.x.toFixed(4) : 'N/A'}
                  </span>
                ) : (
                  <input
                    type="number"
                    step="any"
                    value={typeof point.x === 'number' && !isNaN(point.x) ? point.x : ''}
                    onChange={(e) => {
                      const val = e.target.value.trim()
                      if (val === '') {
                        // Если поле пустое, передаем NaN, чтобы валидация могла его отфильтровать
                        onPointChange(index, 'x', NaN)
                      } else {
                        const numValue = parseFloat(val)
                        onPointChange(index, 'x', isNaN(numValue) ? NaN : numValue)
                      }
                    }}
                    className="w-full px-2 py-1 bg-dark-surface2 border border-gray-600 rounded text-dark-text focus:outline-none focus:ring-2 focus:ring-dark-primary"
                  />
                )}
              </td>
              <td className="px-4 py-2 border-b border-gray-600">
                {readOnly ? (
                  <span className="text-dark-text">
                    {typeof point.y === 'number' && !isNaN(point.y) ? point.y.toFixed(4) : 'N/A'}
                  </span>
                ) : (
                  <input
                    type="number"
                    step="any"
                    value={typeof point.y === 'number' && !isNaN(point.y) ? point.y : ''}
                    onChange={(e) => {
                      const val = e.target.value.trim()
                      if (val === '') {
                        // Если поле пустое, передаем NaN, чтобы валидация могла его отфильтровать
                        onPointChange(index, 'y', NaN)
                      } else {
                        const numValue = parseFloat(val)
                        onPointChange(index, 'y', isNaN(numValue) ? NaN : numValue)
                      }
                    }}
                    className="w-full px-2 py-1 bg-dark-surface2 border border-gray-600 rounded text-dark-text focus:outline-none focus:ring-2 focus:ring-dark-primary"
                  />
                )}
              </td>
              <td className="px-4 py-2 border-b border-gray-600">
                {!readOnly && (
                  <button
                    onClick={() => onPointRemove(index)}
                    className="px-3 py-1 bg-red-600 hover:bg-red-700 text-white text-sm rounded focus:outline-none focus:ring-2 focus:ring-red-500"
                    aria-label={`Удалить точку ${index + 1}`}
                  >
                    Удалить
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default PointTable

