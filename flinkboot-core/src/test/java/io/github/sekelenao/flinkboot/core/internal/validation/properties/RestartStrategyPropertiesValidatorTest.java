package io.github.sekelenao.flinkboot.core.internal.validation.properties;

import io.github.sekelenao.flinkboot.core.api.properties.restart.ExponentialDelayRestartProperties;
import io.github.sekelenao.flinkboot.core.api.properties.restart.FailureRateRestartProperties;
import io.github.sekelenao.flinkboot.core.api.properties.restart.FixedDelayRestartProperties;
import io.github.sekelenao.flinkboot.core.api.properties.restart.RestartStrategyProperties;
import io.github.sekelenao.flinkboot.core.api.properties.restart.RestartStrategyType;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RestartStrategyPropertiesValidator")
class RestartStrategyPropertiesValidatorTest {

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("Should throw NullPointerException when properties is null")
        void shouldThrowWhenPropertiesIsNull() {
            var context = mock(ConstraintValidatorContext.class);
            var exception = assertThrows(NullPointerException.class, () -> RestartStrategyPropertiesValidator.validate(null, context));
            assertEquals("properties must not be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw NullPointerException when context is null")
        void shouldThrowWhenContextIsNull() {
            var props = new RestartStrategyProperties(null, null, null, null);
            var exception = assertThrows(NullPointerException.class, () -> RestartStrategyPropertiesValidator.validate(props, null));
            assertEquals("context must not be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        private ConstraintValidatorContext createMockContext() {
            var context = mock(ConstraintValidatorContext.class);
            var builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
            var nodeBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);

            when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
            when(builder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
            return context;
        }

        @Test
        @DisplayName("Should return true for valid FIXED_DELAY strategy")
        void shouldPassWithValidFixedDelay() {
            var context = mock(ConstraintValidatorContext.class);
            var fixed = new FixedDelayRestartProperties(3, Duration.ofSeconds(5));
            var props = new RestartStrategyProperties(RestartStrategyType.FIXED_DELAY, fixed, null, null);

            assertTrue(RestartStrategyPropertiesValidator.validate(props, context));
        }

        @Test
        @DisplayName("Should return false when sub-configuration provided for NO_RESTART")
        void shouldFailWhenSubConfigProvidedForNoRestart() {
            var context = createMockContext();
            var fixed = new FixedDelayRestartProperties(3, Duration.ofSeconds(5));
            var props = new RestartStrategyProperties(RestartStrategyType.NO_RESTART, fixed, null, null);

            assertFalse(RestartStrategyPropertiesValidator.validate(props, context));
            verify(context).disableDefaultConstraintViolation();
        }

        @Test
        @DisplayName("Should return false when sub-configuration provided without type")
        void shouldFailWhenSubConfigProvidedWithoutType() {
            var context = createMockContext();
            var fixed = new FixedDelayRestartProperties(3, Duration.ofSeconds(5));
            var props = new RestartStrategyProperties(null, fixed, null, null);

            assertFalse(RestartStrategyPropertiesValidator.validate(props, context));
            verify(context).disableDefaultConstraintViolation();
            verify(context).buildConstraintViolationWithTemplate(
                "restart strategy type is required when configuring a sub-block (fixed-delay, failure-rate, exponential-delay)"
            );
        }

        @Test
        @DisplayName("Should return false when failure-rate is provided for FIXED_DELAY")
        void shouldFailWhenFailureRateProvidedForFixedDelay() {
            var context = createMockContext();
            var fixed = new FixedDelayRestartProperties(3, Duration.ofSeconds(5));
            var failure = new FailureRateRestartProperties(3, Duration.ofMinutes(1), Duration.ofSeconds(1));
            var props = new RestartStrategyProperties(RestartStrategyType.FIXED_DELAY, fixed, failure, null);

            assertFalse(RestartStrategyPropertiesValidator.validate(props, context));
            verify(context).disableDefaultConstraintViolation();
            verify(context).buildConstraintViolationWithTemplate(
                "Cannot specify failure-rate or exponential-delay when restart strategy type is FIXED_DELAY"
            );
        }

        @Test
        @DisplayName("Should return false when fixed-delay is provided for FAILURE_RATE")
        void shouldFailWhenFixedDelayProvidedForFailureRate() {
            var context = createMockContext();
            var fixed = new FixedDelayRestartProperties(3, Duration.ofSeconds(5));
            var failure = new FailureRateRestartProperties(3, Duration.ofMinutes(1), Duration.ofSeconds(1));
            var props = new RestartStrategyProperties(RestartStrategyType.FAILURE_RATE, fixed, failure, null);

            assertFalse(RestartStrategyPropertiesValidator.validate(props, context));
            verify(context).disableDefaultConstraintViolation();
            verify(context).buildConstraintViolationWithTemplate(
                "Cannot specify fixed-delay or exponential-delay when restart strategy type is FAILURE_RATE"
            );
        }

        @Test
        @DisplayName("Should return false when failure-rate is provided for EXPONENTIAL_DELAY")
        void shouldFailWhenFailureRateProvidedForExponentialDelay() {
            var context = createMockContext();
            var expo = new ExponentialDelayRestartProperties(Duration.ofSeconds(1), Duration.ofMinutes(1), 2.0, Duration.ofHours(1), 0.1);
            var failure = new FailureRateRestartProperties(3, Duration.ofMinutes(1), Duration.ofSeconds(1));
            var props = new RestartStrategyProperties(RestartStrategyType.EXPONENTIAL_DELAY, null, failure, expo);

            assertFalse(RestartStrategyPropertiesValidator.validate(props, context));
            verify(context).disableDefaultConstraintViolation();
            verify(context).buildConstraintViolationWithTemplate(
                "Cannot specify fixed-delay or failure-rate when restart strategy type is EXPONENTIAL_DELAY"
            );
        }

        @Test
        @DisplayName("Should return false when maxBackoff < initialBackoff in EXPONENTIAL_DELAY")
        void shouldFailWhenMaxBackoffSmallerThanInitialInExponentialDelay() {
            var context = createMockContext();
            var expo = new ExponentialDelayRestartProperties(Duration.ofSeconds(10), Duration.ofSeconds(1), 2.0, Duration.ofHours(1), 0.1);
            var props = new RestartStrategyProperties(RestartStrategyType.EXPONENTIAL_DELAY, null, null, expo);

            assertFalse(RestartStrategyPropertiesValidator.validate(props, context));
            verify(context).disableDefaultConstraintViolation();
            verify(context).buildConstraintViolationWithTemplate(
                "max-backoff cannot be smaller than initial-backoff in exponential-delay restart strategy"
            );
        }

        @Test
        @DisplayName("Should return true when strategy type is FALLBACK with no sub-configuration")
        void shouldPassWhenFallbackWithNoSubConfig() {
            var context = mock(ConstraintValidatorContext.class);
            var props = new RestartStrategyProperties(RestartStrategyType.FALLBACK, null, null, null);

            assertTrue(RestartStrategyPropertiesValidator.validate(props, context));
        }

        @Test
        @DisplayName("Should return true for valid FAILURE_RATE strategy")
        void shouldPassWithValidFailureRate() {
            var context = mock(ConstraintValidatorContext.class);
            var failure = new FailureRateRestartProperties(3, Duration.ofMinutes(1), Duration.ofSeconds(1));
            var props = new RestartStrategyProperties(RestartStrategyType.FAILURE_RATE, null, failure, null);

            assertTrue(RestartStrategyPropertiesValidator.validate(props, context));
        }

        @Test
        @DisplayName("Should return true for valid EXPONENTIAL_DELAY strategy")
        void shouldPassWithValidExponentialDelay() {
            var context = mock(ConstraintValidatorContext.class);
            var expo = new ExponentialDelayRestartProperties(Duration.ofSeconds(1), Duration.ofMinutes(1), 2.0, Duration.ofHours(1), 0.1);
            var props = new RestartStrategyProperties(RestartStrategyType.EXPONENTIAL_DELAY, null, null, expo);

            assertTrue(RestartStrategyPropertiesValidator.validate(props, context));
        }

        @Test
        @DisplayName("Should return true when initial-backoff equals max-backoff in EXPONENTIAL_DELAY")
        void shouldPassWhenInitialBackoffEqualsMaxBackoffInExponentialDelay() {
            var context = mock(ConstraintValidatorContext.class);
            var expo = new ExponentialDelayRestartProperties(Duration.ofSeconds(5), Duration.ofSeconds(5), 2.0, Duration.ofHours(1), 0.1);
            var props = new RestartStrategyProperties(RestartStrategyType.EXPONENTIAL_DELAY, null, null, expo);

            assertTrue(RestartStrategyPropertiesValidator.validate(props, context));
        }

        @Test
        @DisplayName("Should return false when exponential-delay is provided for FIXED_DELAY")
        void shouldFailWhenExponentialDelayProvidedForFixedDelay() {
            var context = createMockContext();
            var fixed = new FixedDelayRestartProperties(3, Duration.ofSeconds(5));
            var expo = new ExponentialDelayRestartProperties(Duration.ofSeconds(1), Duration.ofMinutes(1), 2.0, Duration.ofHours(1), 0.1);
            var props = new RestartStrategyProperties(RestartStrategyType.FIXED_DELAY, fixed, null, expo);

            assertFalse(RestartStrategyPropertiesValidator.validate(props, context));
            verify(context).disableDefaultConstraintViolation();
            verify(context).buildConstraintViolationWithTemplate(
                "Cannot specify failure-rate or exponential-delay when restart strategy type is FIXED_DELAY"
            );
        }

        @Test
        @DisplayName("Should return false when exponential-delay is provided for FAILURE_RATE")
        void shouldFailWhenExponentialDelayProvidedForFailureRate() {
            var context = createMockContext();
            var failure = new FailureRateRestartProperties(3, Duration.ofMinutes(1), Duration.ofSeconds(1));
            var expo = new ExponentialDelayRestartProperties(Duration.ofSeconds(1), Duration.ofMinutes(1), 2.0, Duration.ofHours(1), 0.1);
            var props = new RestartStrategyProperties(RestartStrategyType.FAILURE_RATE, null, failure, expo);

            assertFalse(RestartStrategyPropertiesValidator.validate(props, context));
            verify(context).disableDefaultConstraintViolation();
            verify(context).buildConstraintViolationWithTemplate(
                "Cannot specify fixed-delay or exponential-delay when restart strategy type is FAILURE_RATE"
            );
        }

        @Test
        @DisplayName("Should return false when fixed-delay is provided for EXPONENTIAL_DELAY")
        void shouldFailWhenFixedDelayProvidedForExponentialDelay() {
            var context = createMockContext();
            var fixed = new FixedDelayRestartProperties(3, Duration.ofSeconds(5));
            var expo = new ExponentialDelayRestartProperties(Duration.ofSeconds(1), Duration.ofMinutes(1), 2.0, Duration.ofHours(1), 0.1);
            var props = new RestartStrategyProperties(RestartStrategyType.EXPONENTIAL_DELAY, fixed, null, expo);

            assertFalse(RestartStrategyPropertiesValidator.validate(props, context));
            verify(context).disableDefaultConstraintViolation();
            verify(context).buildConstraintViolationWithTemplate(
                "Cannot specify fixed-delay or failure-rate when restart strategy type is EXPONENTIAL_DELAY"
            );
        }

        @Test
        @DisplayName("Should return false when sub-configuration provided for FALLBACK")
        void shouldFailWhenSubConfigProvidedForFallback() {
            var context = createMockContext();
            var fixed = new FixedDelayRestartProperties(3, Duration.ofSeconds(5));
            var props = new RestartStrategyProperties(RestartStrategyType.FALLBACK, fixed, null, null);

            assertFalse(RestartStrategyPropertiesValidator.validate(props, context));
            verify(context).disableDefaultConstraintViolation();
            verify(context).buildConstraintViolationWithTemplate(
                "No sub-configuration (fixed-delay, failure-rate, exponential-delay) must be specified when restart strategy type is FALLBACK"
            );
        }
    }
}
