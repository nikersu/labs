import api from './api'
import { UserDto } from '../types'

export const userService = {
  async update(id: number, data: Partial<UserDto>): Promise<UserDto> {
    const response = await api.put<UserDto>(`/users/${id}`, data)
    return response.data
  },
}





