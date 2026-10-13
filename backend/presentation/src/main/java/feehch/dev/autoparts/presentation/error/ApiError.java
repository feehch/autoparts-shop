package feehch.dev.autoparts.presentation.error;

import java.time.Instant;
import java.util.List;

/**
 * Единый формат ответа об ошибке.
 */
public record ApiError(Instant timestamp, int status, String error, String message, String path,
                       List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {
    }
}
