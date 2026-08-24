package dev.amosalb.fenix.customer;

public enum PublicIdType {
    CPF,
    CNPJ;

    /**
     * Detects the document type from its digit count: 11 digits for CPF, 14 for CNPJ.
     * Returns null if the value is null or doesn't match either length.
     */
    public static PublicIdType fromDocument(String digitsOnly) {
        if (digitsOnly == null) {
            return null;
        }
        return switch (digitsOnly.length()) {
            case 11 -> CPF;
            case 14 -> CNPJ;
            default -> null;
        };
    }
}
