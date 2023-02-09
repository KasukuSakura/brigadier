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
import com.kasukusakura.brigadier.command.tree.ArgumentCommandNode;
import com.kasukusakura.brigadier.reader.AnyValueReader;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class EnumArgumentType<T> implements ArgumentType<T> {
    private final Function<String, T> mapper;
    private final Iterable<String> values;

    public EnumArgumentType(Function<String, T> mapper, Iterable<String> values) {
        this.mapper = mapper;
        this.values = values;
    }

    public EnumArgumentType(Map<String, T> values) {
        this(values::get, values.keySet());
    }

    public EnumArgumentType(Collection<T> values) {
        HashMap<String, T> mapping = new HashMap<>();
        for (T value : values) {
            mapping.put(String.valueOf(value), value);
        }
        this.mapper = mapping::get;
        this.values = mapping.keySet();
    }

    @Override
    public T parse(CommandDispatcher<?> dispatcher, AnyValueReader reader) throws CommandSyntaxException {
        int cursorx = reader.getCursor();
        Object any = reader.readAny();
        if (any == null) {
            reader.setCursor(cursorx);
            throw ArgumentCommandNode.incorrectArgument(reader, dispatcher, null);
        }

        T value = mapper.apply(any.toString());
        if (value == null) {
            reader.setCursor(cursorx);
            throw ArgumentCommandNode.incorrectArgument(reader, dispatcher, null);
        }
        return value;
    }

    @Override
    public <Src> CompletableFuture<Suggestions> listSuggestions(CommandContextBuilder<Src> context, SuggestionsBuilder builder) {
        for (String key : values) {
            if (key.toLowerCase().startsWith(builder.remainingAsStringLowercase)) {
                builder.suggest(key);
            }
        }
        return builder.buildFuture();
    }
}
