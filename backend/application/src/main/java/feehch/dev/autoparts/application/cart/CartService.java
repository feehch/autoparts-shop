package feehch.dev.autoparts.application.cart;

import feehch.dev.autoparts.application.port.out.CartRepository;
import feehch.dev.autoparts.application.port.out.ProductRepository;
import feehch.dev.autoparts.application.port.out.UnitOfWork;
import feehch.dev.autoparts.domain.cart.Cart;
import feehch.dev.autoparts.domain.cart.CartItem;
import feehch.dev.autoparts.domain.catalog.Product;
import feehch.dev.autoparts.domain.common.NotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сценарии работы с корзиной покупателя.
 */
public class CartService {

    private final CartRepository carts;
    private final ProductRepository products;
    private final UnitOfWork unitOfWork;

    public CartService(CartRepository carts, ProductRepository products, UnitOfWork unitOfWork) {
        this.carts = carts;
        this.products = products;
        this.unitOfWork = unitOfWork;
    }

    public CartView getCart(Long userId) {
        return toView(carts.findByUserId(userId));
    }

    public CartView addItem(Long userId, Long productId, int quantity) {
        return unitOfWork.inTransaction(() -> {
            Cart cart = carts.findByUserId(userId);
            Product product = loadProduct(productId);
            product.ensureCanBeOrdered(cart.quantityOf(productId) + quantity);
            cart.add(productId, quantity);
            carts.save(cart);
            return toView(cart);
        });
    }

    public CartView updateItem(Long userId, Long productId, int quantity) {
        return unitOfWork.inTransaction(() -> {
            Cart cart = carts.findByUserId(userId);
            if (!cart.contains(productId)) {
                throw new NotFoundException("Товара с идентификатором " + productId + " нет в корзине");
            }
            loadProduct(productId).ensureCanBeOrdered(quantity);
            cart.setQuantity(productId, quantity);
            carts.save(cart);
            return toView(cart);
        });
    }

    public CartView removeItem(Long userId, Long productId) {
        return unitOfWork.inTransaction(() -> {
            Cart cart = carts.findByUserId(userId);
            cart.remove(productId);
            carts.save(cart);
            return toView(cart);
        });
    }

    public void clear(Long userId) {
        unitOfWork.inTransaction(() -> {
            Cart cart = carts.findByUserId(userId);
            cart.clear();
            carts.save(cart);
        });
    }

    private Product loadProduct(Long productId) {
        return products.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> NotFoundException.of("Товар", productId));
    }

    private CartView toView(Cart cart) {
        List<CartItem> items = cart.getItems();
        Map<Long, Product> productById = products.findAllById(items.stream().map(CartItem::productId).toList())
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        List<CartItemView> views = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int totalQuantity = 0;
        for (CartItem item : items) {
            Product p = productById.get(item.productId());
            if (p == null) {
                continue;
            }
            BigDecimal lineTotal = p.getPrice().multiply(BigDecimal.valueOf(item.quantity()));
            boolean available = p.isAvailable(item.quantity());
            views.add(new CartItemView(p.getId(), p.getSku(), p.getName(), p.getImageUrl(), p.getPrice(),
                    item.quantity(), lineTotal, p.getStockQuantity(), available));
            total = total.add(lineTotal);
            totalQuantity += item.quantity();
        }
        return new CartView(views, totalQuantity, total.setScale(2, RoundingMode.HALF_UP));
    }
}
