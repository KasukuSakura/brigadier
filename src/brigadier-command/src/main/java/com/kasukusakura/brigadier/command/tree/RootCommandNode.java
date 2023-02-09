/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.tree;

import com.kasukusakura.brigadier.command.context.CommandContextBuilder;
import com.kasukusakura.brigadier.command.exceptions.CommandSyntaxException;
import com.kasukusakura.brigadier.command.suggestion.Suggestions;
import com.kasukusakura.brigadier.command.suggestion.SuggestionsBuilder;
import com.kasukusakura.brigadier.reader.AnyValueReader;

import java.util.concurrent.CompletableFuture;

public class RootCommandNode<Src> extends CommandNode<Src> {
    public RootCommandNode() {
        super(null, allowAll(), null, null, false);
    }

    @Override
    public void parse(CommandContextBuilder<Src> contextBuilder, AnyValueReader reader) throws CommandSyntaxException {
    }

    @Override
    public String getName() {
        return "";
    }

    public void merge(RootCommandNode<Src> other) {
        for (CommandNode<Src> child : other.children.values()) {
            register(child);
        }
    }

    @Override
    public CompletableFuture<Suggestions> listSuggestions(CommandContextBuilder<Src> context, SuggestionsBuilder builder) {
        return Suggestions.empty();
    }
}
