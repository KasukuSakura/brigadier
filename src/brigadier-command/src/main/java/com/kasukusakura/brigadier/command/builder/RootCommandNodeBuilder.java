/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.builder;

import com.kasukusakura.brigadier.command.tree.CommandNode;

public class RootCommandNodeBuilder<Src> extends AbstractCommandNodeBuilder<Src, RootCommandNodeBuilder<Src>> {
    @Override
    public CommandNode<Src> build() {
        return after(delegate);
    }
}
