package fr.livio.azuredevvm;

import jakarta.ws.rs.core.MultivaluedHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class CollectorUtils {

    /**
     * Returns a collector that accumulates elements into a {@code MultivaluedHashMap} by mapping the stream
     * elements to keys and values using the provided functions.
     * @param keyMapper a function to map the stream elements to keys
     * @param valueMapper a function to map the stream elements to values
     * @param <T> the type of the input elements of the stream
     * @param <K> the type of keys maintained by the resulting {@code MultivaluedHashMap}
     * @param <U> the type of mapped values in the resulting {@code MultivaluedHashMap}
     * @return a {@code Collector} that collects elements into a {@code MultivaluedHashMap<K, U>}
     */
    public static <T, K, U> Collector<T, ?, MultivaluedHashMap<K, U>> toMultivaluedMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper) {
        final Supplier<MultivaluedHashMap<K, U>> supplier = MultivaluedHashMap::new;

        final BiConsumer<MultivaluedHashMap<K, U>, T> accumulator = (map, element) -> map.add(
                keyMapper.apply(element),
                valueMapper.apply(element)
        );

        final BinaryOperator<MultivaluedHashMap<K, U>> combiner = (map1, map2) -> {
            map2.forEach((key, value1) -> value1.forEach(value -> map1.add(key, value)));
            return map1;
        };

        return Collector.of(supplier, accumulator, combiner, Collector.Characteristics.IDENTITY_FINISH);
    }
}
