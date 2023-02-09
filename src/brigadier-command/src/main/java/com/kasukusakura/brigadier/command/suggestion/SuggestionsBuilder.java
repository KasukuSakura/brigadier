/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.suggestion;

import com.kasukusakura.brigadier.command.context.StringRange;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SuggestionsBuilder {
    public final CharSequence remaining;
    public final List<Suggestion> suggestions = new ArrayList<>();
    public final int start;
    public final String remainingAsString;
    public final String remainingAsStringLowercase;

    public SuggestionsBuilder(CharSequence remaining, String remainingAsString, String remainingAsStringLowercase, int start) {
        this.remaining = remaining;
        this.remainingAsString = remainingAsString;
        this.remainingAsStringLowercase = remainingAsStringLowercase;
        this.start = start;
    }

    public Suggestions build() {
        return Suggestions.create(suggestions);
    }

    public CompletableFuture<Suggestions> buildFuture() {
        return CompletableFuture.completedFuture(build());
    }

    public SuggestionsBuilder suggest(String text) {
        if (text.contentEquals(remaining)) {
            return this;
        }
        suggestions.add(new Suggestion(StringRange.at(start), text, null));
        return this;
    }

    public SuggestionsBuilder suggest(String text, String tooltip) {
        if (text.contentEquals(remaining)) {
            return this;
        }
        suggestions.add(new Suggestion(StringRange.at(start), text, tooltip));
        return this;
    }

    public SuggestionsBuilder newClean() {
        return new SuggestionsBuilder(remaining, remainingAsString, remainingAsStringLowercase, start);
    }
}
