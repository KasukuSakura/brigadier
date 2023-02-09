/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.utils;

import java.util.Objects;

public class Pair<K, V> {
    public final K k;
    public final V v;

    public Pair(K k, V v) {
        this.k = k;
        this.v = v;
    }

    @Override
    public String toString() {
        return "Pair[" + k + "," + v + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(k) ^ ~Objects.hashCode(v);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair)) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(k, pair.k) && Objects.equals(v, pair.v);
    }
}
