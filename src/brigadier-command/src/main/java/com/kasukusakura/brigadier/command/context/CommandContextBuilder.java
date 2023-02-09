/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.context;

import com.kasukusakura.brigadier.command.CommandDispatcher;
import com.kasukusakura.brigadier.command.CommandHandler;
import com.kasukusakura.brigadier.command.ParsedResults;
import com.kasukusakura.brigadier.command.tree.CommandNode;

public interface CommandContextBuilder<Src> extends CommandContext<Src> {
    public static <Src> CommandContextBuilder<Src> newBuilder(
            CommandDispatcher<Src> dispatcher,
            Src source,
            CommandNode<Src> rootNode,
            int start
    ) {
        return new CommandContextBuilderImpl<>(dispatcher, source, rootNode, start);
    }

    CommandContextBuilder<Src> copy();

    CommandContextBuilder<Src> withCommand(CommandHandler<Src> handler);

    CommandContextBuilder<Src> withNode(CommandNode<Src> node, StringRange range);

    CommandContextBuilder<Src> withChild(CommandContextBuilder<Src> child);

    CommandContextBuilder<Src> withSource(Src source);

    CommandContextBuilder<Src> withMetadata(String name, Class<?> type, Object metadata);

    CommandContextBuilder<Src> withArgument(String name, Class<?> type, Object value);

    SuggestionContext<Src> findSuggestionContext(int cursor);

    boolean inheritCommandHandlerForChild();

    CommandContextBuilder<Src> inheritCommandHandlerForChild(boolean value);

    boolean doExecuteChild();

    CommandContextBuilder<Src> doExecuteChild(boolean execute);

    @Override
    CommandContextBuilder<Src> getChild();

    @Override
    CommandContextBuilder<Src> copyFor(Src source);

    default CommandContextBuilder<Src> newBuilderForChild(CommandDispatcher<Src> dispatcher, Src source, CommandNode<Src> redirect, int cursor) {
        CommandContextBuilderImpl<Src> impl = (CommandContextBuilderImpl<Src>) newBuilder(dispatcher, source, redirect, cursor);
        impl.getAllMetadata().addAll(this.getAllMetadata());
        return impl;
    }

    default CommandContextBuilder<Src> getLastChild() {
        CommandContextBuilder<Src> rsp = this;
        while (true) {
            CommandContextBuilder<Src> next = rsp.getChild();
            if (next == null) return rsp;
            rsp = next;
        }
    }

    CommandContextBuilder<Src> withResults(ParsedResults<Src> results);
}
