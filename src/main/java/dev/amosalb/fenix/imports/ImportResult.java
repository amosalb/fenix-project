package dev.amosalb.fenix.imports;

public record ImportResult(int ordersCreated, int customersCreated, int productsCreated, int itemsCreated,
        int ordersSkipped) {
}
