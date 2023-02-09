/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.tree;

import com.kasukusakura.brigadier.command.CommandPreprocessHandler;
import com.kasukusakura.brigadier.command.RedirectModifier;
import com.kasukusakura.brigadier.command.context.CommandContextBuilder;
import com.kasukusakura.brigadier.command.context.StringRange;
import com.kasukusakura.brigadier.command.exceptions.CommandSyntaxException;
import com.kasukusakura.brigadier.command.suggestion.Suggestions;
import com.kasukusakura.brigadier.command.suggestion.SuggestionsBuilder;
import com.kasukusakura.brigadier.reader.AnyValueReader;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class LiteralCommandNode<Src> extends CommandNode<Src> {
    private final String name;
    private final String nameLowercase;

    public LiteralCommandNode(
            String name,
            CommandPreprocessHandler<Src> command,
            Predicate<Src> req,
            CommandNode<Src> redirect,
            RedirectModifier<Src> modifier,
            boolean fork
    ) {
        super(command, req, redirect, modifier, fork);
        this.name = name;
        this.nameLowercase = name.toLowerCase(Locale.ROOT);
    }

    @Override
    public void parse(CommandContextBuilder<Src> contextBuilder, AnyValueReader reader) throws CommandSyntaxException {
        int cursor = reader.getCursor();
        String readed = String.valueOf(reader.readAny());
        if (!name.equals(readed)) {
            reader.setCursor(cursor);

            throw contextBuilder.getDispatcher().newCommandSyntaxException("Excepted " + name + " but found " + readed + " at " + cursor);
        }

        contextBuilder.withNode(this, StringRange.between(cursor, reader.getCursor()));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CompletableFuture<Suggestions> listSuggestions(CommandContextBuilder<Src> context, SuggestionsBuilder builder) {
        if (nameLowercase.startsWith(builder.remainingAsStringLowercase)) {
            return builder.suggest(name, description()).buildFuture();
        }
        return Suggestions.empty();
    }
}
