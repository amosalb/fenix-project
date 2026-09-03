package dev.amosalb.fenix.imports;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import dev.amosalb.fenix.customer.Customer;
import dev.amosalb.fenix.customer.CustomerPanacheRepository;
import dev.amosalb.fenix.customer.PublicIdType;
import dev.amosalb.fenix.orders.Order;
import dev.amosalb.fenix.orders.OrderItem;
import dev.amosalb.fenix.orders.OrderItemPanacheRepository;
import dev.amosalb.fenix.orders.OrderPanacheRepository;
import dev.amosalb.fenix.products.Product;
import dev.amosalb.fenix.products.ProductPanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

@ApplicationScoped
public class CsvImportService {

    private static final Logger LOG = Logger.getLogger(CsvImportService.class);

    private static final String PLATFORM_SOURCE = "nuvemshop";

    @Inject
    CustomerPanacheRepository customerRepo;

    @Inject
    ProductPanacheRepository productRepo;

    @Inject
    OrderPanacheRepository orderRepo;

    @Inject
    OrderItemPanacheRepository orderItemRepo;

    @Transactional
    public ImportResult importCsv(InputStream inputStream) throws IOException {
        CsvMapper mapper = new CsvMapper();
        mapper.enable(CsvParser.Feature.TRIM_SPACES);

        CsvSchema schema = CsvSchema.emptySchema()
                .withHeader()
                .withColumnSeparator(';')
                .withQuoteChar('"');

        Counters counters = new Counters();

        try (MappingIterator<Map<String, String>> rows = mapper
                .readerFor(Map.class)
                .with(schema)
                .readValues(new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1))) {

            while (rows.hasNext()) {
                Map<String, String> row = rows.next();
                long line = rows.getCurrentLocation().getLineNr();
                try {
                    processRow(row, counters);
                } catch (RuntimeException ex) {
                    // Abort the whole import (transaction rolls back) but tell the user exactly
                    // which CSV line and what went wrong, instead of failing silently.
                    throw new CsvImportException(line, ex.getMessage(), ex);
                }
            }
        }

        return new ImportResult(counters.ordersCreated, counters.customersCreated,
                counters.productsCreated, counters.itemsCreated, counters.ordersSkipped);
    }

    private void processRow(Map<String, String> row, Counters counters) {
        String externalId = row.get("Número do Pedido");
        if (externalId.isBlank()) {
            counters.ordersSkipped++;
            return;
        }

        Long customerId = null;
        String rawPublicId = digitsOnly(row.get("CPF / CNPJ"));
        if (rawPublicId != null) {
            Customer customer = customerRepo.findByPublicId(rawPublicId).orElseGet(Customer::new);
            if (customer.getId() == null) {
                customer.setPublicId(rawPublicId);
                customer.setPublicIdType(PublicIdType.fromDocument(rawPublicId));
                customer.setName(blankToNull(row.get("Nome do comprador")));
                customer.setEmail(blankToNull(row.get("E-mail")));
                customer.setPhone(blankToNull(row.get("Telefone")));
                customerRepo.persistAndFlush(customer);
                counters.customersCreated++;
            }
            customerId = customer.getId();
        }

        Order order = orderRepo.findByExternalId(externalId).orElseGet(Order::new);
        boolean orderCreated = order.getId() == null;
        if (orderCreated) {
            order.setExternalId(externalId);
            order.setPlatformSource(PLATFORM_SOURCE);
            orderRepo.persistAndFlush(order);
            counters.ordersCreated++;
        }
        if (customerId != null || orderCreated) {
            order.setCustomerId(customerId);
        }

        if (!row.get("Nome do comprador").isBlank()) {
            order.setStatus(blankToNull(row.get("Status do Envio")));
            order.setOrderDate(parseDate(row.get("Data")));
            order.setPlatformOrderId(blankToNull(row.get("Identificador do pedido")));
            order.setSubtotalCents(parseMoneyToCents(row.get("Subtotal")));
            order.setShippingCents(parseMoneyToCents(row.get("Valor do Frete")));
            order.setDiscountCents(parseMoneyToCents(row.get("Desconto")));
            order.setTotalCents(parseMoneyToCents(row.get("Total")));
        }

        Product product = resolveProduct(row);
        if (product.getId() == null) {
            productRepo.persistAndFlush(product);
            counters.productsCreated++;
        }

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setProductId(product.getId());
        item.setQuantity(parseQuantity(row.get("Quantidade Comprada")));
        item.setUnitPriceCents(parseMoneyToCents(row.get("Valor do Produto")));
        orderItemRepo.persist(item);
        counters.itemsCreated++;
    }

    private Product resolveProduct(Map<String, String> row) {
        String sku = row.get("SKU");
        Product product = sku.isBlank() ? new Product() : productRepo.findBySku(sku).orElseGet(Product::new);
        if (product.getId() == null) {
            product.setPlatformSource(PLATFORM_SOURCE);
            product.setSku(sku);
            product.setName(blankToNull(row.get("Nome do Produto")));
            product.setCurrentPriceCents(parseMoneyToCents(row.get("Valor do Produto")));
        }
        return product;
    }

    private String digitsOnly(String value) {
        // TODO CNPJ now allows letters, so we should probably not strip them out. But for now, let's keep it simple and just remove non-digits.
        return value == null ? null : value.replaceAll("\\D", "");
    }

    private Long parseMoneyToCents(String value) {
        String v = blankToNull(value);
        if (v == null) return null;
        return new BigDecimal(v)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    private LocalDateTime parseDate(String value) {
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        } catch (DateTimeParseException ex) {
            LOG.warnf("Data inválida: '%s'. Ignorando e usando null.", value);
            return null;
        }
    }

    private Integer parseQuantity(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Quantidade Comprada inválida: '" + value + "'", ex);
        }
    }

    private String blankToNull(String value) {
        return value.isBlank() ? null : value;
    }

    /** Mutable counters accumulated while iterating the CSV rows. */
    private static final class Counters {
        int ordersCreated;
        int customersCreated;
        int productsCreated;
        int itemsCreated;
        int ordersSkipped;
    }

}
