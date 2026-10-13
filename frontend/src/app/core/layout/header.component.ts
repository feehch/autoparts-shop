import { Component, ElementRef, HostListener, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../services/auth.service';
import { CartService } from '../services/cart.service';

@Component({
  selector: 'app-header',
  imports: [RouterLink, RouterLinkActive, FormsModule],
  templateUrl: './header.component.html',
})
export class HeaderComponent {
  protected readonly auth = inject(AuthService);
  protected readonly cart = inject(CartService);
  private readonly router = inject(Router);
  private readonly host = inject(ElementRef<HTMLElement>);

  protected query = '';
  protected readonly menuOpen = signal(false);

  constructor() {
    // Синхронизация строки поиска с параметром q и закрытие меню при навигации
    this.router.events
      .pipe(
        filter((e) => e instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => {
        this.menuOpen.set(false);
        const q = this.router.parseUrl(this.router.url).queryParams['q'];
        this.query = typeof q === 'string' ? q : '';
      });
  }

  search(): void {
    const q = this.query.trim();
    this.router.navigate(['/catalog'], { queryParams: q ? { q } : {} });
  }

  toggleMenu(): void {
    this.menuOpen.update((v) => !v);
  }

  logout(): void {
    this.menuOpen.set(false);
    this.auth.logout();
  }

  @HostListener('document:click', ['$event'])
  onDocClick(e: MouseEvent): void {
    if (this.menuOpen() && !this.host.nativeElement.querySelector('.user-menu')?.contains(e.target as Node)) {
      this.menuOpen.set(false);
    }
  }
}
