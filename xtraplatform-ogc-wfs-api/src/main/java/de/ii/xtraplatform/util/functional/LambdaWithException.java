/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.util.functional;

import java.util.function.BiConsumer;
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

    @FunctionalInterface
    public interface BiConsumerWithException<T, U, E extends Exception> {
        void apply(T t, U u) throws E;
    }

    public static <T, R, E extends Exception> Function<T, R> mayThrow(FunctionWithException<T, R, E> fe) {
        return arg -> {
            try {
                return fe.apply(arg);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <T, E extends Exception> Consumer<T> consumerMayThrow(ConsumerWithException<T, E> ce) {
        return arg -> {
            try {
                ce.apply(arg);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <T, U, E extends Exception> BiConsumer<T, U> biConsumerMayThrow(BiConsumerWithException<T, U, E> ce) {
        return (arg, arg2) -> {
            try {
                ce.apply(arg, arg2);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
