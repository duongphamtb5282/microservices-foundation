package com.pacific.core.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pacific.core.messaging.MDCUtil;

/**
 * Shared executors for the async API family (ADR-0011).
 *
 * <p>{@code coreAsyncExecutor} — virtual-thread backed (house style, ADR-0008) so the executeAsync*
 * family (KafkaCommandBus, SimpleQueryBus, RetryStrategyImpl, CircuitBreakerService, MDCUtil) can
 * never silently land on {@code ForkJoinPool.commonPool()} — unbounded, shared, starvation-prone.
 *
 * <p>{@code outboxPublisherExecutor} — small dedicated platform pool (4 threads, named {@code
 * outbox-publish-*}) for the outbox relays, the only latency-critical scheduled jobs.
 */
@Configuration
public class AsyncExecutorConfiguration {

  @Bean(name = "coreAsyncExecutor", destroyMethod = "shutdown")
  public ExecutorService coreAsyncExecutor() {
    ExecutorService executor =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("core-async-", 0).factory());
    // MDCUtil is a static utility; wire its async helpers to this executor (ADR-0011).
    MDCUtil.setAsyncExecutor(executor);
    return executor;
  }

  @Bean(name = "outboxPublisherExecutor", destroyMethod = "shutdown")
  public ExecutorService outboxPublisherExecutor() {
    ThreadFactory factory =
        r -> {
          Thread t = new Thread(r);
          t.setName("outbox-publish-" + t.getId());
          return t;
        };
    return Executors.newFixedThreadPool(4, factory);
  }
}
