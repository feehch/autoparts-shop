package feehch.dev.autoparts.application.catalog;

import feehch.dev.autoparts.domain.catalog.CarMake;
import feehch.dev.autoparts.domain.catalog.CarModel;

public record CarModelView(Long id, Long makeId, String makeName, String name, Integer yearFrom, Integer yearTo) {

    public static CarModelView from(CarModel m, CarMake make) {
        return new CarModelView(m.getId(), m.getMakeId(), make == null ? null : make.getName(), m.getName(),
                m.getYearFrom(), m.getYearTo());
    }
}
