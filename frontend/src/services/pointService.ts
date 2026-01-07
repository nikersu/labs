import api from './api'
import { PointDto } from '../types'

export const pointService = {
  async getByFunctionId(functionId: number): Promise<PointDto[]> {
    const response = await api.get<PointDto[]>(`/points/function/${functionId}`)
    return response.data
  },

  async getByRange(
    functionId: number,
    fromX?: number,
    toX?: number,
    sortBy: string = 'id.xValue',
    sortDir: string = 'asc'
  ): Promise<PointDto[]> {
    const params = new URLSearchParams()
    if (fromX !== undefined) params.append('fromX', fromX.toString())
    if (toX !== undefined) params.append('toX', toX.toString())
    params.append('sortBy', sortBy)
    params.append('sortDir', sortDir)

    const response = await api.get<PointDto[]>(`/points/function/${functionId}/range?${params.toString()}`)
    return response.data
  },

  async getByX(functionId: number, xValue: number): Promise<PointDto> {
    const response = await api.get<PointDto>(`/points/function/${functionId}/x/${xValue}`)
    return response.data
  },

  async create(pointDto: PointDto): Promise<PointDto> {
    const response = await api.post<PointDto>('/points', pointDto)
    return response.data
  },

  async update(functionId: number, xValue: number, pointDto: PointDto): Promise<PointDto> {
    const response = await api.put<PointDto>(`/points/function/${functionId}/x/${xValue}`, pointDto)
    return response.data
  },

  async delete(functionId: number, xValue: number): Promise<void> {
    await api.delete(`/points/function/${functionId}/x/${xValue}`)
  },
}





