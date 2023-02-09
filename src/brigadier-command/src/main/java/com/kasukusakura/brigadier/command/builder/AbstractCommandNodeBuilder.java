/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.builder;

import com.kasukusakura.brigadier.command.CommandHandler;
import com.kasukusakura.brigadier.command.CommandPreprocessHandler;
import com.kasukusakura.brigadier.command.RedirectModifier;
import com.kasukusakura.brigadier.command.tree.CommandNode;
import com.kasukusakura.brigadier.command.tree.CommandNodeBuilderBase;
import com.kasukusakura.brigadier.command.tree.RootCommandNode;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class AbstractCommandNodeBuilder<Src, Thiz extends AbstractCommandNodeBuilder<Src, Thiz>> extends CommandNodeBuilderBase {
    public abstract CommandNode<Src> build();

    protected final RootCommandNode<Src> delegate = new RootCommandNode<>();
    protected Predicate<Src> requirement = CommandNode.allowAll();
    protected CommandNode<Src> redirect;
    protected RedirectModifier<Src> modifier;
    protected boolean isFork = false;
    protected Boolean inheritCommandHandlerForChild;
    protected String description;

    protected Thiz getThis() {
        //noinspection unchecked
        return (Thiz) this;
    }

    public Thiz register(CommandNode<Src> node) {
        delegate.register(node);
        return getThis();
    }

    public Thiz addLiteral(Consumer<LiteralCommandNodeBuilder<Src>> action) {
        LiteralCommandNodeBuilder<Src> builder = new LiteralCommandNodeBuilder<>();
        action.accept(builder);
        return register(builder.build());
    }

    public Thiz addArgument(Consumer<ArgumentCommandNodeBuilder<Src, ?>> action) {
        ArgumentCommandNodeBuilder<Src, ?> builder = new ArgumentCommandNodeBuilder<>();
        action.accept(builder);
        return register(builder.build());
    }

    public Thiz command(CommandHandler<Src> handler) {
        delegate.setCommandHandler(handler);
        return getThis();
    }

    public Thiz preprocessedHandler(CommandPreprocessHandler<Src> handler) {
        delegate.setPreprocessHandler(handler);
        return getThis();
    }

    public Thiz requirement(Predicate<Src> requirement) {
        this.requirement = Objects.requireNonNull(requirement);
        return getThis();
    }

    public Thiz redirect(CommandNode<Src> redirect) {
        this.redirect = redirect;
        return getThis();
    }

    public Thiz modifier(RedirectModifier<Src> modifier) {
        this.modifier = modifier;
        return getThis();
    }

    public Thiz forked() {
        return fork(true);
    }

    public Thiz fork(boolean isFork) {
        this.isFork = isFork;
        return getThis();
    }

    protected <T extends CommandNode<Src>> T after(T obj) {
        obj.inheritCommandHandlerForChild(inheritCommandHandlerForChild);
        obj.description(description);
        return obj;
    }

    protected <T extends CommandNode<Src>> T mirror(T obj) {
        return mirror(obj, delegate);
    }


    public Thiz inheritCommandHandlerForChild() {
        return inheritCommandHandlerForChild(true);
    }

    public Thiz inheritCommandHandlerForChild(Boolean value) {
        inheritCommandHandlerForChild = value;
        return getThis();
    }

    public Thiz description(String description) {
        this.description = description;
        return getThis();
    }
}
