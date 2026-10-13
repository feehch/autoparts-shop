package feehch.dev.autoparts.presentation.rest;

import feehch.dev.autoparts.application.order.OrderService;
import feehch.dev.autoparts.application.order.OrderView;
import feehch.dev.autoparts.application.security.AuthenticatedUser;
import feehch.dev.autoparts.domain.common.Page;
import feehch.dev.autoparts.domain.common.PageQuery;
import feehch.dev.autoparts.domain.order.OrderStatus;
import feehch.dev.autoparts.presentation.dto.OrderRequests;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Заказы")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Оформить заказ из корзины")
    public OrderView place(@AuthenticationPrincipal AuthenticatedUser user,
                           @Valid @RequestBody OrderRequests.PlaceOrder request) {
        return orderService.placeOrder(user.id(), request.toCommand());
    }

    @GetMapping("/my")
    public Page<OrderView> myOrders(@AuthenticationPrincipal AuthenticatedUser user,
                                    @RequestParam(required = false) Integer page,
                                    @RequestParam(required = false) Integer size) {
        return orderService.myOrders(user.id(), PageQuery.of(page, size));
    }

    @GetMapping("/{id}")
    public OrderView get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return orderService.get(user, id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Отмена нового заказа покупателем")
    public OrderView cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return orderService.cancelByCustomer(user, id);
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Все заказы (для менеджера)")
    public Page<OrderView> search(@RequestParam(required = false) OrderStatus status,
                                  @RequestParam(required = false) String q,
                                  @RequestParam(required = false) Integer page,
                                  @RequestParam(required = false) Integer size) {
        return orderService.search(status, q, PageQuery.of(page, size));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Смена статуса заказа")
    public OrderView changeStatus(@PathVariable Long id, @Valid @RequestBody OrderRequests.ChangeStatus request) {
        return orderService.changeStatus(id, request.status());
    }
}
