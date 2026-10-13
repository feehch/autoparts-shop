import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { BrandDto, BrandRequest } from '../../../core/models/api.models';
import { CatalogService } from '../../../core/services/catalog.service';
import { NotificationService } from '../../../core/services/notification.service';
import { FieldErrorComponent } from '../../../shared/components/field-error.component';
import { applyServerErrors } from '../../../shared/utils/form-errors';

@Component({
  selector: 'app-admin-brands',
  imports: [ReactiveFormsModule, FieldErrorComponent],
  template: `
    <div class="page-head">
      <h1>Бренды производителей</h1>
      @if (!formOpen()) {
        <button type="button" class="btn btn-primary" (click)="openCreate()">+ Новый бренд</button>
      }
    </div>

    @if (formOpen()) {
      <form class="card form-card wide" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <h3>{{ editing() ? 'Редактирование: ' + editing()!.name : 'Новый бренд' }}</h3>
        <div class="grid-2">
          <label class="field">
            <span>Название</span>
            <input formControlName="name" />
            <app-field-error [control]="form.controls.name" />
          </label>
          <label class="field">
            <span>Страна</span>
            <input formControlName="country" />
            <app-field-error [control]="form.controls.country" />
          </label>
        </div>
        <label class="field">
          <span>Описание</span>
          <textarea formControlName="description" rows="2"></textarea>
        </label>
        <div class="form-actions">
          <button type="button" class="btn btn-ghost" (click)="close()">Отмена</button>
          <button type="submit" class="btn btn-primary" [disabled]="saving()">Сохранить</button>
        </div>
      </form>
    }

    <div class="card table-wrap">
      <table class="table">
        <thead><tr><th>Название</th><th>Страна</th><th>Описание</th><th></th></tr></thead>
        <tbody>
          @for (b of brands(); track b.id) {
            <tr>
              <td class="strong">{{ b.name }}</td>
              <td>{{ b.country }}</td>
              <td class="muted small">{{ b.description }}</td>
              <td class="right nowrap">
                <button type="button" class="btn btn-ghost btn-xs" (click)="openEdit(b)">Изменить</button>
                <button type="button" class="btn btn-danger-ghost btn-xs" (click)="remove(b)">Удалить</button>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="4" class="muted center">Брендов пока нет</td></tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class AdminBrandsComponent {
  private readonly fb = inject(FormBuilder);
  private readonly catalog = inject(CatalogService);
  private readonly notify = inject(NotificationService);

  protected readonly brands = signal<BrandDto[]>([]);
  protected readonly editing = signal<BrandDto | null>(null);
  protected readonly formOpen = signal(false);
  protected readonly saving = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    country: ['', [Validators.maxLength(100)]],
    description: [''],
  });

  constructor() {
    this.reload();
  }

  reload(): void {
    this.catalog.getBrands().subscribe((list) => this.brands.set([...list].sort((a, b) => a.name.localeCompare(b.name))));
  }

  openCreate(): void {
    this.editing.set(null);
    this.form.reset({ name: '', country: '', description: '' });
    this.formOpen.set(true);
  }

  openEdit(b: BrandDto): void {
    this.editing.set(b);
    this.form.reset({ name: b.name, country: b.country ?? '', description: b.description ?? '' });
    this.formOpen.set(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  close(): void {
    this.formOpen.set(false);
    this.editing.set(null);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const req: BrandRequest = {
      name: v.name.trim(),
      country: v.country.trim() || null,
      description: v.description.trim() || null,
    };
    const current = this.editing();
    this.saving.set(true);
    (current ? this.catalog.updateBrand(current.id, req) : this.catalog.createBrand(req)).subscribe({
      next: () => {
        this.saving.set(false);
        this.notify.success(current ? 'Бренд сохранён' : 'Бренд создан');
        this.close();
        this.reload();
      },
      error: (err) => {
        this.saving.set(false);
        applyServerErrors(this.form, err);
      },
    });
  }

  remove(b: BrandDto): void {
    if (!confirm(`Удалить бренд «${b.name}»?`)) return;
    this.catalog.deleteBrand(b.id).subscribe(() => {
      this.notify.success('Бренд удалён');
      this.reload();
    });
  }
}
