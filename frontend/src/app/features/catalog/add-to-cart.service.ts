import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';
import { NotificationService } from '../../core/services/notification.service';

/** Добавление в корзину с проверкой аутентификации и уведомлением. */
@Injectable({ providedIn: 'root' })
export class AddToCartService {
  private readonly auth = inject(AuthService);
  private readonly cart = inject(CartService);
  private readonly notify = inject(NotificationService);
  private readonly router = inject(Router);

  add(productId: number, name: string, quantity: number, done?: () => void): void {
    if (!this.auth.isAuthenticated()) {
      this.notify.info('Войдите, чтобы добавлять товары в корзину');
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      done?.();
      return;
    }
    this.cart.add(productId, quantity).subscribe({
      next: () => {
        this.notify.success(`«${name}» добавлен в корзину`);
        done?.();
      },
      error: () => done?.(),
    });
  }
}
