package com.foodapp.service;

import com.foodapp.entity.Order;
import com.foodapp.entity.OrderStatus;
import com.foodapp.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Demonstrates Spring's @Scheduled task support: a restaurant that never
 * confirms an order within 15 minutes has it auto-cancelled, so customers
 * aren't left waiting forever and the order doesn't sit in limbo.
 */
@Component
@RequiredArgsConstructor
@Slf4j   // Lombok: injects a `log` field (SLF4J logger) without boilerplate
public class OrderCleanupScheduler {

    private final OrderRepository orderRepository;

    private static final int STALE_ORDER_MINUTES = 15;

    // fixedRate = 300000 -> runs every 5 minutes (300,000 ms), independent of how
    // long the previous run took. Use fixedDelay instead if you need runs to wait
    // for the previous execution to finish first.
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cancelStaleOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(STALE_ORDER_MINUTES);

        List<Order> staleOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PLACED, cutoff);

        for (Order order : staleOrders) {
            order.setStatus(OrderStatus.CANCELLED);
            log.info("Auto-cancelled stale order id={} (placed at {})", order.getId(), order.getCreatedAt());
        }
        // dirty checking flushes all these updates at the end of the transaction
    }
}
