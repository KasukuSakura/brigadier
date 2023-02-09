/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.suggestion;

import com.kasukusakura.brigadier.command.context.StringRange;

import java.util.Objects;

public class Suggestion {
    public final StringRange range;
    public final String text;
    public final String tooltip;

    public Suggestion(StringRange range, String text, String tooltip) {
        this.range = range;
        this.text = text;
        this.tooltip = tooltip;
    }

    public int compareToIgnoreCase(Suggestion b) {
        return text.compareToIgnoreCase(b.text);
    }

    @Override
    public int hashCode() {
        return text.hashCode() ^ range.hashCode() ^ (~Objects.hashCode(tooltip));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Suggestion)) return false;

        Suggestion that = (Suggestion) o;
        return Objects.equals(range, that.range) && Objects.equals(text, that.text) && Objects.equals(tooltip, that.tooltip);
    }
}
