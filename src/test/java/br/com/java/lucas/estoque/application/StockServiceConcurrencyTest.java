package br.com.java.lucas.estoque.application;

import br.com.java.lucas.estoque.domain.StockHold;
import br.com.java.lucas.estoque.domain.exception.HoldAlreadyReleasedException;
import br.com.java.lucas.estoque.domain.exception.InsufficientStockException;
import br.com.java.lucas.estoque.infrastructure.InMemoryStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Timeout(30)
class StockServiceConcurrencyTest {

    private static final int THREADS = 16;

    private StockService service;

    @BeforeEach
    void setUp() {
        service = new StockService(new InMemoryStockRepository());
    }

    /**
     * Executa as tarefas em várias threads, liberando todas ao mesmo tempo para aumentar a disputa.
     * Qualquer exceção inesperada dentro de uma tarefa faz o teste falhar.
     */
    private void runConcurrently(int tasks, IntConsumer task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Void>> futures = new ArrayList<>();
            for (int i = 0; i < tasks; i++) {
                int index = i;
                futures.add(executor.submit(() -> {
                    start.await();
                    task.accept(index);
                    return null;
                }));
            }
            start.countDown();
            for (Future<Void> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("reservas simultâneas nunca excedem o disponível")
    void concurrentReservationsNeverExceedAvailable() throws Exception {
        int stock = 100;
        int attempts = 1000;
        service.addStock("PROD-1", stock);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger rejections = new AtomicInteger();

        runConcurrently(attempts, i -> {
            try {
                service.reserve("PROD-1", 1, "REQ-" + i);
                successes.incrementAndGet();
            } catch (InsufficientStockException e) {
                rejections.incrementAndGet();
            }
        });

        assertEquals(stock, successes.get());
        assertEquals(attempts - stock, rejections.get());
        StockSummary summary = service.getStock("PROD-1");
        assertEquals(stock, summary.total());
        assertEquals(0, summary.available());
        assertEquals(stock, summary.reserved());
    }

    @Test
    @DisplayName("primeiras entradas simultâneas em produto novo não perdem unidades")
    void concurrentFirstEntriesDoNotLoseUnits() throws Exception {
        int entries = 1000;

        runConcurrently(entries, i -> service.addStock("PROD-NEW", 1));

        StockSummary summary = service.getStock("PROD-NEW");
        assertEquals(entries, summary.total());
        assertEquals(entries, summary.available());
    }

    @RepeatedTest(10)
    @DisplayName("liberações simultâneas da mesma reserva só valem uma vez")
    void concurrentReleasesOfSameHoldSucceedOnlyOnce() throws Exception {
        service.addStock("PROD-1", 10);
        StockHold hold = service.reserve("PROD-1", 5, "REQ-1");
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger alreadyReleased = new AtomicInteger();

        runConcurrently(50, i -> {
            try {
                service.release("PROD-1", hold.getId());
                successes.incrementAndGet();
            } catch (HoldAlreadyReleasedException e) {
                alreadyReleased.incrementAndGet();
            }
        });

        assertEquals(1, successes.get());
        assertEquals(49, alreadyReleased.get());
        StockSummary summary = service.getStock("PROD-1");
        assertEquals(10, summary.total());
        assertEquals(10, summary.available());
        assertEquals(0, summary.reserved());
    }

    @Test
    @DisplayName("consultas durante reservas e liberações sempre veem um estado consistente")
    void readsDuringWritesAreAlwaysConsistent() throws Exception {
        int stock = 100;
        service.addStock("PROD-1", stock);
        AtomicInteger inconsistentReads = new AtomicInteger();

        // índices pares reservam e liberam; índices ímpares consultam
        runConcurrently(2000, i -> {
            if (i % 2 == 0) {
                StockHold hold = service.reserve("PROD-1", 1, "REQ-" + i);
                service.release("PROD-1", hold.getId());
            } else {
                StockSummary s = service.getStock("PROD-1");
                boolean partsAddUp = s.available() + s.reserved() + s.blocked() == s.total();
                if (s.total() != stock || !partsAddUp) {
                    inconsistentReads.incrementAndGet();
                }
            }
        });

        assertEquals(0, inconsistentReads.get());
        StockSummary summary = service.getStock("PROD-1");
        assertEquals(stock, summary.total());
        assertEquals(stock, summary.available());
        assertEquals(0, summary.reserved());
    }
}