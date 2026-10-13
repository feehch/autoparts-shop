import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-forbidden',
  imports: [RouterLink],
  template: `
    <div class="container error-page">
      <div class="error-code">403</div>
      <h1>Доступ запрещён</h1>
      <p class="muted">У вас недостаточно прав для просмотра этой страницы.</p>
      <a routerLink="/catalog" class="btn btn-primary">Вернуться в каталог</a>
    </div>
  `,
})
export class ForbiddenComponent {}
