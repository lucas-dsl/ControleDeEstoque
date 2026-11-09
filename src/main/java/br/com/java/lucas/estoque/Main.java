package br.com.java.lucas.estoque;

import br.com.java.lucas.estoque.application.StockService;
import br.com.java.lucas.estoque.application.StockSummary;
import br.com.java.lucas.estoque.domain.StockHold;
import br.com.java.lucas.estoque.domain.exception.StockException;
import br.com.java.lucas.estoque.infrastructure.InMemoryStockRepository;

//Demonstração do controle de estoque. Percorre um cenário completo e mostra o estado a cada passo.
public class Main {

    private static final String PRODUCT = "CAFE-500G";

    public static void main(String[] args) {
        StockService service = new StockService(new InMemoryStockRepository());

        step("Entrada de 100 unidades");
        service.addStock(PRODUCT, 100);
        show(service);

        step("Reserva de 30 unidades para o pedido PED-1001");
        StockHold reservation = service.reserve(PRODUCT, 30, "PED-1001");
        System.out.println("  reserva criada: " + reservation.getId());
        show(service);

        step("Bloqueio de 20 unidades (avaria), sem pedido associado");
        StockHold block = service.block(PRODUCT, 20, null);
        System.out.println("  bloqueio criado: " + block.getId());
        show(service);

        step("Saída de 60 unidades (só 50 estão disponíveis): deve falhar");
        attempt(() -> service.removeStock(PRODUCT, 60));
        show(service);

        step("Saída de 50 unidades");
        service.removeStock(PRODUCT, 50);
        show(service);

        step("Liberação da reserva");
        service.release(PRODUCT, reservation.getId());
        show(service);

        step("Nova liberação da mesma reserva: deve falhar");
        attempt(() -> service.release(PRODUCT, reservation.getId()));
        show(service);

        step("Liberação do bloqueio");
        service.release(PRODUCT, block.getId());
        show(service);

        step("Consulta de produto inexistente: deve falhar");
        attempt(() -> service.getStock("INEXISTENTE"));
    }

    private static void step(String description) {
        System.out.println();
        System.out.println("> " + description);
    }

    private static void show(StockService service) {
        StockSummary s = service.getStock(PRODUCT);
        System.out.printf("  produto=%s total=%d disponível=%d reservado=%d bloqueado=%d%n",
                s.productId(), s.total(), s.available(), s.reserved(), s.blocked());
    }

    private static void attempt(Runnable action) {
        try {
            action.run();
            System.out.println("  (não houve erro, e era esperado um)");
        } catch (StockException e) {
            System.out.println("  erro de negócio: " + e.getMessage());
        }
    }
}