package feehch.dev.autoparts.application.catalog;

import feehch.dev.autoparts.domain.catalog.CarMake;

public record CarMakeView(Long id, String name, String country) {

    public static CarMakeView from(CarMake m) {
        return new CarMakeView(m.getId(), m.getName(), m.getCountry());
    }
}
