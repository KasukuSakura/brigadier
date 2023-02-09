/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.context;

import com.kasukusakura.brigadier.command.tree.CommandNode;

public class ParsedCommandNode<Src> {
    public final CommandNode<Src> node;
    public final StringRange range;

    public ParsedCommandNode(CommandNode<Src> node, StringRange range) {
        this.node = node;
        this.range = range;
    }
}
