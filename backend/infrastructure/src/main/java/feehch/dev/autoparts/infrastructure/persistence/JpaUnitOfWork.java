package feehch.dev.autoparts.infrastructure.persistence;

import feehch.dev.autoparts.application.port.out.UnitOfWork;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Реализация единицы работы через транзакции Spring (JPA / PostgreSQL).
 */
@Component
public class JpaUnitOfWork implements UnitOfWork {

    private final TransactionTemplate transactionTemplate;

    public JpaUnitOfWork(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T inTransaction(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }
}
