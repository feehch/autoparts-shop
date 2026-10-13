package feehch.dev.autoparts.presentation.rest;

import feehch.dev.autoparts.application.catalog.CatalogService;
import feehch.dev.autoparts.application.catalog.CategoryView;
import feehch.dev.autoparts.presentation.dto.CatalogRequests;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Категории")
public class CategoryController {

    private final CatalogService catalogService;

    public CategoryController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<CategoryView> list() {
        return catalogService.listCategories();
    }

    @GetMapping("/{id}")
    public CategoryView get(@PathVariable Long id) {
        return catalogService.getCategory(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    public CategoryView create(@Valid @RequestBody CatalogRequests.CategoryRequest request) {
        return catalogService.createCategory(request.toCommand());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public CategoryView update(@PathVariable Long id, @Valid @RequestBody CatalogRequests.CategoryRequest request) {
        return catalogService.updateCategory(id, request.toCommand());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('MANAGER')")
    public void delete(@PathVariable Long id) {
        catalogService.deleteCategory(id);
    }
}
