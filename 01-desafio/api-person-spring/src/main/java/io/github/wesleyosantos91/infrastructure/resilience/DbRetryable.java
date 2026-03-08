package io.github.wesleyosantos91.infrastructure.resilience;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.transaction.CannotCreateTransactionException;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Retryable(
        includes = {
                TransientDataAccessException.class,
                QueryTimeoutException.class,
                CannotAcquireLockException.class,
                PessimisticLockingFailureException.class,
                CannotCreateTransactionException.class,
                DataAccessResourceFailureException.class
        },
        maxRetriesString = "${app.resilience.retry.max-retries:3}",
        delayString = "${app.resilience.retry.delay:100ms}",
        jitterString = "${app.resilience.retry.jitter:25ms}",
        multiplierString = "${app.resilience.retry.multiplier:2.0}",
        maxDelayString = "${app.resilience.retry.max-delay:2s}"
)
public @interface DbRetryable {
}
