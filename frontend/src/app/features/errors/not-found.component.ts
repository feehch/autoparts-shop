import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  imports: [RouterLink],
  template: `
    <div class="container error-page">
      <div class="error-code">404</div>
      <h1>Страница не найдена</h1>
      <p class="muted">Возможно, она была удалена или вы ошиблись в адресе.</p>
      <a routerLink="/catalog" class="btn btn-primary">Перейти в каталог</a>
    </div>
  `,
})
export class NotFoundComponent {}
