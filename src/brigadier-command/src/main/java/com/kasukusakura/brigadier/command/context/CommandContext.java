/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.context;

import com.kasukusakura.brigadier.command.CommandDispatcher;
import com.kasukusakura.brigadier.command.CommandHandler;
import com.kasukusakura.brigadier.command.ParsedResults;
import com.kasukusakura.brigadier.command.RedirectModifier;

import java.util.List;

public interface CommandContext<Src> {
    Src getSource();

    CommandContext<?> dropSource();

    CommandContext<Src> getChild();

    List<ParsedCommandNode<Src>> getNodes();

    List<MetadataValue> getAllMetadata();

    <T> T getMetadata(String name, Class<T> type);

    <T> T getArgument(String name, Class<T> type);

    CommandHandler<Src> getCommand();

    StringRange getRange();

    default boolean hasNodes() {
        return !getNodes().isEmpty();
    }

    RedirectModifier<Src> getRedirectModifier();

    CommandContext<Src> copyFor(Src source);

    CommandDispatcher<Src> getDispatcher();

    ParsedResults<Src> getResults();
}
