package io.github.wesleyosantos91.infrastructure.resilience;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.resilience.annotation.ConcurrencyLimit;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ConcurrencyLimit(
        limitString = "${app.db.concurrency.write-limit:2}",
        policy = ConcurrencyLimit.ThrottlePolicy.REJECT
)
public @interface DbWriteConcurrencyLimit {
}
