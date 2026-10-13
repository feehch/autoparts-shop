package feehch.dev.autoparts.presentation.rest;

import feehch.dev.autoparts.application.cart.CartService;
import feehch.dev.autoparts.application.cart.CartView;
import feehch.dev.autoparts.application.security.AuthenticatedUser;
import feehch.dev.autoparts.presentation.dto.OrderRequests;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Корзина")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartView get(@AuthenticationPrincipal AuthenticatedUser user) {
        return cartService.getCart(user.id());
    }

    @PostMapping("/items")
    public CartView add(@AuthenticationPrincipal AuthenticatedUser user,
                        @Valid @RequestBody OrderRequests.AddCartItem request) {
        return cartService.addItem(user.id(), request.productId(), request.quantity());
    }

    @PutMapping("/items/{productId}")
    public CartView update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long productId,
                           @Valid @RequestBody OrderRequests.UpdateCartItem request) {
        return cartService.updateItem(user.id(), productId, request.quantity());
    }

    @DeleteMapping("/items/{productId}")
    public CartView remove(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long productId) {
        return cartService.removeItem(user.id(), productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@AuthenticationPrincipal AuthenticatedUser user) {
        cartService.clear(user.id());
    }
}
