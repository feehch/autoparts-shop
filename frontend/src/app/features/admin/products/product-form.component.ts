import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CarModelDto, ProductRequest } from '../../../core/models/api.models';
import { CatalogService, categoryOptions } from '../../../core/services/catalog.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ProductService } from '../../../core/services/product.service';
import { FieldErrorComponent } from '../../../shared/components/field-error.component';
import { YearsPipe } from '../../../shared/pipes/years.pipe';
import { applyServerErrors } from '../../../shared/utils/form-errors';

interface MakeGroup {
  makeId: number;
  makeName: string;
  models: CarModelDto[];
}

@Component({
  selector: 'app-product-form',
  imports: [ReactiveFormsModule, FormsModule, RouterLink, FieldErrorComponent, YearsPipe],
  templateUrl: './product-form.component.html',
})
export class ProductFormComponent implements OnInit {
  /** :id маршрута; отсутствует для /admin/products/new. */
  readonly id = input<string>();

  private readonly fb = inject(FormBuilder);
  private readonly catalog = inject(CatalogService);
  private readonly products = inject(ProductService);
  private readonly notify = inject(NotificationService);
  private readonly router = inject(Router);

  protected readonly isEdit = computed(() => !!this.id());
  protected readonly saving = signal(false);
  protected readonly loading = signal(false);

  protected readonly brands = toSignal(this.catalog.getBrands(), { initialValue: [] });
  private readonly categoriesRaw = toSignal(this.catalog.getCategories(), { initialValue: [] });
  protected readonly categories = computed(() => categoryOptions(this.categoriesRaw()));
  private readonly allModels = toSignal(this.catalog.getAllModels(), { initialValue: [] as CarModelDto[] });

  /** Выбранные совместимые модели. */
  protected readonly selectedModels = signal<Set<number>>(new Set());
  protected modelFilter = signal('');

  protected readonly groups = computed<MakeGroup[]>(() => {
    const f = this.modelFilter().trim().toLowerCase();
    const map = new Map<number, MakeGroup>();
    for (const m of this.allModels()) {
      if (f && !`${m.makeName} ${m.name}`.toLowerCase().includes(f)) continue;
      let g = map.get(m.makeId);
      if (!g) {
        g = { makeId: m.makeId, makeName: m.makeName, models: [] };
        map.set(m.makeId, g);
      }
      g.models.push(m);
    }
    const list = [...map.values()].sort((a, b) => a.makeName.localeCompare(b.makeName, 'ru'));
    list.forEach((g) => g.models.sort((a, b) => a.name.localeCompare(b.name, 'ru')));
    return list;
  });

  protected readonly form = this.fb.group({
    sku: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(64)]),
    oemNumber: this.fb.nonNullable.control('', [Validators.maxLength(64)]),
    name: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(255)]),
    description: this.fb.nonNullable.control(''),
    price: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    stockQuantity: this.fb.control<number | null>(0, [Validators.required, Validators.min(0)]),
    imageUrl: this.fb.nonNullable.control('', [Validators.maxLength(500)]),
    brandId: this.fb.control<number | null>(null, [Validators.required]),
    categoryId: this.fb.control<number | null>(null, [Validators.required]),
    active: this.fb.nonNullable.control(true),
  });

  ngOnInit(): void {
    const id = this.id();
    if (!id) return;
    this.loading.set(true);
    this.products.get(Number(id)).subscribe({
      next: (p) => {
        this.form.setValue({
          sku: p.sku,
          oemNumber: p.oemNumber ?? '',
          name: p.name,
          description: p.description ?? '',
          price: p.price,
          stockQuantity: p.stockQuantity,
          imageUrl: p.imageUrl ?? '',
          brandId: p.brandId,
          categoryId: p.categoryId,
          active: p.active,
        });
        this.selectedModels.set(new Set(p.compatibleModels.map((m) => m.id)));
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.router.navigate(['/admin/products']);
      },
    });
  }

  protected toggleModel(id: number): void {
    const next = new Set(this.selectedModels());
    if (next.has(id)) next.delete(id);
    else next.add(id);
    this.selectedModels.set(next);
  }

  protected toggleMake(g: MakeGroup): void {
    const next = new Set(this.selectedModels());
    const allSelected = g.models.every((m) => next.has(m.id));
    g.models.forEach((m) => (allSelected ? next.delete(m.id) : next.add(m.id)));
    this.selectedModels.set(next);
  }

  protected isMakeSelected(g: MakeGroup): boolean {
    const s = this.selectedModels();
    return g.models.length > 0 && g.models.every((m) => s.has(m.id));
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const req: ProductRequest = {
      sku: v.sku.trim(),
      oemNumber: v.oemNumber.trim() || null,
      name: v.name.trim(),
      description: v.description.trim() || null,
      price: Number(v.price),
      stockQuantity: Number(v.stockQuantity),
      imageUrl: v.imageUrl.trim() || null,
      brandId: Number(v.brandId),
      categoryId: Number(v.categoryId),
      compatibleModelIds: [...this.selectedModels()],
      active: v.active,
    };
    this.saving.set(true);
    const id = this.id();
    const op = id ? this.products.update(Number(id), req) : this.products.create(req);
    op.subscribe({
      next: () => {
        this.notify.success(id ? 'Товар сохранён' : 'Товар создан');
        this.router.navigate(['/admin/products']);
      },
      error: (err) => {
        this.saving.set(false);
        applyServerErrors(this.form, err);
      },
    });
  }
}
