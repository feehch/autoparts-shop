package feehch.dev.autoparts.presentation.rest;

import feehch.dev.autoparts.application.catalog.BrandView;
import feehch.dev.autoparts.application.catalog.CatalogService;
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
@RequestMapping("/api/brands")
@Tag(name = "Бренды")
public class BrandController {

    private final CatalogService catalogService;

    public BrandController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<BrandView> list() {
        return catalogService.listBrands();
    }

    @GetMapping("/{id}")
    public BrandView get(@PathVariable Long id) {
        return catalogService.getBrand(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    public BrandView create(@Valid @RequestBody CatalogRequests.BrandRequest request) {
        return catalogService.createBrand(request.toCommand());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public BrandView update(@PathVariable Long id, @Valid @RequestBody CatalogRequests.BrandRequest request) {
        return catalogService.updateBrand(id, request.toCommand());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('MANAGER')")
    public void delete(@PathVariable Long id) {
        catalogService.deleteBrand(id);
    }
}
