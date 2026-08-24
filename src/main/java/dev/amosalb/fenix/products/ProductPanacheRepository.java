package dev.amosalb.fenix.products;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class ProductPanacheRepository implements PanacheRepository<Product> {

    public Optional<Product> findBySku(String sku) {
        return find("sku", sku).firstResultOptional();
    }
}
