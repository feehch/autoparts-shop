import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CarMakeDto, CarModelDto, CarModelRequest } from '../../../core/models/api.models';
import { CatalogService } from '../../../core/services/catalog.service';
import { NotificationService } from '../../../core/services/notification.service';
import { FieldErrorComponent } from '../../../shared/components/field-error.component';
import { YearsPipe } from '../../../shared/pipes/years.pipe';
import { applyServerErrors } from '../../../shared/utils/form-errors';

const CURRENT_YEAR = new Date().getFullYear();

@Component({
  selector: 'app-admin-cars',
  imports: [ReactiveFormsModule, FieldErrorComponent, YearsPipe],
  template: `
    <h1>Автомобили</h1>
    <div class="cars-layout">
      <!-- Марки -->
      <section class="card section">
        <div class="section-head">
          <h3>Марки</h3>
          @if (!makeFormOpen()) {
            <button type="button" class="btn btn-primary btn-sm" (click)="openMakeCreate()">+ Марка</button>
          }
        </div>
        @if (makeFormOpen()) {
          <form class="subform" [formGroup]="makeForm" (ngSubmit)="saveMake()" novalidate>
            <label class="field">
              <span>Название</span>
              <input formControlName="name" />
              <app-field-error [control]="makeForm.controls.name" />
            </label>
            <label class="field">
              <span>Страна</span>
              <input formControlName="country" />
            </label>
            <div class="form-actions">
              <button type="button" class="btn btn-ghost btn-sm" (click)="makeFormOpen.set(false)">Отмена</button>
              <button type="submit" class="btn btn-primary btn-sm">{{ editingMake() ? 'Сохранить' : 'Создать' }}</button>
            </div>
          </form>
        }
        <ul class="list-select">
          @for (m of makes(); track m.id) {
            <li [class.active]="selectedMake()?.id === m.id">
              <button type="button" class="list-item-main" (click)="selectMake(m)">
                <span class="strong">{{ m.name }}</span>
                <span class="muted small">{{ m.country }}</span>
              </button>
              <button type="button" class="icon-btn" title="Изменить" (click)="openMakeEdit(m)">✎</button>
              <button type="button" class="icon-btn danger" title="Удалить" (click)="removeMake(m)">×</button>
            </li>
          } @empty {
            <li class="muted small">Марок пока нет</li>
          }
        </ul>
      </section>

      <!-- Модели -->
      <section class="card section">
        <div class="section-head">
          <h3>Модели {{ selectedMake() ? '— ' + selectedMake()!.name : '' }}</h3>
          @if (selectedMake() && !modelFormOpen()) {
            <button type="button" class="btn btn-primary btn-sm" (click)="openModelCreate()">+ Модель</button>
          }
        </div>
        @if (!selectedMake()) {
          <p class="muted">Выберите марку слева, чтобы увидеть её модели.</p>
        } @else {
          @if (modelFormOpen()) {
            <form class="subform" [formGroup]="modelForm" (ngSubmit)="saveModel()" novalidate>
              <div class="grid-3">
                <label class="field">
                  <span>Название модели</span>
                  <input formControlName="name" placeholder="Camry XV70" />
                  <app-field-error [control]="modelForm.controls.name" />
                </label>
                <label class="field">
                  <span>Год начала выпуска</span>
                  <input type="number" formControlName="yearFrom" />
                  <app-field-error [control]="modelForm.controls.yearFrom" />
                </label>
                <label class="field">
                  <span>Год окончания <span class="muted">(пусто — н.в.)</span></span>
                  <input type="number" formControlName="yearTo" />
                  <app-field-error [control]="modelForm.controls.yearTo" />
                </label>
              </div>
              <div class="form-actions">
                <button type="button" class="btn btn-ghost btn-sm" (click)="modelFormOpen.set(false)">Отмена</button>
                <button type="submit" class="btn btn-primary btn-sm">{{ editingModel() ? 'Сохранить' : 'Создать' }}</button>
              </div>
            </form>
          }
          <div class="table-wrap">
            <table class="table">
              <thead><tr><th>Модель</th><th>Годы выпуска</th><th></th></tr></thead>
              <tbody>
                @for (m of models(); track m.id) {
                  <tr>
                    <td class="strong">{{ m.name }}</td>
                    <td>{{ m | years }}</td>
                    <td class="right nowrap">
                      <button type="button" class="btn btn-ghost btn-xs" (click)="openModelEdit(m)">Изменить</button>
                      <button type="button" class="btn btn-danger-ghost btn-xs" (click)="removeModel(m)">Удалить</button>
                    </td>
                  </tr>
                } @empty {
                  <tr><td colspan="3" class="muted center">Моделей пока нет</td></tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>
    </div>
  `,
})
export class AdminCarsComponent {
  private readonly fb = inject(FormBuilder);
  private readonly catalog = inject(CatalogService);
  private readonly notify = inject(NotificationService);

  protected readonly makes = signal<CarMakeDto[]>([]);
  protected readonly selectedMakeId = signal<number | null>(null);
  protected readonly selectedMake = computed(() => this.makes().find((m) => m.id === this.selectedMakeId()) ?? null);
  protected readonly models = signal<CarModelDto[]>([]);

  protected readonly makeFormOpen = signal(false);
  protected readonly editingMake = signal<CarMakeDto | null>(null);
  protected readonly modelFormOpen = signal(false);
  protected readonly editingModel = signal<CarModelDto | null>(null);

  protected readonly makeForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    country: ['', [Validators.maxLength(100)]],
  });

  protected readonly modelForm = this.fb.group({
    name: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(100)]),
    yearFrom: this.fb.control<number | null>(null, [Validators.min(1900), Validators.max(CURRENT_YEAR + 1)]),
    yearTo: this.fb.control<number | null>(null, [Validators.min(1900), Validators.max(CURRENT_YEAR + 1)]),
  });

  constructor() {
    this.reloadMakes();
  }

  reloadMakes(): void {
    this.catalog.getMakes().subscribe((list) => {
      this.makes.set([...list].sort((a, b) => a.name.localeCompare(b.name)));
      if (!this.selectedMakeId() && list.length) this.selectMake(this.makes()[0]);
    });
  }

  selectMake(m: CarMakeDto): void {
    this.selectedMakeId.set(m.id);
    this.modelFormOpen.set(false);
    this.reloadModels();
  }

  reloadModels(): void {
    const id = this.selectedMakeId();
    if (!id) {
      this.models.set([]);
      return;
    }
    this.catalog.getModelsByMake(id).subscribe((list) => this.models.set([...list].sort((a, b) => a.name.localeCompare(b.name))));
  }

  // ----- Марки -----
  openMakeCreate(): void {
    this.editingMake.set(null);
    this.makeForm.reset({ name: '', country: '' });
    this.makeFormOpen.set(true);
  }

  openMakeEdit(m: CarMakeDto): void {
    this.editingMake.set(m);
    this.makeForm.reset({ name: m.name, country: m.country ?? '' });
    this.makeFormOpen.set(true);
  }

  saveMake(): void {
    if (this.makeForm.invalid) {
      this.makeForm.markAllAsTouched();
      return;
    }
    const v = this.makeForm.getRawValue();
    const req = { name: v.name.trim(), country: v.country.trim() || null };
    const current = this.editingMake();
    (current ? this.catalog.updateMake(current.id, req) : this.catalog.createMake(req)).subscribe({
      next: (saved) => {
        this.notify.success(current ? 'Марка сохранена' : 'Марка создана');
        this.makeFormOpen.set(false);
        this.selectedMakeId.set(saved.id);
        this.reloadMakes();
        this.reloadModels();
      },
      error: (err) => applyServerErrors(this.makeForm, err),
    });
  }

  removeMake(m: CarMakeDto): void {
    if (!confirm(`Удалить марку «${m.name}»?`)) return;
    this.catalog.deleteMake(m.id).subscribe(() => {
      this.notify.success('Марка удалена');
      if (this.selectedMakeId() === m.id) {
        this.selectedMakeId.set(null);
        this.models.set([]);
      }
      this.reloadMakes();
    });
  }

  // ----- Модели -----
  openModelCreate(): void {
    this.editingModel.set(null);
    this.modelForm.reset({ name: '', yearFrom: null, yearTo: null });
    this.modelFormOpen.set(true);
  }

  openModelEdit(m: CarModelDto): void {
    this.editingModel.set(m);
    this.modelForm.reset({ name: m.name, yearFrom: m.yearFrom, yearTo: m.yearTo });
    this.modelFormOpen.set(true);
  }

  saveModel(): void {
    const makeId = this.selectedMakeId();
    if (this.modelForm.invalid || !makeId) {
      this.modelForm.markAllAsTouched();
      return;
    }
    const v = this.modelForm.getRawValue();
    if (v.yearFrom != null && v.yearTo != null && v.yearTo < v.yearFrom) {
      this.modelForm.controls.yearTo.setErrors({ server: 'Год окончания меньше года начала' });
      this.modelForm.controls.yearTo.markAsTouched();
      return;
    }
    const req: CarModelRequest = { makeId, name: v.name.trim(), yearFrom: v.yearFrom ?? null, yearTo: v.yearTo ?? null };
    const current = this.editingModel();
    (current ? this.catalog.updateModel(current.id, req) : this.catalog.createModel(req)).subscribe({
      next: () => {
        this.notify.success(current ? 'Модель сохранена' : 'Модель создана');
        this.modelFormOpen.set(false);
        this.reloadModels();
      },
      error: (err) => applyServerErrors(this.modelForm, err),
    });
  }

  removeModel(m: CarModelDto): void {
    if (!confirm(`Удалить модель «${m.makeName} ${m.name}»?`)) return;
    this.catalog.deleteModel(m.id).subscribe(() => {
      this.notify.success('Модель удалена');
      this.reloadModels();
    });
  }
}
