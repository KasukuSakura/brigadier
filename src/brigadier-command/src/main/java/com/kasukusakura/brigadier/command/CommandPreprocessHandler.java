/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command;

import com.kasukusakura.brigadier.command.context.CommandContext;
import com.kasukusakura.brigadier.command.exceptions.CommandSyntaxException;

@FunctionalInterface
public interface CommandPreprocessHandler<Src> {
    CommandHandler<Src> parse(CommandContext<?> context) throws CommandSyntaxException;
}
