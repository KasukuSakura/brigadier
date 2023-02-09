/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command;

import com.kasukusakura.brigadier.command.context.CommandContext;
import com.kasukusakura.brigadier.command.exceptions.CommandSyntaxException;

@FunctionalInterface
public interface CommandHandler<Src> extends CommandPreprocessHandler<Src> {
    void process(CommandContext<Src> context) throws CommandSyntaxException;

    @Override
    default CommandHandler<Src> parse(CommandContext<?> context) throws CommandSyntaxException {
        return this;
    }
}
