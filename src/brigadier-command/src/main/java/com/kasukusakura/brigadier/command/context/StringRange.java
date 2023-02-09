/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.context;

public class StringRange {
    public final int start;
    public final int end;

    public StringRange(int start, int end) {
        this.start = start;
        this.end = end;
    }


    public static StringRange at(final int pos) {
        return new StringRange(pos, pos);
    }

    public static StringRange between(final int start, final int end) {
        return new StringRange(start, end);
    }

    public static StringRange encompassing(final StringRange a, final StringRange b) {
        return new StringRange(Math.min(a.start, b.start), Math.max(a.end, b.end));
    }

    @Override
    public String toString() {
        return "[" + start + ", " + end + "]";
    }

    public boolean isEmpty() {
        return start == end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StringRange)) return false;
        StringRange that = (StringRange) o;
        return start == that.start && end == that.end;
    }

    @Override
    public int hashCode() {
        return start ^ end;
    }
}
