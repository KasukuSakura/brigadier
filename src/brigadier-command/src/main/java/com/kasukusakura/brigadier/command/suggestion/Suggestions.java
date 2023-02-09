/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.suggestion;

import com.kasukusakura.brigadier.command.context.StringRange;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Suggestions {
    public static final Suggestions EMPTY = new Suggestions(StringRange.at(0), Collections.emptyList());

    public final StringRange range;
    public final List<Suggestion> suggestions;


    public Suggestions(StringRange range, List<Suggestion> suggestions) {
        this.range = range;
        this.suggestions = suggestions;
    }

    public static CompletableFuture<Suggestions> empty() {
        return CompletableFuture.completedFuture(EMPTY);
    }

    public static Suggestions create(Collection<Suggestion> suggestions) {

        if (suggestions.isEmpty()) {
            return EMPTY;
        }

        int start = Integer.MAX_VALUE;
        int end = Integer.MIN_VALUE;
        for (final Suggestion suggestion : suggestions) {
            start = Math.min(suggestion.range.start, start);
            end = Math.max(suggestion.range.end, end);
        }

        List<Suggestion> sorted = new ArrayList<>(
                suggestions instanceof Set ? suggestions : new HashSet<>(suggestions)
        );
        sorted.sort(Suggestion::compareToIgnoreCase);
        return new Suggestions(StringRange.between(start, end), sorted);
    }

    public static Suggestions merge(Collection<Suggestions> suggestions) {
        if (suggestions.isEmpty()) return EMPTY;
        if (suggestions.size() == 1) return suggestions.iterator().next();

        Set<Suggestion> texts = new HashSet<>();
        for (Suggestions s : suggestions) texts.addAll(s.suggestions);

        return create(texts);
    }

}
