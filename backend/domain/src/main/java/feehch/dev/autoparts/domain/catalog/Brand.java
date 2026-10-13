package feehch.dev.autoparts.domain.catalog;

import feehch.dev.autoparts.domain.common.Guard;

import java.io.Serializable;

/**
 * Бренд — производитель запчастей (Bosch, Mann-Filter, ...).
 */
public class Brand implements Serializable {

    private Long id;
    private String name;
    private String country;
    private String description;

    public Brand(Long id, String name, String country, String description) {
        this.id = id;
        update(name, country, description);
    }

    public void update(String name, String country, String description) {
        this.name = Guard.notBlank(name, "название");
        this.country = Guard.trimToNull(country);
        this.description = Guard.trimToNull(description);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    public String getDescription() {
        return description;
    }
}
