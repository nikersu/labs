export interface UserDto {
  id: number
  username: string
  passwordHash?: string
  role: string
}

export interface FunctionDto {
  id?: number
  name: string
  expression: string
  userId: number
  points?: PointDto[]
  xValues?: number[]
  yValues?: number[]
  count?: number
  isInsertable?: boolean
  isRemovable?: boolean
}

export interface PointDto {
  functionId: number
  xValue: number
  yValue: number
}

export interface RegisterRequest {
  username: string
  password: string
}

export interface LoginCredentials {
  username: string
  password: string
}

export type FunctionType = 'math' | 'array' | 'linkedlist'

export interface FunctionFormData {
  name: string
  type: FunctionType
  expression?: string
  points?: Array<{ x: number; y: number }>
  // Для математических функций
  mathFromX?: number
  mathToX?: number
  mathPointCount?: number
}

export interface CreateFunctionFromMathRequest {
  name: string
  mathFunctionType: string
  expression?: string // Для пользовательских выражений
  xFrom: number
  xTo: number
  count: number
  factoryType: string
}

export interface CreateFunctionFromArraysRequest {
  name: string
  xValues: number[]
  yValues: number[]
  factoryType: string
}

