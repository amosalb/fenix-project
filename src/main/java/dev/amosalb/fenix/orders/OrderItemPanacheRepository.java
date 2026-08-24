package dev.amosalb.fenix.orders;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderItemPanacheRepository implements PanacheRepository<OrderItem> {
}
