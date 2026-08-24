package dev.amosalb.fenix.imports;

import dev.amosalb.fenix.customer.Customer;
import dev.amosalb.fenix.customer.CustomerPanacheRepository;
import dev.amosalb.fenix.customer.PublicIdType;
import dev.amosalb.fenix.orders.Order;
import dev.amosalb.fenix.orders.OrderItem;
import dev.amosalb.fenix.orders.OrderItemPanacheRepository;
import dev.amosalb.fenix.orders.OrderPanacheRepository;
import dev.amosalb.fenix.products.Product;
import dev.amosalb.fenix.products.ProductPanacheRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsvImportServiceTest {

    @Mock CustomerPanacheRepository customerRepo;
    @Mock ProductPanacheRepository productRepo;
    @Mock OrderPanacheRepository orderRepo;
    @Mock OrderItemPanacheRepository orderItemRepo;

    @InjectMocks
    CsvImportService service;


    private InputStream csv(String... dataRows) {
        String content = "Número do Pedido;E-mail;Data;Status do Envio;Subtotal;Desconto;Valor do Frete;" +
        "Total;Nome do comprador;CPF / CNPJ;Telefone;Nome do Produto;" +
        "Valor do Produto;Quantidade Comprada;SKU;Identificador do pedido" + "\n" + String.join("\n", dataRows);
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1));
    }

    @Test
    void shouldCreateCustomerOrderAndProductWhenAllAreNew() throws IOException {
        when(customerRepo.findByPublicId(anyString())).thenReturn(Optional.empty());
        when(orderRepo.findByExternalId(anyString())).thenReturn(Optional.empty());
        when(productRepo.findBySku(anyString())).thenReturn(Optional.empty());

        String row = "1001;cliente@email.com;08/08/2026 10:00:00;Aberto;314.7;0;57.43;372.13;" +
                     "João Silva;06040006692;+5511999999999;Produto Teste;314.7;1;SKU-001;9999";

        ImportResult result = service.importCsv(csv(row));

        assertEquals(1, result.ordersCreated());
        assertEquals(1, result.customersCreated());
        assertEquals(1, result.productsCreated());
        assertEquals(1, result.itemsCreated());
        assertEquals(0, result.ordersSkipped());
    }

    @Test
    void shouldDetectCpfTypeWhenPublicIdHas11Digits() throws IOException {
        when(customerRepo.findByPublicId("06040006692")).thenReturn(Optional.empty());
        when(orderRepo.findByExternalId(anyString())).thenReturn(Optional.empty());
        when(productRepo.findBySku(anyString())).thenReturn(Optional.empty());

        String row = "1001;;08/08/2026 10:00:00;Aberto;100;0;0;100;" +
                     "Nome;06040006692;;Produto;100;1;SKU-X;1";

        service.importCsv(csv(row));

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepo).persistAndFlush(captor.capture());
        assertEquals(PublicIdType.CPF, captor.getValue().getPublicIdType());
        assertNull(captor.getValue().getEmail());
    }

    @Test
    void shouldDetectCnpjTypeWhenPublicIdHas14Digits() throws IOException {
        when(customerRepo.findByPublicId("12345678000195")).thenReturn(Optional.empty());
        when(orderRepo.findByExternalId(anyString())).thenReturn(Optional.empty());
        when(productRepo.findBySku(anyString())).thenReturn(Optional.empty());

        String row = "1002;empresa@b.com;08/08/2026 10:00:00;Aberto;100;0;0;100;" +
                     "Empresa;12345678000195;;Produto;100;1;SKU-Y;1";

        service.importCsv(csv(row));

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepo).persistAndFlush(captor.capture());
        assertEquals(PublicIdType.CNPJ, captor.getValue().getPublicIdType());
    }

    @Test
    void shouldNotCreateDuplicateCustomerWhenAlreadyExists() throws IOException {
        Customer existing = new Customer();
        existing.setId(5L);
        existing.setPublicId("06040006692");

        when(customerRepo.findByPublicId("06040006692")).thenReturn(Optional.of(existing));
        when(orderRepo.findByExternalId(anyString())).thenReturn(Optional.empty());
        when(productRepo.findBySku(anyString())).thenReturn(Optional.empty());

        String row = "1001;a@b.com;08/08/2026 10:00:00;Aberto;100;0;0;100;" +
                     "Nome;06040006692;;Produto;100;1;SKU-001;1";

        ImportResult result = service.importCsv(csv(row));

        assertEquals(0, result.customersCreated());
        verify(customerRepo, never()).persistAndFlush(any(Customer.class));
    }

    @Test
    void shouldNotCreateDuplicateOrderWhenAlreadyExists() throws IOException {
        Order existing = new Order();
        existing.setId(99L);
        existing.setExternalId("1001");

        when(customerRepo.findByPublicId(anyString())).thenReturn(Optional.empty());
        when(orderRepo.findByExternalId("1001")).thenReturn(Optional.of(existing));
        when(productRepo.findBySku(anyString())).thenReturn(Optional.empty());

        String row = "1001;a@b.com;08/08/2026 10:00:00;Aberto;100;0;0;100;" +
                     "Nome;06040006692;;Produto;100;1;SKU-001;1";

        ImportResult result = service.importCsv(csv(row));

        assertEquals(0, result.ordersCreated());
        verify(orderRepo, never()).persistAndFlush(any(Order.class));
    }

    @Test
    void shouldNotCreateDuplicateProductWhenSkuAlreadyExists() throws IOException {
        Product existing = new Product();
        existing.setId(50L);
        existing.setSku("SKU-001");

        when(customerRepo.findByPublicId(anyString())).thenReturn(Optional.empty());
        when(orderRepo.findByExternalId(anyString())).thenReturn(Optional.empty());
        when(productRepo.findBySku("SKU-001")).thenReturn(Optional.of(existing));

        String row = "1001;a@b.com;08/08/2026 10:00:00;Aberto;100;0;0;100;" +
                     "Nome;06040006692;;Produto;100;1;SKU-001;1";

        ImportResult result = service.importCsv(csv(row));

        assertEquals(0, result.productsCreated());
        verify(productRepo, never()).persistAndFlush(any(Product.class));
    }

    @Test
    void shouldAlwaysCreateNewProductWhenSkuIsBlank() throws IOException {
        when(customerRepo.findByPublicId(anyString())).thenReturn(Optional.empty());
        Order createdOrder = new Order();
        createdOrder.setId(20L);
        when(orderRepo.findByExternalId(anyString()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(createdOrder));
        doAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(p.getName().hashCode() & 0x7FFFFFFFL);
            return null;
        }).when(productRepo).persistAndFlush(any(Product.class));

        // Both rows have blank SKU (field index 14)
        String row1 = "1001;a@b.com;08/08/2026 10:00:00;Aberto;200;0;0;200;" +
                      "Nome;06040006692;;Produto A;100;1;;1";
        String row2 = "1001;;;;;;;;;;;Produto B;100;1;;";


        InputStream csv = csv(row1, row2);
        ImportResult result = service.importCsv(csv);

        assertEquals(2, result.productsCreated());
        assertEquals(2, result.itemsCreated());
        verify(productRepo, never()).findBySku(anyString());
    }

    @Test
    void shouldSkipRowsWithEmptyOrderNumber() throws IOException {
        String emptyRow = ";a@b.com;;;;;;;Nome;;;Produto;100;1;SKU-001;1";

        ImportResult result = service.importCsv(csv(emptyRow));

        assertEquals(1, result.ordersSkipped());
        assertEquals(0, result.ordersCreated());
        assertEquals(0, result.itemsCreated());
        verifyNoInteractions(orderRepo, customerRepo, productRepo, orderItemRepo);
    }

    @Test
    void shouldCreateSingleOrderWithMultipleItems() throws IOException {
        Order o = new Order();
        o.setId(20L);
        when(customerRepo.findByPublicId(anyString())).thenReturn(Optional.empty());
        when(orderRepo.findByExternalId("2001")).thenReturn(Optional.empty())
                .thenReturn(Optional.of(o));
        when(productRepo.findBySku("SKU-A")).thenReturn(Optional.empty());
        when(productRepo.findBySku("SKU-B")).thenReturn(Optional.empty());
        doAnswer(inv -> { ((Product) inv.getArgument(0)).setId(30L + ((Product) inv.getArgument(0)).hashCode() % 100); return null; })
                .when(productRepo).persistAndFlush(any(Product.class));

        String row1 = "2001;a@b.com;08/08/2026 10:00:00;Aberto;500;0;0;500;" +
                      "Nome;06040006692;;Produto A;250;1;SKU-A;1";
        String row2 = "2001;;;;;;;;;;;Produto B;250;1;SKU-B;";

        ImportResult result = service.importCsv(csv(row1, row2));

        assertEquals(1, result.ordersCreated());
        assertEquals(2, result.productsCreated());
        assertEquals(2, result.itemsCreated());
        verify(orderItemRepo, times(2)).persist(any(OrderItem.class));
    }

    @Test
    void shouldPopulateMoneyFieldsInCents() throws IOException {
        when(customerRepo.findByPublicId(anyString())).thenReturn(Optional.empty());
        when(orderRepo.findByExternalId(anyString())).thenReturn(Optional.empty());
        when(productRepo.findBySku(anyString())).thenReturn(Optional.empty());

        // total = 372.13, subtotal = 314.7, shipping = 57.43, discount = 0
        String row = "1001;a@b.com;08/08/2026 10:00:00;Aberto;314.7;0;57.43;372.13;" +
                     "Nome;06040006692;;Produto;314.7;1;SKU-001;1";

        service.importCsv(csv(row));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepo).persistAndFlush(captor.capture());
        Order order = captor.getValue();
        assertEquals(37213L, order.getTotalCents());
        assertEquals(31470L, order.getSubtotalCents());
        assertEquals(5743L, order.getShippingCents());
        assertEquals(0L, order.getDiscountCents());
    }

    @Test
    void shouldReturnZeroCountsWhenNoDataRows() throws IOException {
        ImportResult result = service.importCsv(csv()); // header only, no data rows

        assertEquals(0, result.ordersCreated());
        assertEquals(0, result.customersCreated());
        assertEquals(0, result.productsCreated());
        assertEquals(0, result.itemsCreated());
    }

    @Test
    void shouldThrowCsvImportExceptionWithLineNumberWhenQuantityIsInvalid() {
        when(customerRepo.findByPublicId(anyString())).thenReturn(Optional.empty());
        when(orderRepo.findByExternalId(anyString())).thenReturn(Optional.empty());
        when(productRepo.findBySku(anyString())).thenReturn(Optional.empty());

        String goodRow = "1001;a@b.com;08/08/2026 10:00:00;Aberto;100;0;0;100;" +
                          "Nome;06040006692;;Produto;100;1;SKU-001;1";
        String badRow = "1002;a@b.com;08/08/2026 10:00:00;Aberto;100;0;0;100;" +
                         "Nome;06040006692;;Produto;100;abc;SKU-002;2";

        CsvImportException ex = assertThrows(CsvImportException.class,
                () -> service.importCsv(csv(goodRow, badRow)));

        assertEquals(3, ex.getLine()); // header=1, goodRow=2, badRow=3
        assertTrue(ex.getMessage().contains("Linha 3"));
        assertTrue(ex.getMessage().contains("Quantidade Comprada"));
    }

}
