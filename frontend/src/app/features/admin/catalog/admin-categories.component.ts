import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CategoryDto, CategoryRequest } from '../../../core/models/api.models';
import { CatalogService, categoryOptions } from '../../../core/services/catalog.service';
import { NotificationService } from '../../../core/services/notification.service';
import { FieldErrorComponent } from '../../../shared/components/field-error.component';
import { applyServerErrors } from '../../../shared/utils/form-errors';

@Component({
  selector: 'app-admin-categories',
  imports: [ReactiveFormsModule, FieldErrorComponent],
  template: `
    <div class="page-head">
      <h1>Категории</h1>
      @if (!formOpen()) {
        <button type="button" class="btn btn-primary" (click)="openCreate()">+ Новая категория</button>
      }
    </div>

    @if (formOpen()) {
      <form class="card form-card wide" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <h3>{{ editing() ? 'Редактирование: ' + editing()!.name : 'Новая категория' }}</h3>
        <div class="grid-2">
          <label class="field">
            <span>Название</span>
            <input formControlName="name" />
            <app-field-error [control]="form.controls.name" />
          </label>
          <label class="field">
            <span>Slug <span class="muted">(необязательно — сгенерируется)</span></span>
            <input formControlName="slug" placeholder="brake-pads" />
            <app-field-error [control]="form.controls.slug" />
          </label>
        </div>
        <label class="field">
          <span>Родительская категория</span>
          <select formControlName="parentId">
            <option [ngValue]="null">— корневая категория —</option>
            @for (c of parentOptions(); track c.id) {
              <option [ngValue]="c.id">{{ c.label }}</option>
            }
          </select>
        </label>
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
        <thead><tr><th>Название</th><th>Slug</th><th>Описание</th><th></th></tr></thead>
        <tbody>
          @for (c of rows(); track c.id) {
            <tr>
              <td [style.padding-left.px]="12 + c.depth * 20">
                @if (c.depth) {<span class="muted">└ </span>}{{ c.name }}
              </td>
              <td class="mono small">{{ c.slug }}</td>
              <td class="muted small">{{ c.description }}</td>
              <td class="right nowrap">
                <button type="button" class="btn btn-ghost btn-xs" (click)="openEdit(c)">Изменить</button>
                <button type="button" class="btn btn-danger-ghost btn-xs" (click)="remove(c)">Удалить</button>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="4" class="muted center">Категорий пока нет</td></tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class AdminCategoriesComponent {
  private readonly fb = inject(FormBuilder);
  private readonly catalog = inject(CatalogService);
  private readonly notify = inject(NotificationService);

  protected readonly categories = signal<CategoryDto[]>([]);
  protected readonly editing = signal<CategoryDto | null>(null);
  protected readonly formOpen = signal(false);
  protected readonly saving = signal(false);

  protected readonly rows = computed(() => {
    const byId = new Map(this.categories().map((c) => [c.id, c]));
    return categoryOptions(this.categories()).map((o) => ({ ...byId.get(o.id)!, depth: o.depth }));
  });

  /** Родителем нельзя выбрать саму категорию и её потомков. */
  protected readonly parentOptions = computed(() => {
    const current = this.editing();
    const all = categoryOptions(this.categories());
    if (!current) return all;
    const excluded = new Set<number>([current.id]);
    let changed = true;
    while (changed) {
      changed = false;
      for (const c of this.categories()) {
        if (c.parentId != null && excluded.has(c.parentId) && !excluded.has(c.id)) {
          excluded.add(c.id);
          changed = true;
        }
      }
    }
    return all.filter((o) => !excluded.has(o.id));
  });

  protected readonly form = this.fb.group({
    name: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(100)]),
    slug: this.fb.nonNullable.control('', [Validators.maxLength(100), Validators.pattern(/^[a-z0-9-]*$/)]),
    description: this.fb.nonNullable.control(''),
    parentId: this.fb.control<number | null>(null),
  });

  constructor() {
    this.reload();
  }

  reload(): void {
    this.catalog.getCategories().subscribe((list) => this.categories.set(list));
  }

  openCreate(): void {
    this.editing.set(null);
    this.form.reset({ name: '', slug: '', description: '', parentId: null });
    this.formOpen.set(true);
  }

  openEdit(c: CategoryDto): void {
    this.editing.set(c);
    this.form.reset({ name: c.name, slug: c.slug ?? '', description: c.description ?? '', parentId: c.parentId });
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
    const req: CategoryRequest = {
      name: v.name.trim(),
      slug: v.slug.trim() || null,
      description: v.description.trim() || null,
      parentId: v.parentId,
    };
    const current = this.editing();
    this.saving.set(true);
    (current ? this.catalog.updateCategory(current.id, req) : this.catalog.createCategory(req)).subscribe({
      next: () => {
        this.saving.set(false);
        this.notify.success(current ? 'Категория сохранена' : 'Категория создана');
        this.close();
        this.reload();
      },
      error: (err) => {
        this.saving.set(false);
        applyServerErrors(this.form, err);
      },
    });
  }

  remove(c: CategoryDto): void {
    if (!confirm(`Удалить категорию «${c.name}»?`)) return;
    this.catalog.deleteCategory(c.id).subscribe(() => {
      this.notify.success('Категория удалена');
      this.reload();
    });
  }
}
