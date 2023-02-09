/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.builder;

import com.kasukusakura.brigadier.command.arguments.ArgumentType;
import com.kasukusakura.brigadier.command.tree.ArgumentCommandNode;

import java.util.Objects;

public class ArgumentCommandNodeBuilder<Src, T> extends AbstractCommandNodeBuilder<Src, ArgumentCommandNodeBuilder<Src, T>> {
    private String name;
    private ArgumentType<T> type;

    @Override
    public ArgumentCommandNode<Src, T> build() {
        return after(mirror(new ArgumentCommandNode<>(
                Objects.requireNonNull(name, "ArgumentCommandNodeBuilder.name"),
                Objects.requireNonNull(type, "ArgumentCommandNodeBuilder.type"),
                null,
                requirement,
                redirect,
                modifier,
                isFork
        )));
    }

    public ArgumentCommandNodeBuilder<Src, T> name(String name) {
        this.name = name;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T2> ArgumentCommandNodeBuilder<Src, T2> type(ArgumentType<T2> type) {
        this.type = (ArgumentType<T>) type;
        return (ArgumentCommandNodeBuilder<Src, T2>) this;
    }
}
