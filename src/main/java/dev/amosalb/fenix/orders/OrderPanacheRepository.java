package dev.amosalb.fenix.orders;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class OrderPanacheRepository implements PanacheRepository<Order> {

    public Optional<Order> findByExternalId(String externalId) {
        return find("externalId", externalId).firstResultOptional();
    }
}
