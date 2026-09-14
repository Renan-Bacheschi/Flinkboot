package io.github.sekelenao.flinkboot.kafka.internal.validation.properties;

import io.github.sekelenao.flinkboot.core.internal.validation.properties.PropertiesValidator;
import io.github.sekelenao.flinkboot.kafka.api.properties.source.KafkaOffsetInitializer;
import io.github.sekelenao.flinkboot.kafka.api.properties.source.KafkaSourceProperties;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates cross-field invariants for {@link KafkaSourceProperties}.
 */
public final class KafkaSourcePropertiesValidator {

    private KafkaSourcePropertiesValidator() {
        throw new AssertionError("You cannot instantiate this class");
    }

    public static boolean validate(KafkaSourceProperties properties, ConstraintValidatorContext context) {
        return validateTopicSubscription(properties, context)
            && validateStartingOffsets(properties, context);
    }

    private static boolean validateTopicSubscription(KafkaSourceProperties properties, ConstraintValidatorContext context) {
        var hasTopics = !properties.topics().isEmpty();
        var hasPattern = properties.topicPattern().isPresent() && !properties.topicPattern().get().isBlank();

        if (hasTopics && hasPattern) {
            return PropertiesValidator.reject(
                context,
                "topicPattern",
                "Cannot configure both 'topics' and 'topic-pattern'"
            );
        }

        if (!hasTopics && !hasPattern) {
            return PropertiesValidator.reject(
                context,
                "topics",
                "Either 'topics' or 'topic-pattern' must be specified"
            );
        }

        return true;
    }

    private static boolean validateStartingOffsets(KafkaSourceProperties properties, ConstraintValidatorContext context) {
        var strategy = properties.startingOffsets();
        if (strategy == null) {
            return true;
        }

        switch (strategy) {
            case TIMESTAMP:
                return validateTimestampOffsets(properties, context);
            case OFFSETS:
                return validateSpecificOffsets(properties, context);
            default:
                return validateStandardOffsets(properties, context, strategy);
        }
    }

    private static boolean validateTimestampOffsets(KafkaSourceProperties properties, ConstraintValidatorContext context) {
        if (properties.startingOffsetsTimestamp().isEmpty()) {
            return PropertiesValidator.reject(
                context,
                "startingOffsetsTimestamp",
                "starting-offsets-timestamp is required when starting-offsets is TIMESTAMP"
            );
        }
        if (!properties.startingOffsetsPartitionOffsets().isEmpty()) {
            return PropertiesValidator.reject(
                context,
                "startingOffsetsPartitionOffsets",
                "starting-offsets-partition-offsets must not be specified when starting-offsets is TIMESTAMP"
            );
        }
        return true;
    }

    private static boolean validateSpecificOffsets(KafkaSourceProperties properties, ConstraintValidatorContext context) {
        if (properties.startingOffsetsPartitionOffsets().isEmpty()) {
            return PropertiesValidator.reject(
                context,
                "startingOffsetsPartitionOffsets",
                "starting-offsets-partition-offsets is required and cannot be empty when starting-offsets is OFFSETS"
            );
        }
        if (properties.startingOffsetsTimestamp().isPresent()) {
            return PropertiesValidator.reject(
                context,
                "startingOffsetsTimestamp",
                "starting-offsets-timestamp must not be specified when starting-offsets is OFFSETS"
            );
        }
        return true;
    }

    private static boolean validateStandardOffsets(
        KafkaSourceProperties properties,
        ConstraintValidatorContext context,
        KafkaOffsetInitializer strategy
    ) {
        if (properties.startingOffsetsTimestamp().isPresent()) {
            return PropertiesValidator.reject(
                context,
                "startingOffsetsTimestamp",
                "starting-offsets-timestamp must not be specified when starting-offsets is " + strategy
            );
        }
        if (!properties.startingOffsetsPartitionOffsets().isEmpty()) {
            return PropertiesValidator.reject(
                context,
                "startingOffsetsPartitionOffsets",
                "starting-offsets-partition-offsets must not be specified when starting-offsets is " + strategy
            );
        }
        return true;
    }
}
