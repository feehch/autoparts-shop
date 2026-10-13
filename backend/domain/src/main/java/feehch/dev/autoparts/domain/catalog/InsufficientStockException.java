package feehch.dev.autoparts.domain.catalog;

import feehch.dev.autoparts.domain.common.ConflictException;

/**
 * На складе недостаточно товара.
 */
public class InsufficientStockException extends ConflictException {

    public InsufficientStockException(String productName, int requested, int available) {
        super("Недостаточно товара «" + productName + "» на складе: запрошено " + requested
                + ", доступно " + available);
    }
}
