package io.github.sekelenao.flinkboot.core.internal.validation.properties;

import io.github.sekelenao.flinkboot.core.api.properties.execution.ExecutionProperties;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates cross-field invariants for {@link ExecutionProperties}.
 */
public final class ExecutionPropertiesValidator {

    private ExecutionPropertiesValidator() {
        throw new AssertionError("You cannot instantiate this class");
    }

    public static boolean validate(ExecutionProperties properties, ConstraintValidatorContext context) {
        var parallelism = properties.parallelism();
        var maxParallelism = properties.maxParallelism();
        if (parallelism.isEmpty() || maxParallelism.isEmpty()) {
            return true;
        }
        var current = parallelism.getAsInt();
        var max = maxParallelism.getAsInt();
        if (current > max) {
            return PropertiesValidator.reject(
                context,
                "parallelism",
                "parallelism (" + current + ") cannot exceed max-parallelism (" + max + ")"
            );
        }

        return true;
    }
}
