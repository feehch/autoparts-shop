package feehch.dev.autoparts.presentation.rest;

import feehch.dev.autoparts.application.catalog.CarMakeView;
import feehch.dev.autoparts.application.catalog.CarModelView;
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
@RequestMapping("/api/cars")
@Tag(name = "Автомобили")
public class CarController {

    private final CatalogService catalogService;

    public CarController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/makes")
    public List<CarMakeView> makes() {
        return catalogService.listMakes();
    }

    @PostMapping("/makes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    public CarMakeView createMake(@Valid @RequestBody CatalogRequests.CarMakeRequest request) {
        return catalogService.createMake(request.toCommand());
    }

    @PutMapping("/makes/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public CarMakeView updateMake(@PathVariable Long id, @Valid @RequestBody CatalogRequests.CarMakeRequest request) {
        return catalogService.updateMake(id, request.toCommand());
    }

    @DeleteMapping("/makes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('MANAGER')")
    public void deleteMake(@PathVariable Long id) {
        catalogService.deleteMake(id);
    }

    @GetMapping("/makes/{makeId}/models")
    public List<CarModelView> modelsByMake(@PathVariable Long makeId) {
        return catalogService.listModelsByMake(makeId);
    }

    @GetMapping("/models")
    public List<CarModelView> models() {
        return catalogService.listModels();
    }

    @PostMapping("/models")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    public CarModelView createModel(@Valid @RequestBody CatalogRequests.CarModelRequest request) {
        return catalogService.createModel(request.toCommand());
    }

    @PutMapping("/models/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public CarModelView updateModel(@PathVariable Long id,
                                    @Valid @RequestBody CatalogRequests.CarModelRequest request) {
        return catalogService.updateModel(id, request.toCommand());
    }

    @DeleteMapping("/models/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('MANAGER')")
    public void deleteModel(@PathVariable Long id) {
        catalogService.deleteModel(id);
    }
}
