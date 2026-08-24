package dev.amosalb.fenix.orders;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Entity
@Table(name = "orders")
public class Order {

    private static final NumberFormat BRL_FORMAT = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;

    private String externalId;

    private String platformSource;

    private String status;

    private Long subtotalCents;

    private Long shippingCents;

    private Long discountCents;

    private Long totalCents;

    private LocalDateTime orderDate;

    @Column(name = "platform_order_id")
    private String platformOrderId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getPlatformSource() {
        return platformSource;
    }

    public void setPlatformSource(String platformSource) {
        this.platformSource = platformSource;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getSubtotalCents() {
        return subtotalCents;
    }

    public void setSubtotalCents(Long subtotalCents) {
        this.subtotalCents = subtotalCents;
    }

    public Long getShippingCents() {
        return shippingCents;
    }

    public void setShippingCents(Long shippingCents) {
        this.shippingCents = shippingCents;
    }

    public Long getDiscountCents() {
        return discountCents;
    }

    public void setDiscountCents(Long discountCents) {
        this.discountCents = discountCents;
    }

    public Long getTotalCents() {
        return totalCents;
    }

    public void setTotalCents(Long totalCents) {
        this.totalCents = totalCents;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public String getPlatformOrderId() {
        return platformOrderId;
    }

    public void setPlatformOrderId(String platformOrderId) {
        this.platformOrderId = platformOrderId;
    }

    public String getTotalBrl() {
        if (totalCents == null) {
            return "-";
        }
        return BRL_FORMAT.format(BigDecimal.valueOf(totalCents, 2));
    }

    public String getOrderDateFormatted() {
        return orderDate == null ? "-" : orderDate.format(DATE_FORMAT);
    }
}
