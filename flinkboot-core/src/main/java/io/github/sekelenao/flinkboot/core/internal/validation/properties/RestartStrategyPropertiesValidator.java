package io.github.sekelenao.flinkboot.core.internal.validation.properties;

import io.github.sekelenao.flinkboot.core.api.properties.restart.RestartStrategyProperties;
import io.github.sekelenao.flinkboot.core.api.properties.restart.RestartStrategyType;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates cross-field invariants for {@link RestartStrategyProperties}.
 */
public final class RestartStrategyPropertiesValidator {

    private RestartStrategyPropertiesValidator() {
        throw new AssertionError("You cannot instantiate this class");
    }

    public static boolean validate(RestartStrategyProperties properties, ConstraintValidatorContext context) {
        var type = properties.type().orElse(RestartStrategyType.FALLBACK);
        switch (type) {
            case FALLBACK: case NO_RESTART: return validateNoSubConfiguration(properties, context, type);
            case FIXED_DELAY: return validateFixedDelay(properties, context);
            case FAILURE_RATE: return validateFailureRate(properties, context);
            case EXPONENTIAL_DELAY: return validateExponentialDelay(properties, context);
            default: return true;
        }
    }

    private static boolean validateNoSubConfiguration(
        RestartStrategyProperties properties,
        ConstraintValidatorContext context,
        RestartStrategyType type
    ) {
        if (properties.fixedDelay().isPresent()
                || properties.failureRate().isPresent()
                || properties.exponentialDelay().isPresent()) {
            return PropertiesValidator.reject(
                context,
                "type",
                "No sub-configuration (fixed-delay, failure-rate, exponential-delay) must be specified when restart strategy type is " + type
            );
        }
        return true;
    }

    private static boolean validateFixedDelay(
        RestartStrategyProperties properties,
        ConstraintValidatorContext context
    ) {
        if (properties.failureRate().isPresent() || properties.exponentialDelay().isPresent()) {
            return PropertiesValidator.reject(
                context,
                "type",
                "Cannot specify failure-rate or exponential-delay when restart strategy type is FIXED_DELAY"
            );
        }
        return true;
    }

    private static boolean validateFailureRate(
        RestartStrategyProperties properties,
        ConstraintValidatorContext context
    ) {
        if (properties.fixedDelay().isPresent() || properties.exponentialDelay().isPresent()) {
            return PropertiesValidator.reject(
                context,
                "type",
                "Cannot specify fixed-delay or exponential-delay when restart strategy type is FAILURE_RATE"
            );
        }
        return true;
    }

    private static boolean validateExponentialDelay(
        RestartStrategyProperties properties,
        ConstraintValidatorContext context
    ) {
        if (properties.fixedDelay().isPresent() || properties.failureRate().isPresent()) {
            return PropertiesValidator.reject(
                context,
                "type",
                "Cannot specify fixed-delay or failure-rate when restart strategy type is EXPONENTIAL_DELAY"
            );
        }

        var expoOpt = properties.exponentialDelay();
        if (expoOpt.isPresent()) {
            var expo = expoOpt.get();
            var initial = expo.initialBackoff();
            var max = expo.maxBackoff();
            if (initial.isPresent() && max.isPresent() && max.get().compareTo(initial.get()) < 0) {
                return PropertiesValidator.reject(
                    context,
                    "exponentialDelay",
                    "max-backoff cannot be smaller than initial-backoff in exponential-delay restart strategy"
                );
            }
        }
        return true;
    }
}
