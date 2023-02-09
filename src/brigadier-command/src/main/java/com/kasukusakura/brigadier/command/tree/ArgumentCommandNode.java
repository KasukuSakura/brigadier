/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.tree;

import com.kasukusakura.brigadier.command.CommandDispatcher;
import com.kasukusakura.brigadier.command.CommandPreprocessHandler;
import com.kasukusakura.brigadier.command.RedirectModifier;
import com.kasukusakura.brigadier.command.arguments.ArgumentType;
import com.kasukusakura.brigadier.command.context.CommandContextBuilder;
import com.kasukusakura.brigadier.command.context.StringRange;
import com.kasukusakura.brigadier.command.exceptions.CommandSyntaxException;
import com.kasukusakura.brigadier.command.suggestion.Suggestions;
import com.kasukusakura.brigadier.command.suggestion.SuggestionsBuilder;
import com.kasukusakura.brigadier.reader.AnyValueReader;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class ArgumentCommandNode<Src, T> extends CommandNode<Src> {
    private final String name;
    private final ArgumentType<T> arg;

    public ArgumentCommandNode(
            String name, ArgumentType<T> argumentType,
            CommandPreprocessHandler<Src> command,
            Predicate<Src> req,
            CommandNode<Src> redirect,
            RedirectModifier<Src> modifier,
            boolean fork
    ) {
        super(command, req, redirect, modifier, fork);
        this.name = name;
        this.arg = argumentType;
    }

    @Override
    public void parse(CommandContextBuilder<Src> contextBuilder, AnyValueReader reader) throws CommandSyntaxException {
        int cursor = reader.getCursor();

        try {
            T rsp = arg.parse(contextBuilder, reader);
            contextBuilder.withArgument(getName(), arg.type(), rsp);
        } catch (CommandSyntaxException e) {
            reader.setCursor(cursor);
            throw e;
        } catch (RuntimeException cause) {
            reader.setCursor(cursor);
            throw incorrectArgument(reader, contextBuilder.getDispatcher(), cause);
        }
        contextBuilder.withNode(this, StringRange.between(cursor, reader.getCursor()));
    }

    public static String incorrectArgumentMessage(AnyValueReader reader) {
        return "Incorrect argument at position " + reader.getCursor() + ": " + reader.fetchContent(reader.getCursor(), Integer.MAX_VALUE) + "\n" + "Full command: " + reader.fetchContent(0, Integer.MAX_VALUE);
    }

    public static CommandSyntaxException incorrectArgument(AnyValueReader reader, CommandDispatcher<?> dispatcher, Throwable cause) {
        return dispatcher.newCommandSyntaxException(incorrectArgumentMessage(reader), cause);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CompletableFuture<Suggestions> listSuggestions(CommandContextBuilder<Src> context, SuggestionsBuilder builder) {
        return arg.listSuggestions(context, builder);
    }
}
