/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.tree;

public class CommandNodeBuilderBase {
    protected <Src, T extends CommandNode<Src>> T mirror(T obj, RootCommandNode<Src> src) {
        obj.initMap();
        obj.children.putAll(src.children);
        obj.arguments.putAll(src.arguments);
        obj.literals.putAll(src.literals);
        obj.setPreprocessHandler(src.getPreprocessHandler());
        return obj;
    }
}
