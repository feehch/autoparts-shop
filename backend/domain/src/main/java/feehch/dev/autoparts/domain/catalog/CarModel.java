package feehch.dev.autoparts.domain.catalog;

import feehch.dev.autoparts.domain.common.DomainException;
import feehch.dev.autoparts.domain.common.Guard;

import java.io.Serializable;

/**
 * Модель (поколение) автомобиля с годами выпуска. {@code yearTo == null} — выпускается по сей день.
 */
public class CarModel implements Serializable {

    private static final int MIN_YEAR = 1950;
    private static final int MAX_YEAR = 2100;

    private Long id;
    private Long makeId;
    private String name;
    private Integer yearFrom;
    private Integer yearTo;

    public CarModel(Long id, Long makeId, String name, Integer yearFrom, Integer yearTo) {
        this.id = id;
        update(makeId, name, yearFrom, yearTo);
    }

    public void update(Long makeId, String name, Integer yearFrom, Integer yearTo) {
        this.makeId = Guard.notNull(makeId, "марка");
        this.name = Guard.notBlank(name, "название модели");
        this.yearFrom = Guard.notNull(yearFrom, "год начала выпуска");
        if (yearFrom < MIN_YEAR || yearFrom > MAX_YEAR || (yearTo != null && (yearTo < MIN_YEAR || yearTo > MAX_YEAR))) {
            throw new DomainException("Год выпуска должен быть в диапазоне " + MIN_YEAR + "–" + MAX_YEAR);
        }
        if (yearTo != null && yearTo < yearFrom) {
            throw new DomainException("Год окончания выпуска не может быть меньше года начала");
        }
        this.yearTo = yearTo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMakeId() {
        return makeId;
    }

    public String getName() {
        return name;
    }

    public Integer getYearFrom() {
        return yearFrom;
    }

    public Integer getYearTo() {
        return yearTo;
    }
}
