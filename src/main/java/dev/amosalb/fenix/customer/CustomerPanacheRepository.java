package dev.amosalb.fenix.customer;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.RequestScoped;

import java.util.Optional;

@RequestScoped
public class CustomerPanacheRepository implements PanacheRepository<Customer> {

    public Optional<Customer> findByPublicId(String publicId) {
        return find("publicId", publicId).firstResultOptional();
    }
}
