package io.github.sekelenao.flinkboot.test.api;

import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A sink for collecting elements in unit tests.
 *
 * @param <T> the type of elements collected by this sink
 */
@SuppressWarnings("deprecation")
public final class CollectingSink<T> implements SinkFunction<T> {

    private final List<T> elements = new CopyOnWriteArrayList<>();

    private CollectingSink() {
    }

    /**
     * Creates a collecting sink.
     *
     * @param <T> the type of elements collected by the sink
     * @return a new collecting sink
     */
    public static <T> CollectingSink<T> create() {
        return new CollectingSink<>();
    }

    /**
     * Collects an element.
     *
     * @param value the element to collect
     */
    @Override
    public void invoke(T value) {
        elements.add(Objects.requireNonNull(value, "value must not be null"));
    }

    /**
     * Returns an immutable snapshot of the collected elements.
     *
     * @return the collected elements
     */
    public List<T> elements() {
        return List.copyOf(elements);
    }

    /**
     * Removes all collected elements.
     */
    public void clear() {
        elements.clear();
    }
}
