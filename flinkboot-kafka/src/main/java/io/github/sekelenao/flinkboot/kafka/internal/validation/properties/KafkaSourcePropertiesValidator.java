package io.github.sekelenao.flinkboot.kafka.internal.validation.properties;

import io.github.sekelenao.flinkboot.core.internal.validation.properties.PropertiesValidator;
import io.github.sekelenao.flinkboot.kafka.api.properties.source.KafkaOffsetInitializer;
import io.github.sekelenao.flinkboot.kafka.api.properties.source.KafkaSourceProperties;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Objects;

/**
 * Validates cross-field invariants for {@link KafkaSourceProperties}.
 */
public final class KafkaSourcePropertiesValidator {

    private final KafkaSourceProperties properties;
    private final ConstraintValidatorContext context;

    private KafkaSourcePropertiesValidator(KafkaSourceProperties properties, ConstraintValidatorContext context) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.context = Objects.requireNonNull(context, "context must not be null");
    }

    public static boolean validate(KafkaSourceProperties properties, ConstraintValidatorContext context) {
        return new KafkaSourcePropertiesValidator(properties, context).execute();
    }

    private boolean execute() {
        return validateTopicSubscription() && validateStartingOffsets();
    }

    private boolean validateTopicSubscription() {
        var hasTopics = !properties.topics().isEmpty();
        var hasPattern = properties.topicPattern().isPresent();

        if (hasTopics && hasPattern) {
            return reject("topicPattern", "Cannot configure both 'topics' and 'topic-pattern'");
        }

        if (!hasTopics && !hasPattern) {
            return reject("topics", "Either 'topics' or 'topic-pattern' must be specified");
        }

        return true;
    }

    private boolean validateStartingOffsets() {
        var strategy = properties.startingOffsets();
        if (strategy == null) {
            return true;
        }

        switch (strategy) {
            case TIMESTAMP: return validateTimestampOffsets();
            case OFFSETS: return validateSpecificOffsets();
            default: return validateStandardOffsets(strategy);
        }
    }

    private boolean validateTimestampOffsets() {
        if (properties.startingOffsetsTimestamp().isEmpty()) {
            return reject(
                "startingOffsetsTimestamp",
                "starting-offsets-timestamp is required when starting-offsets is TIMESTAMP"
            );
        }
        if (!properties.startingOffsetsPartitionOffsets().isEmpty()) {
            return reject(
                "startingOffsetsPartitionOffsets",
                "starting-offsets-partition-offsets must not be specified when starting-offsets is TIMESTAMP"
            );
        }
        return true;
    }

    private boolean validateSpecificOffsets() {
        if (properties.startingOffsetsPartitionOffsets().isEmpty()) {
            return reject(
                "startingOffsetsPartitionOffsets",
                "starting-offsets-partition-offsets is required and cannot be empty when starting-offsets is OFFSETS"
            );
        }
        if (properties.startingOffsetsTimestamp().isPresent()) {
            return reject(
                "startingOffsetsTimestamp",
                "starting-offsets-timestamp must not be specified when starting-offsets is OFFSETS"
            );
        }
        return true;
    }

    private boolean validateStandardOffsets(KafkaOffsetInitializer strategy) {
        if (properties.startingOffsetsTimestamp().isPresent()) {
            return reject(
                "startingOffsetsTimestamp",
                "starting-offsets-timestamp must not be specified when starting-offsets is " + strategy
            );
        }
        if (!properties.startingOffsetsPartitionOffsets().isEmpty()) {
            return reject(
                "startingOffsetsPartitionOffsets",
                "starting-offsets-partition-offsets must not be specified when starting-offsets is " + strategy
            );
        }
        return true;
    }

    private boolean reject(String property, String message) {
        return PropertiesValidator.reject(context, property, message);
    }
}
