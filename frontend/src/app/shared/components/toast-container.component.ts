import { Component, inject } from '@angular/core';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-toast-container',
  template: `
    <div class="toasts" aria-live="polite">
      @for (t of notify.toasts(); track t.id) {
        <div class="toast toast-{{ t.type }}" role="alert">
          <div class="toast-body">
            <div>{{ t.text }}</div>
            @if (t.details?.length) {
              <ul class="toast-details">
                @for (d of t.details; track $index) {
                  <li>{{ d }}</li>
                }
              </ul>
            }
          </div>
          <button type="button" class="toast-close" aria-label="Закрыть" (click)="notify.dismiss(t.id)">×</button>
        </div>
      }
    </div>
  `,
})
export class ToastContainerComponent {
  protected readonly notify = inject(NotificationService);
}
