import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { UserService } from '../../core/services/user.service';
import { FieldErrorComponent } from '../../shared/components/field-error.component';
import { RoleNamePipe } from '../../shared/pipes/labels.pipes';
import { applyServerErrors } from '../../shared/utils/form-errors';
import { passwordsMatch } from '../../shared/utils/validators';
import { belarusPhoneValidator, formatBelarusPhone, phoneOrNull } from '../../shared/utils/phone';
import { PhoneMaskDirective } from '../../shared/directives/phone-mask.directive';

@Component({
  selector: 'app-profile',
  imports: [ReactiveFormsModule, DatePipe, FieldErrorComponent, RoleNamePipe, PhoneMaskDirective],
  template: `
    <div class="container">
      <h1>Профиль</h1>
      @if (auth.currentUser(); as u) {
        <p class="muted">
          {{ u.email }} ·
          @for (r of u.roles; track r) {
            <span class="badge badge-muted">{{ r | roleName }}</span>
          }
          · с нами с {{ u.createdAt | date: 'dd.MM.yyyy' }}
        </p>
      }
      <div class="grid-2 align-start">
        <form class="card form-card" [formGroup]="profileForm" (ngSubmit)="saveProfile()" novalidate>
          <h3>Личные данные</h3>
          <label class="field">
            <span>Имя</span>
            <input formControlName="firstName" />
            <app-field-error [control]="profileForm.controls.firstName" />
          </label>
          <label class="field">
            <span>Фамилия</span>
            <input formControlName="lastName" />
            <app-field-error [control]="profileForm.controls.lastName" />
          </label>
          <label class="field">
            <span>Телефон</span>
            <input type="tel" formControlName="phone" appPhoneMask placeholder="+375 (29) 123-45-67" />
            <app-field-error [control]="profileForm.controls.phone" />
          </label>
          <button type="submit" class="btn btn-primary" [disabled]="savingProfile()">Сохранить</button>
        </form>

        <form class="card form-card" [formGroup]="passwordForm" (ngSubmit)="changePassword()" novalidate>
          <h3>Смена пароля</h3>
          <label class="field">
            <span>Текущий пароль</span>
            <input type="password" formControlName="currentPassword" autocomplete="current-password" />
            <app-field-error [control]="passwordForm.controls.currentPassword" />
          </label>
          <label class="field">
            <span>Новый пароль</span>
            <input type="password" formControlName="newPassword" autocomplete="new-password" />
            <app-field-error [control]="passwordForm.controls.newPassword" />
          </label>
          <label class="field">
            <span>Повторите новый пароль</span>
            <input type="password" formControlName="confirm" autocomplete="new-password" />
            <app-field-error [control]="passwordForm.controls.confirm" />
          </label>
          <button type="submit" class="btn btn-primary" [disabled]="savingPassword()">Изменить пароль</button>
        </form>
      </div>
    </div>
  `,
})
export class ProfileComponent {
  private readonly fb = inject(FormBuilder);
  protected readonly auth = inject(AuthService);
  private readonly users = inject(UserService);
  private readonly notify = inject(NotificationService);

  protected readonly savingProfile = signal(false);
  protected readonly savingPassword = signal(false);

  protected readonly profileForm = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    phone: ['', [belarusPhoneValidator]],
  });

  protected readonly passwordForm = this.fb.nonNullable.group(
    {
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
      confirm: ['', [Validators.required]],
    },
    { validators: passwordsMatch('newPassword', 'confirm') },
  );

  constructor() {
    const u = this.auth.currentUser();
    if (u) this.patch(u.firstName, u.lastName, u.phone);
    this.users.getProfile().subscribe((p) => {
      this.auth.setUser(p);
      this.patch(p.firstName, p.lastName, p.phone);
    });
  }

  private patch(firstName: string, lastName: string, phone: string | null): void {
    this.profileForm.setValue({ firstName, lastName, phone: phone ? formatBelarusPhone(phone) : '' });
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }
    const v = this.profileForm.getRawValue();
    this.savingProfile.set(true);
    this.users.updateProfile({ ...v, phone: phoneOrNull(v.phone) }).subscribe({
      next: (u) => {
        this.auth.setUser(u);
        this.savingProfile.set(false);
        this.notify.success('Профиль сохранён');
      },
      error: (err) => {
        this.savingProfile.set(false);
        applyServerErrors(this.profileForm, err);
      },
    });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    const { currentPassword, newPassword } = this.passwordForm.getRawValue();
    this.savingPassword.set(true);
    this.users.changePassword({ currentPassword, newPassword }).subscribe({
      next: () => {
        this.savingPassword.set(false);
        this.passwordForm.reset();
        this.notify.success('Пароль изменён');
      },
      error: (err) => {
        this.savingPassword.set(false);
        applyServerErrors(this.passwordForm, err);
      },
    });
  }
}
