package io.github.wesleyosantos91.infrastructure.async;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfig {

    @Bean("applicationTaskExecutor")
    public AsyncTaskExecutor applicationTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("vt-app-");
        executor.setVirtualThreads(true);
        return executor;
    }

    @Bean("cpuTaskExecutor")
    public Executor cpuTaskExecutor() {
        return Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors())
        );
    }
}