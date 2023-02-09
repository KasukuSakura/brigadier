/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.arguments;

import com.kasukusakura.brigadier.command.CommandDispatcher;
import com.kasukusakura.brigadier.command.context.CommandContextBuilder;
import com.kasukusakura.brigadier.command.exceptions.CommandSyntaxException;
import com.kasukusakura.brigadier.command.suggestion.Suggestions;
import com.kasukusakura.brigadier.command.suggestion.SuggestionsBuilder;
import com.kasukusakura.brigadier.reader.AnyValueReader;

import java.util.concurrent.CompletableFuture;

public interface ArgumentType<T> {
    default T parse(CommandDispatcher<?> dispatcher, AnyValueReader reader) throws CommandSyntaxException {
        throw dispatcher.newCommandSyntaxException("ArgumentType.parse not yet implemented");
    }

    default T parse(CommandContextBuilder<?> dispatcher, AnyValueReader reader) throws CommandSyntaxException {
        return parse(dispatcher.getDispatcher(), reader);
    }

    default Class<T> type() {
        return null;
    }

    default <Src> CompletableFuture<Suggestions> listSuggestions(CommandContextBuilder<Src> context, SuggestionsBuilder builder) {
        return Suggestions.empty();
    }

}
