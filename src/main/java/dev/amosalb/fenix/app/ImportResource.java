package dev.amosalb.fenix.app;

import dev.amosalb.fenix.imports.CsvImportException;
import dev.amosalb.fenix.imports.CsvImportService;
import dev.amosalb.fenix.imports.ImportResult;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.FileInputStream;
import java.io.IOException;

@Path("app/importar")
@Produces(MediaType.TEXT_HTML)
public class ImportResource {

    @Location("imports/upload.html")
    Template uploadTemplate;

    @Location("imports/result.html")
    Template resultTemplate;

    @Inject
    CsvImportService csvImportService;

    @GET
    public TemplateInstance uploadForm() {
        return uploadTemplate.data("message", null);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public TemplateInstance upload(@RestForm("file") FileUpload file) throws IOException {
        if (file == null) {
            throw new WebApplicationException("Arquivo não enviado", Response.Status.BAD_REQUEST);
        }
        try (FileInputStream inputStream = new FileInputStream(file.uploadedFile().toFile())) {
            ImportResult result = csvImportService.importCsv(inputStream);
            return resultTemplate.data("result", result);
        } catch (CsvImportException ex) {
            // Show the failing line and reason back on the upload form instead of a raw 500.
            return uploadTemplate.data("message", ex.getMessage());
        }
    }
}
