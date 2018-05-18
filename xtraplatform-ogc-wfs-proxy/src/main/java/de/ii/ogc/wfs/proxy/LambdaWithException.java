package de.ii.ogc.wfs.proxy;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author zahnen
 */
public class LambdaWithException {

    @FunctionalInterface
    public interface FunctionWithException<T, R, E extends Exception> {
        R apply(T t) throws E;
    }

    @FunctionalInterface
    public interface ConsumerWithException<T, E extends Exception> {
        void apply(T t) throws E;
    }

    static <T, R, E extends Exception> Function<T, R> mayThrow(FunctionWithException<T, R, E> fe) {
        return arg -> {
            try {
                return fe.apply(arg);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    static <T, R, E extends Exception> Consumer<T> consumerMayThrow(ConsumerWithException<T, E> ce) {
        return arg -> {
            try {
                ce.apply(arg);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
