import api from './api'
import { FunctionDto, CreateFunctionFromMathRequest, CreateFunctionFromArraysRequest } from '../types'

export const functionService = {
  async getAll(): Promise<FunctionDto[]> {
    const response = await api.get<FunctionDto[]>('/functions')
    return response.data
  },

  async getById(id: number): Promise<FunctionDto> {
    const response = await api.get<FunctionDto>(`/functions/${id}`)
    return response.data
  },

  async getByUserId(userId: number): Promise<FunctionDto[]> {
    const response = await api.get<FunctionDto[]>(`/functions/user/${userId}`)
    return response.data
  },

  async search(userId: number, name?: string, sortBy: string = 'id', sortDir: string = 'asc'): Promise<FunctionDto[]> {
    const params = new URLSearchParams()
    if (name) params.append('name', name)
    params.append('sortBy', sortBy)
    params.append('sortDir', sortDir)
    
    const response = await api.get<FunctionDto[]>(`/functions/user/${userId}/search?${params.toString()}`)
    return response.data
  },

  async create(functionDto: FunctionDto): Promise<FunctionDto> {
    const response = await api.post<FunctionDto>('/functions', functionDto)
    return response.data
  },

  async update(id: number, functionDto: FunctionDto): Promise<FunctionDto> {
    const response = await api.put<FunctionDto>(`/functions/${id}`, functionDto)
    return response.data
  },

  async delete(id: number): Promise<void> {
    await api.delete(`/functions/${id}`)
  },

  async previewFromMath(request: CreateFunctionFromMathRequest): Promise<FunctionDto> {
    const response = await api.post<FunctionDto>('/functions/preview-from-math', request)
    return response.data
  },

  async createFromMath(request: CreateFunctionFromMathRequest): Promise<FunctionDto> {
    const response = await api.post<FunctionDto>('/functions/create-from-math', request)
    return response.data
  },

  async createFromArrays(request: CreateFunctionFromArraysRequest): Promise<FunctionDto> {
    const response = await api.post<FunctionDto>('/functions/create-from-arrays', request)
    return response.data
  },

  async getAvailableMathFunctions(): Promise<string[]> {
    const response = await api.get<string[]>('/functions/available-math-functions')
    return response.data
  },

  async performOperation(
    functionId1: number,
    functionId2: number,
    operation: 'ADD' | 'SUBTRACT' | 'MULTIPLY' | 'DIVIDE',
    resultName: string,
    factoryType: string = 'ARRAY'
  ): Promise<FunctionDto> {
    const response = await api.post<FunctionDto>('/functions/operate', {
      functionId1,
      functionId2,
      operation,
      resultName,
      factoryType,
    })
    return response.data
  },

  async createCompositeFunction(
    name: string,
    innerFunction: string,
    outerFunction: string
  ): Promise<{ name: string; innerFunction: string; outerFunction: string }> {
    const response = await api.post<{ name: string; innerFunction: string; outerFunction: string }>('/functions/composite', {
      name,
      innerFunction,
      outerFunction,
    })
    return response.data
  },

  async updateYValues(functionId: number, yValues: number[]): Promise<FunctionDto> {
    const response = await api.put<FunctionDto>(`/functions/${functionId}/y-values`, { yValues })
    return response.data
  },

  async insertPoint(functionId: number, x: number, y: number): Promise<FunctionDto> {
    const response = await api.post<FunctionDto>(`/functions/${functionId}/insert-point`, { x, y })
    return response.data
  },

  async removePoint(functionId: number, index: number): Promise<FunctionDto> {
    const response = await api.delete<FunctionDto>(`/functions/${functionId}/remove-point/${index}`)
    return response.data
  },
}

