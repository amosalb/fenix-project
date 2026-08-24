package dev.amosalb.fenix.imports;

/**
 * Thrown when the CSV import must be aborted due to an unrecoverable error in a specific row.
 * Carries the CSV line number so the user can locate and fix the offending row.
 */
public class CsvImportException extends RuntimeException {

    private final long line;

    public CsvImportException(long line, String message, Throwable cause) {
        super("Linha " + line + ": " + message, cause);
        this.line = line;
    }

    public long getLine() {
        return line;
    }
}
