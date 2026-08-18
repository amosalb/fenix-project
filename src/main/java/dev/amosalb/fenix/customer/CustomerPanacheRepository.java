 package dev.amosalb.fenix.customer;

 import io.quarkus.hibernate.orm.panache.PanacheRepository;
 import jakarta.enterprise.context.RequestScoped;

 @RequestScoped
 public class CustomerPanacheRepository implements PanacheRepository<Customer> {
 }
