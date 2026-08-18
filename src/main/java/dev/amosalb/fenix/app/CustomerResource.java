package dev.amosalb.fenix.app;

import dev.amosalb.fenix.customer.CustomerPanacheRepository;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("app/clientes")
@Produces(MediaType.TEXT_HTML)
public class CustomerResource {

    @Location("customers/list.html")
    Template template;

    @Inject
    CustomerPanacheRepository repository;

    @GET
    public TemplateInstance list() {

        return template.data(
                "customers", repository.listAll()
        );
    }
}
