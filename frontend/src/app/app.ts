import { Component, effect, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './core/layout/header.component';
import { AuthService } from './core/services/auth.service';
import { CartService } from './core/services/cart.service';
import { ToastContainerComponent } from './shared/components/toast-container.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, HeaderComponent, ToastContainerComponent],
  template: `
    <app-header />
    <main class="site-main">
      <router-outlet />
    </main>
    <footer class="site-footer">
      <div class="container footer-inner">
        <span>© {{ year }} АвтоДеталь — интернет-магазин автозапчастей для легковых автомобилей</span>
        <a href="/swagger-ui.html" target="_blank" rel="noopener">API</a>
      </div>
    </footer>
    <app-toast-container />
  `,
})
export class App {
  private readonly auth = inject(AuthService);
  private readonly cart = inject(CartService);
  protected readonly year = new Date().getFullYear();

  constructor() {
    // Счётчик корзины обновляется после входа и сбрасывается после выхода
    effect(() => {
      if (this.auth.isAuthenticated()) {
        this.cart.refresh();
      } else {
        this.cart.reset();
      }
    });
  }
}
