/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.builder;

import com.kasukusakura.brigadier.command.tree.LiteralCommandNode;

import java.util.Objects;

public class LiteralCommandNodeBuilder<Src> extends AbstractCommandNodeBuilder<Src, LiteralCommandNodeBuilder<Src>> {
    private String name;

    @Override
    public LiteralCommandNode<Src> build() {
        return after(mirror(new LiteralCommandNode<>(
                Objects.requireNonNull(name, "LiteralCommandNodeBuilder.literal"),
                null,
                requirement,
                redirect,
                modifier,
                isFork
        )));
    }

    public LiteralCommandNodeBuilder<Src> literal(String name) {
        this.name = name;
        return this;
    }
}
