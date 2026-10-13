package feehch.dev.autoparts.presentation.dto;

import feehch.dev.autoparts.application.catalog.BrandCommand;
import feehch.dev.autoparts.application.catalog.CarMakeCommand;
import feehch.dev.autoparts.application.catalog.CarModelCommand;
import feehch.dev.autoparts.application.catalog.CategoryCommand;
import feehch.dev.autoparts.application.catalog.ProductCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Запросы управления каталогом.
 */
public final class CatalogRequests {

    private CatalogRequests() {
    }

    public record CategoryRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 150) @Pattern(regexp = "^[a-zA-Z0-9-]*$", message = "допустимы латиница, цифры и дефис") String slug,
            @Size(max = 2000) String description,
            Long parentId) {

        public CategoryCommand toCommand() {
            return new CategoryCommand(name, slug, description, parentId);
        }
    }

    public record BrandRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 100) String country,
            @Size(max = 2000) String description) {

        public BrandCommand toCommand() {
            return new BrandCommand(name, country, description);
        }
    }

    public record CarMakeRequest(@NotBlank @Size(max = 100) String name, @Size(max = 100) String country) {

        public CarMakeCommand toCommand() {
            return new CarMakeCommand(name, country);
        }
    }

    public record CarModelRequest(
            @NotNull Long makeId,
            @NotBlank @Size(max = 150) String name,
            @NotNull @Min(1950) @Max(2100) Integer yearFrom,
            @Min(1950) @Max(2100) Integer yearTo) {

        public CarModelCommand toCommand() {
            return new CarModelCommand(makeId, name, yearFrom, yearTo);
        }
    }

    public record ProductRequest(
            @NotBlank @Size(max = 64) String sku,
            @Size(max = 64) String oemNumber,
            @NotBlank @Size(max = 255) String name,
            @Size(max = 5000) String description,
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
            @NotNull @Min(0) Integer stockQuantity,
            @Size(max = 500) String imageUrl,
            @NotNull Long brandId,
            @NotNull Long categoryId,
            Set<Long> compatibleModelIds,
            Boolean active) {

        public ProductCommand toCommand() {
            return new ProductCommand(sku, oemNumber, name, description, price, stockQuantity, imageUrl, brandId,
                    categoryId, compatibleModelIds == null ? Set.of() : compatibleModelIds,
                    active == null || active);
        }
    }

    public record StockRequest(@NotNull @Min(0) Integer stockQuantity) {
    }
}
