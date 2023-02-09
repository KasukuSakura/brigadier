/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.builtin;

import com.kasukusakura.brigadier.command.CommandHandler;
import com.kasukusakura.brigadier.command.CommandPreprocessHandler;
import com.kasukusakura.brigadier.command.ParsedResults;
import com.kasukusakura.brigadier.command.context.CommandContext;
import com.kasukusakura.brigadier.command.context.CommandContextBuilder;
import com.kasukusakura.brigadier.command.exceptions.CommandSyntaxException;
import com.kasukusakura.brigadier.command.tree.CommandNode;
import com.kasukusakura.brigadier.command.tree.LiteralCommandNode;
import com.kasukusakura.brigadier.reader.AnyValueReader;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class HelpCommand<Src> extends LiteralCommandNode<Src> implements CommandHandler<Src> {
    private final BiConsumer<Src, StringBuilder> sendMessage;
    private CommandNode<Src> redirect;

    public HelpCommand(
            Predicate<Src> req,
            CommandNode<Src> redirect,
            boolean fork,
            BiConsumer<Src, StringBuilder> sendMessage
    ) {
        super("help", null, req, redirect, null, fork);
        description("Show helping command");
        this.sendMessage = sendMessage;
    }

    public HelpCommand<Src> attach(CommandNode<Src> target) {
        this.redirect = target;
        return this;
    }

    @Override
    public CommandNode<Src> getRedirect() {
        return redirect;
    }

    @Override
    public CommandPreprocessHandler<Src> getPreprocessHandler() {
        return this;
    }

    @Override
    public CommandHandler<Src> parse(CommandContext<?> context) throws CommandSyntaxException {
        return this;
    }

    @Override
    public void parse(CommandContextBuilder<Src> contextBuilder, AnyValueReader reader) throws CommandSyntaxException {
        super.parse(contextBuilder, reader);

        contextBuilder.doExecuteChild(false);
    }

    @Override
    public void process(CommandContext<Src> context) throws CommandSyntaxException {
        CommandContext<Src> child = context.getChild();
        if (child == null) {
            sendMessage.accept(context.getSource(), context.getDispatcher().renderHelpUsage(
                    context.getDispatcher().root, context.getSource()
            ));
        } else {
            sendMessage.accept(context.getSource(), context.getDispatcher().renderHelpUsage(
                    new ParsedResults<>((CommandContextBuilder<Src>) child, context.getResults().reader, null)
            ));
        }
    }

}
