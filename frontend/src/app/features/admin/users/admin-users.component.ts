import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Page, RoleDto, RoleName, UserDto } from '../../../core/models/api.models';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { UserService } from '../../../core/services/user.service';
import { FieldErrorComponent } from '../../../shared/components/field-error.component';
import { PaginationComponent } from '../../../shared/components/pagination.component';
import { RoleNamePipe } from '../../../shared/pipes/labels.pipes';
import { applyServerErrors } from '../../../shared/utils/form-errors';
import { belarusPhoneValidator, phoneOrNull } from '../../../shared/utils/phone';
import { PhoneMaskDirective } from '../../../shared/directives/phone-mask.directive';
import { PhonePipe } from '../../../shared/pipes/phone.pipe';

@Component({
  selector: 'app-admin-users',
  imports: [FormsModule, ReactiveFormsModule, DatePipe, RoleNamePipe, FieldErrorComponent, PaginationComponent, PhoneMaskDirective, PhonePipe],
  templateUrl: './admin-users.component.html',
})
export class AdminUsersComponent {
  private readonly fb = inject(FormBuilder);
  private readonly users = inject(UserService);
  private readonly notify = inject(NotificationService);
  protected readonly auth = inject(AuthService);

  protected q = '';
  protected role: RoleName | null = null;
  protected readonly roles = signal<RoleDto[]>([]);
  protected readonly page = signal<Page<UserDto> | null>(null);
  protected readonly loading = signal(false);

  protected readonly createOpen = signal(false);
  protected readonly creating = signal(false);
  protected readonly newRoles = signal<Set<RoleName>>(new Set(['CUSTOMER']));

  /** Редактирование ролей: id пользователя и выбранные роли. */
  protected readonly editingRolesFor = signal<number | null>(null);
  protected readonly editRoles = signal<Set<RoleName>>(new Set());

  protected readonly createForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    phone: ['', [belarusPhoneValidator]],
  });

  constructor() {
    this.users.roles().subscribe((r) => this.roles.set(r));
    this.load(0);
  }

  load(page: number): void {
    this.loading.set(true);
    this.users.list({ q: this.q.trim() || null, role: this.role, page, size: 20 }).subscribe({
      next: (p) => {
        this.page.set(p);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  isSelf(u: UserDto): boolean {
    return this.auth.currentUser()?.id === u.id;
  }

  // ----- Создание -----
  toggleNewRole(r: RoleName): void {
    const next = new Set(this.newRoles());
    if (next.has(r)) next.delete(r);
    else next.add(r);
    this.newRoles.set(next);
  }

  openCreate(): void {
    this.createForm.reset();
    this.newRoles.set(new Set(['CUSTOMER']));
    this.createOpen.set(true);
  }

  create(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    if (!this.newRoles().size) {
      this.notify.error('Выберите хотя бы одну роль');
      return;
    }
    const v = this.createForm.getRawValue();
    this.creating.set(true);
    this.users.create({ ...v, phone: phoneOrNull(v.phone), roles: [...this.newRoles()] }).subscribe({
      next: (u) => {
        this.creating.set(false);
        this.createOpen.set(false);
        this.notify.success(`Пользователь ${u.email} создан`);
        this.load(0);
      },
      error: (err) => {
        this.creating.set(false);
        applyServerErrors(this.createForm, err);
      },
    });
  }

  // ----- Роли -----
  startEditRoles(u: UserDto): void {
    this.editingRolesFor.set(u.id);
    this.editRoles.set(new Set(u.roles));
  }

  toggleEditRole(r: RoleName): void {
    const next = new Set(this.editRoles());
    if (next.has(r)) next.delete(r);
    else next.add(r);
    this.editRoles.set(next);
  }

  saveRoles(u: UserDto): void {
    const roles = [...this.editRoles()];
    if (!roles.length) {
      this.notify.error('Список ролей не может быть пустым');
      return;
    }
    this.users.setRoles(u.id, roles).subscribe((updated) => {
      this.replace(updated);
      this.editingRolesFor.set(null);
      this.notify.success('Роли обновлены');
      if (this.isSelf(updated)) this.auth.setUser(updated);
    });
  }

  toggleEnabled(u: UserDto): void {
    const enabled = !u.enabled;
    if (!enabled && !confirm(`Заблокировать пользователя ${u.email}?`)) return;
    this.users.setEnabled(u.id, enabled).subscribe((updated) => {
      this.replace(updated);
      this.notify.success(enabled ? 'Пользователь разблокирован' : 'Пользователь заблокирован');
    });
  }

  private replace(u: UserDto): void {
    this.page.update((p) => (p ? { ...p, content: p.content.map((x) => (x.id === u.id ? u : x)) } : p));
  }
}
