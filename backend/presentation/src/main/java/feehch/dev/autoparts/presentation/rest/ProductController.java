package feehch.dev.autoparts.presentation.rest;

import feehch.dev.autoparts.application.catalog.ProductSearchQuery;
import feehch.dev.autoparts.application.catalog.ProductService;
import feehch.dev.autoparts.application.catalog.ProductSummaryView;
import feehch.dev.autoparts.application.catalog.ProductView;
import feehch.dev.autoparts.application.security.AuthenticatedUser;
import feehch.dev.autoparts.domain.common.Page;
import feehch.dev.autoparts.domain.common.PageQuery;
import feehch.dev.autoparts.domain.user.Role;
import feehch.dev.autoparts.presentation.dto.CatalogRequests;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Товары")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Поиск и фильтрация товаров каталога")
    public Page<ProductSummaryView> search(@AuthenticationPrincipal AuthenticatedUser user,
                                           @RequestParam(required = false) String q,
                                           @RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) Long brandId,
                                           @RequestParam(required = false) Long makeId,
                                           @RequestParam(required = false) Long modelId,
                                           @RequestParam(required = false) BigDecimal minPrice,
                                           @RequestParam(required = false) BigDecimal maxPrice,
                                           @RequestParam(defaultValue = "false") boolean inStock,
                                           @RequestParam(defaultValue = "false") boolean includeInactive,
                                           @RequestParam(required = false) String sort,
                                           @RequestParam(required = false) Integer page,
                                           @RequestParam(required = false) Integer size) {
        ProductSearchQuery query = new ProductSearchQuery(q, categoryId, brandId, makeId, modelId, minPrice,
                maxPrice, inStock, includeInactive, sort);
        return productService.search(query, PageQuery.of(page, size), isStaff(user));
    }

    @GetMapping("/{id}")
    public ProductView get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return productService.get(id, isStaff(user));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    public ProductView create(@Valid @RequestBody CatalogRequests.ProductRequest request) {
        return productService.create(request.toCommand());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ProductView update(@PathVariable Long id, @Valid @RequestBody CatalogRequests.ProductRequest request) {
        return productService.update(id, request.toCommand());
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('MANAGER')")
    public ProductView changeStock(@PathVariable Long id, @Valid @RequestBody CatalogRequests.StockRequest request) {
        return productService.changeStock(id, request.stockQuantity());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Деактивация товара (мягкое удаление)")
    public void delete(@PathVariable Long id) {
        productService.deactivate(id);
    }

    private static boolean isStaff(AuthenticatedUser user) {
        return user != null && user.hasRole(Role.MANAGER);
    }
}
