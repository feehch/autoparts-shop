import { Pipe, PipeTransform } from '@angular/core';
import { OrderStatus, PaymentMethod, RoleName } from '../../core/models/api.models';

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  NEW: 'Новый',
  CONFIRMED: 'Подтверждён',
  SHIPPED: 'Отправлен',
  DELIVERED: 'Доставлен',
  CANCELLED: 'Отменён',
};

export const ROLE_LABELS: Record<RoleName, string> = {
  ADMIN: 'Администратор',
  MANAGER: 'Менеджер',
  CUSTOMER: 'Покупатель',
};

export const PAYMENT_LABELS: Record<PaymentMethod, string> = {
  CASH_ON_DELIVERY: 'Наличными при получении',
  CARD_ON_DELIVERY: 'Картой при получении',
  CARD_ONLINE: 'Картой онлайн',
};

@Pipe({ name: 'orderStatus' })
export class OrderStatusPipe implements PipeTransform {
  transform(value: OrderStatus | null | undefined): string {
    return value ? (ORDER_STATUS_LABELS[value] ?? value) : '';
  }
}

@Pipe({ name: 'roleName' })
export class RoleNamePipe implements PipeTransform {
  transform(value: RoleName | string | null | undefined): string {
    return value ? (ROLE_LABELS[value as RoleName] ?? value) : '';
  }
}

@Pipe({ name: 'paymentMethod' })
export class PaymentMethodPipe implements PipeTransform {
  transform(value: PaymentMethod | null | undefined): string {
    return value ? (PAYMENT_LABELS[value] ?? value) : '';
  }
}
