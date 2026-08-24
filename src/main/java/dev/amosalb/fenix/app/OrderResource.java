package dev.amosalb.fenix.app;

import dev.amosalb.fenix.orders.OrderPanacheRepository;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("app/pedidos")
@Produces(MediaType.TEXT_HTML)
public class OrderResource {

    @Location("orders/list.html")
    Template template;

    @Inject
    OrderPanacheRepository orderRepo;

    @GET
    public TemplateInstance list() {
        return template.data("orders", orderRepo.listAll());
    }
}
