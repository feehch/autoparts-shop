import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ChangePasswordRequest,
  CreateUserRequest,
  Page,
  RoleDto,
  RoleName,
  UpdateProfileRequest,
  UserDto,
} from '../models/api.models';
import { toHttpParams } from './http-params.util';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  // Профиль текущего пользователя
  getProfile(): Observable<UserDto> {
    return this.http.get<UserDto>(`${this.api}/profile`);
  }
  updateProfile(req: UpdateProfileRequest): Observable<UserDto> {
    return this.http.put<UserDto>(`${this.api}/profile`, req);
  }
  changePassword(req: ChangePasswordRequest): Observable<void> {
    return this.http.put<void>(`${this.api}/profile/password`, req);
  }

  // Администрирование пользователей
  list(params: { q?: string | null; role?: RoleName | null; page?: number; size?: number }): Observable<Page<UserDto>> {
    return this.http.get<Page<UserDto>>(`${this.api}/users`, { params: toHttpParams(params) });
  }
  create(req: CreateUserRequest): Observable<UserDto> {
    return this.http.post<UserDto>(`${this.api}/users`, req);
  }
  setRoles(id: number, roles: RoleName[]): Observable<UserDto> {
    return this.http.put<UserDto>(`${this.api}/users/${id}/roles`, { roles });
  }
  setEnabled(id: number, enabled: boolean): Observable<UserDto> {
    return this.http.patch<UserDto>(`${this.api}/users/${id}/enabled`, { enabled });
  }
  roles(): Observable<RoleDto[]> {
    return this.http.get<RoleDto[]>(`${this.api}/users/roles`);
  }
}
