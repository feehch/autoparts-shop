package feehch.dev.autoparts.application;

import feehch.dev.autoparts.application.port.out.UnitOfWork;

import java.util.function.Supplier;

/**
 * Тестовая реализация единицы работы — выполняет действие без транзакции.
 */
public class DirectUnitOfWork implements UnitOfWork {

    @Override
    public <T> T inTransaction(Supplier<T> action) {
        return action.get();
    }
}
