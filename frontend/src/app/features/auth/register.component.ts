import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { FieldErrorComponent } from '../../shared/components/field-error.component';
import { applyServerErrors } from '../../shared/utils/form-errors';
import { passwordsMatch } from '../../shared/utils/validators';
import { belarusPhoneValidator, phoneOrNull } from '../../shared/utils/phone';
import { PhoneMaskDirective } from '../../shared/directives/phone-mask.directive';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, FieldErrorComponent, PhoneMaskDirective],
  template: `
    <div class="container auth-page">
      <form class="card auth-card" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <h1>Регистрация</h1>
        <div class="grid-2">
          <label class="field">
            <span>Имя</span>
            <input formControlName="firstName" autocomplete="given-name" />
            <app-field-error [control]="form.controls.firstName" />
          </label>
          <label class="field">
            <span>Фамилия</span>
            <input formControlName="lastName" autocomplete="family-name" />
            <app-field-error [control]="form.controls.lastName" />
          </label>
        </div>
        <label class="field">
          <span>Email</span>
          <input type="email" formControlName="email" autocomplete="email" />
          <app-field-error [control]="form.controls.email" />
        </label>
        <label class="field">
          <span>Телефон <span class="muted">(необязательно)</span></span>
          <input type="tel" formControlName="phone" appPhoneMask placeholder="+375 (29) 123-45-67" autocomplete="tel" />
          <app-field-error [control]="form.controls.phone" />
        </label>
        <div class="grid-2">
          <label class="field">
            <span>Пароль</span>
            <input type="password" formControlName="password" autocomplete="new-password" />
            <app-field-error [control]="form.controls.password" />
          </label>
          <label class="field">
            <span>Повторите пароль</span>
            <input type="password" formControlName="confirm" autocomplete="new-password" />
            <app-field-error [control]="form.controls.confirm" />
          </label>
        </div>
        <button type="submit" class="btn btn-primary btn-block" [disabled]="loading()">
          {{ loading() ? 'Регистрация…' : 'Зарегистрироваться' }}
        </button>
        <p class="auth-switch">Уже есть аккаунт? <a routerLink="/login">Войти</a></p>
      </form>
    </div>
  `,
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notify = inject(NotificationService);

  protected readonly loading = signal(false);
  protected readonly form = this.fb.nonNullable.group(
    {
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [belarusPhoneValidator]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
      confirm: ['', [Validators.required]],
    },
    { validators: passwordsMatch('password', 'confirm') },
  );

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { confirm: _confirm, phone, ...rest } = this.form.getRawValue();
    this.loading.set(true);
    this.auth.register({ ...rest, phone: phoneOrNull(phone) }).subscribe({
      next: (r) => {
        this.notify.success(`Добро пожаловать, ${r.user.firstName}!`);
        this.router.navigateByUrl('/catalog');
      },
      error: (err) => {
        this.loading.set(false);
        applyServerErrors(this.form, err);
      },
    });
  }
}
