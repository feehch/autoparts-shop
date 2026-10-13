import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { FieldErrorComponent } from '../../shared/components/field-error.component';
import { applyServerErrors } from '../../shared/utils/form-errors';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, FieldErrorComponent],
  template: `
    <div class="container auth-page">
      <form class="card auth-card" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <h1>Вход</h1>
        <p class="muted">Войдите, чтобы оформлять заказы и следить за их статусом.</p>

        <label class="field">
          <span>Email</span>
          <input type="email" formControlName="email" autocomplete="username" />
          <app-field-error [control]="form.controls.email" />
        </label>
        <label class="field">
          <span>Пароль</span>
          <input type="password" formControlName="password" autocomplete="current-password" />
          <app-field-error [control]="form.controls.password" />
        </label>

        <button type="submit" class="btn btn-primary btn-block" [disabled]="loading()">
          {{ loading() ? 'Вход…' : 'Войти' }}
        </button>
        <p class="auth-switch">Нет аккаунта? <a routerLink="/register">Зарегистрироваться</a></p>

        <details class="demo-accounts">
          <summary>Демонстрационные учётные записи</summary>
          <ul>
            <li><button type="button" class="link" (click)="fill('admin@autoparts.local', 'Admin123!')">admin@autoparts.local</button> — администратор</li>
            <li><button type="button" class="link" (click)="fill('manager@autoparts.local', 'Manager123!')">manager@autoparts.local</button> — менеджер</li>
            <li><button type="button" class="link" (click)="fill('customer@autoparts.local', 'Customer123!')">customer@autoparts.local</button> — покупатель</li>
          </ul>
        </details>
      </form>
    </div>
  `,
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly loading = signal(false);
  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  fill(email: string, password: string): void {
    this.form.setValue({ email, password });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/catalog';
        this.router.navigateByUrl(returnUrl);
      },
      error: (err) => {
        this.loading.set(false);
        applyServerErrors(this.form, err);
      },
    });
  }
}
