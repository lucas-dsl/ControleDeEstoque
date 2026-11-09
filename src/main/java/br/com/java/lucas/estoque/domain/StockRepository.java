package br.com.java.lucas.estoque.domain;

import java.util.Optional;

public interface StockRepository {

    Optional<StockItem> findById(String productId);

    void save(StockItem item);
}