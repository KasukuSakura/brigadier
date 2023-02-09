/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command;

import com.kasukusakura.brigadier.command.context.CommandContextBuilder;
import com.kasukusakura.brigadier.command.exceptions.CommandSyntaxException;
import com.kasukusakura.brigadier.command.tree.CommandNode;
import com.kasukusakura.brigadier.reader.AnyValueReader;

import java.util.Map;

public class ParsedResults<Src> {
    public final CommandContextBuilder<Src> context;
    public final AnyValueReader reader;
    public final Map<CommandNode<Src>, CommandSyntaxException> exceptions;


    public ParsedResults(
            CommandContextBuilder<Src> context, AnyValueReader reader, Map<CommandNode<Src>, CommandSyntaxException> exceptions) {
        this.context = context;
        this.reader = reader;
        this.exceptions = exceptions;
    }
}
