export type RoleName = 'ADMIN' | 'MANAGER' | 'CUSTOMER';
export type OrderStatus = 'NEW' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
export type PaymentMethod = 'CASH_ON_DELIVERY' | 'CARD_ON_DELIVERY' | 'CARD_ONLINE';

export const ORDER_STATUSES: OrderStatus[] = ['NEW', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'];
export const PAYMENT_METHODS: PaymentMethod[] = ['CASH_ON_DELIVERY', 'CARD_ON_DELIVERY', 'CARD_ONLINE'];

/** Допустимые переходы статусов заказа. */
export const ORDER_TRANSITIONS: Record<OrderStatus, OrderStatus[]> = {
  NEW: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['SHIPPED', 'CANCELLED'],
  SHIPPED: ['DELIVERED'],
  DELIVERED: [],
  CANCELLED: [],
};

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface FieldError {
  field: string;
  message: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: FieldError[];
}

// ---------- Пользователи / аутентификация ----------
export interface UserDto {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  roles: RoleName[];
  enabled: boolean;
  createdAt: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string | null;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserDto;
}

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  phone: string | null;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface RoleDto {
  name: RoleName;
  description: string;
}

export interface CreateUserRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  roles: RoleName[];
}

// ---------- Каталог ----------
export interface CategoryDto {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  parentId: number | null;
}

export interface CategoryRequest {
  name: string;
  slug?: string | null;
  description?: string | null;
  parentId: number | null;
}

/** Узел дерева категорий (строится на клиенте). */
export interface CategoryNode extends CategoryDto {
  children: CategoryNode[];
  depth: number;
}

export interface BrandDto {
  id: number;
  name: string;
  country: string | null;
  description: string | null;
}

export type BrandRequest = Omit<BrandDto, 'id'>;

export interface CarMakeDto {
  id: number;
  name: string;
  country: string | null;
}

export type CarMakeRequest = Omit<CarMakeDto, 'id'>;

export interface CarModelDto {
  id: number;
  makeId: number;
  makeName: string;
  name: string;
  yearFrom: number | null;
  yearTo: number | null;
}

export interface CarModelRequest {
  makeId: number;
  name: string;
  yearFrom: number | null;
  yearTo: number | null;
}

export interface ProductSummaryDto {
  id: number;
  sku: string;
  oemNumber: string | null;
  name: string;
  price: number;
  stockQuantity: number;
  imageUrl: string | null;
  active: boolean;
  brandId: number;
  brandName: string;
  categoryId: number;
  categoryName: string;
}

export interface ProductDto extends ProductSummaryDto {
  description: string | null;
  compatibleModels: CarModelDto[];
  createdAt: string;
  updatedAt: string;
}

export interface ProductRequest {
  sku: string;
  oemNumber: string | null;
  name: string;
  description: string | null;
  price: number;
  stockQuantity: number;
  imageUrl: string | null;
  brandId: number;
  categoryId: number;
  compatibleModelIds: number[];
  active: boolean;
}

export interface ProductSearchParams {
  q?: string | null;
  categoryId?: number | null;
  brandId?: number | null;
  makeId?: number | null;
  modelId?: number | null;
  minPrice?: number | null;
  maxPrice?: number | null;
  inStock?: boolean | null;
  includeInactive?: boolean | null;
  sort?: string | null;
  page?: number | null;
  size?: number | null;
}

// ---------- Корзина ----------
export interface CartItemDto {
  productId: number;
  sku: string;
  name: string;
  imageUrl: string | null;
  price: number;
  quantity: number;
  lineTotal: number;
  stockQuantity: number;
  available: boolean;
}

export interface CartDto {
  items: CartItemDto[];
  totalQuantity: number;
  totalAmount: number;
}

// ---------- Заказы ----------
export interface OrderItemDto {
  productId: number;
  sku: string;
  name: string;
  price: number;
  quantity: number;
  lineTotal: number;
}

export interface OrderDto {
  id: number;
  number: string;
  userId: number;
  customerEmail: string;
  customerName: string;
  status: OrderStatus;
  items: OrderItemDto[];
  totalAmount: number;
  deliveryAddress: string;
  contactPhone: string;
  comment: string | null;
  paymentMethod: PaymentMethod;
  createdAt: string;
  updatedAt: string;
}

export interface PlaceOrderRequest {
  deliveryAddress: string;
  contactPhone: string;
  comment: string | null;
  paymentMethod: PaymentMethod;
}

// ---------- Статистика ----------
export interface DashboardStats {
  productsCount: number;
  activeProductsCount: number;
  lowStockCount: number;
  ordersCount: number;
  ordersByStatus: Partial<Record<OrderStatus, number>>;
  revenue: number;
  customersCount: number;
}
