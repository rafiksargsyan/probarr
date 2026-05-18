import type { User } from 'firebase/auth';
import { apiRequest } from './client';
import type { UserDTO } from '../types/api.types';

export function signUp(user: User): Promise<UserDTO> {
  return apiRequest<UserDTO>('/user/signup', user, { method: 'POST' });
}
