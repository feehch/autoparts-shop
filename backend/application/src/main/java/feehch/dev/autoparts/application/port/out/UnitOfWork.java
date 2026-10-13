package feehch.dev.autoparts.application.port.out;

import java.util.function.Supplier;

/**
 * Порт единицы работы: выполняет действие в рамках одной транзакции хранилища.
 */
public interface UnitOfWork {

    <T> T inTransaction(Supplier<T> action);

    default void inTransaction(Runnable action) {
        inTransaction(() -> {
            action.run();
            return null;
        });
    }
}
