/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.context;

import com.kasukusakura.brigadier.command.tree.CommandNode;

public class SuggestionContext<Src> {
    public final CommandNode<Src> parent;
    public final int startPos;

    public SuggestionContext(CommandNode<Src> parent, int startPos) {
        this.parent = parent;
        this.startPos = startPos;
    }
}
