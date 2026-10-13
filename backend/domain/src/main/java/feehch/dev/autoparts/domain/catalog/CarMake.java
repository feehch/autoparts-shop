package feehch.dev.autoparts.domain.catalog;

import feehch.dev.autoparts.domain.common.Guard;

import java.io.Serializable;

/**
 * Марка легкового автомобиля (Toyota, Lada, ...).
 */
public class CarMake implements Serializable {

    private Long id;
    private String name;
    private String country;

    public CarMake(Long id, String name, String country) {
        this.id = id;
        update(name, country);
    }

    public void update(String name, String country) {
        this.name = Guard.notBlank(name, "название марки");
        this.country = Guard.trimToNull(country);
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
}
