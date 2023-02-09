/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.utils;

import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class SeedSpliterator<T> extends Spliterators.AbstractSpliterator<T> {
    protected boolean isDone;
    private T seed;
    private final Function<T, T> next;
    private final Predicate<T> canAdvance;

    public SeedSpliterator(T seed, Function<T, T> next) {
        this(seed, next, Objects::nonNull);
    }

    public SeedSpliterator(T seed, Function<T, T> next, Predicate<T> canAdvance) {
        super(
                Long.MAX_VALUE,
                Spliterator.ORDERED | Spliterator.IMMUTABLE
        );
        this.seed = seed;
        this.next = next;
        this.canAdvance = canAdvance;
    }


    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (isDone) return false;

        if (!canAdvance.test(seed)) {
            seed = null;
            isDone = true;
            return false;
        }

        action.accept(seed);
        seed = next.apply(seed);

        return true;
    }
}
