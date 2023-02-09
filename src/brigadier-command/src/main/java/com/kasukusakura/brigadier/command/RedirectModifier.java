/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command;


import com.kasukusakura.brigadier.command.context.CommandContext;
import com.kasukusakura.brigadier.command.exceptions.CommandSyntaxException;

import java.util.Collection;

@FunctionalInterface
public interface RedirectModifier<S> {
    Collection<S> apply(CommandContext<S> context) throws CommandSyntaxException;
}
