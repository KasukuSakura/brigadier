/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.tree;

import com.kasukusakura.brigadier.command.CommandHandler;
import com.kasukusakura.brigadier.command.CommandPreprocessHandler;
import com.kasukusakura.brigadier.command.RedirectModifier;
import com.kasukusakura.brigadier.command.context.CommandContextBuilder;
import com.kasukusakura.brigadier.command.exceptions.CommandSyntaxException;
import com.kasukusakura.brigadier.command.suggestion.Suggestions;
import com.kasukusakura.brigadier.command.suggestion.SuggestionsBuilder;
import com.kasukusakura.brigadier.reader.AnyValueReader;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public abstract class CommandNode<Src> {
    @SuppressWarnings("rawtypes")
    private static final Comparator<CommandNode> COMPARATOR = (a, b) -> {
        if (a == b) return 0;
        boolean aIsLiteral = a instanceof LiteralCommandNode;
        boolean bIsLiteral = b instanceof LiteralCommandNode;
        if (aIsLiteral && !bIsLiteral) return -1;
        if (!aIsLiteral && bIsLiteral) return 1;

        return a.getName().compareToIgnoreCase(b.getName());
    };

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <Src> Comparator<CommandNode<Src>> comparator() {
        return (Comparator) COMPARATOR;
    }

    private static final Predicate<?> ALLOW_ALL = s -> true;

    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> allowAll() {
        return (Predicate<T>) ALLOW_ALL;
    }

    Map<String, CommandNode<Src>> children = Collections.emptyMap();
    Map<String, ArgumentCommandNode<Src, ?>> arguments = Collections.emptyMap();
    Map<String, LiteralCommandNode<Src>> literals = Collections.emptyMap();


    private final Predicate<Src> requirement;
    private final CommandNode<Src> redirect;
    private final RedirectModifier<Src> modifier;
    private final boolean fork;
    private CommandPreprocessHandler<Src> preprocessHandler;
    private Boolean inheritCommandHandlerForChild = null;
    private String description;

    protected CommandNode(
            CommandPreprocessHandler<Src> command,
            Predicate<Src> req,
            CommandNode<Src> redirect,
            RedirectModifier<Src> modifier,
            boolean fork
    ) {
        this.preprocessHandler = command;
        this.requirement = req;
        this.fork = fork;
        this.modifier = modifier;
        this.redirect = redirect;
    }

    public CommandPreprocessHandler<Src> getPreprocessHandler() {
        return preprocessHandler;
    }

    public void setPreprocessHandler(CommandPreprocessHandler<Src> preprocessHandler) {
        this.preprocessHandler = preprocessHandler;
    }

    public void setCommandHandler(CommandHandler<Src> commandHandler) {
        this.preprocessHandler = commandHandler;
    }

    public Predicate<Src> getRequirement() {
        return requirement;
    }

    public CommandNode<Src> getRedirect() {
        return redirect;
    }

    public RedirectModifier<Src> getRedirectModifier() {
        return modifier;
    }

    public Boolean inheritCommandHandlerForChild() {
        return inheritCommandHandlerForChild;
    }

    /**
     * True - Keep current command handler into child
     * <p>
     * False - Remove command handler if child available
     * <p>
     * null - keep settings when this was parsed as child
     */
    public void inheritCommandHandlerForChild(Boolean value) {
        inheritCommandHandlerForChild = value;
    }

    public String description() {
        return description;
    }

    public void description(String description) {
        this.description = description;
    }

    public boolean isFork() {
        return fork;
    }

    public abstract String getName();

    public abstract void parse(CommandContextBuilder<Src> contextBuilder, AnyValueReader reader) throws CommandSyntaxException;

    void initMap() {
        if (Collections.emptyMap().equals(children)) {
            children = new HashMap<>();
            literals = new HashMap<>();
            arguments = new HashMap<>();
        }
    }

    public CommandNode<Src> register(CommandNode<Src> node) {
        if (node instanceof RootCommandNode) {
            throw new IllegalArgumentException("Registering RootCommandNode as child");
        }
        initMap();
        children.put(node.getName(), node);
        if (node instanceof LiteralCommandNode) {
            literals.put(node.getName(), (LiteralCommandNode<Src>) node);
        } else if (node instanceof ArgumentCommandNode) {
            arguments.put(node.getName(), (ArgumentCommandNode<Src, ?>) node);
        }
        return this;
    }

    public Collection<? extends CommandNode<Src>> getRelevantNodes(AnyValueReader input) {
        if (literals.size() > 0) {
            int cursor = input.getCursor();
            Object wordAny = input.readAny();
            input.setCursor(cursor);
            if (wordAny != null) {
                String word = String.valueOf(wordAny);
                LiteralCommandNode<Src> literal = literals.get(word);
                if (literal != null) return Collections.singleton(literal);
            }
        }
        if (arguments.isEmpty()) {
            return children.values();
        }
        return arguments.values();
    }


    public Collection<CommandNode<Src>> getChildren() {
        return children.values();
    }

    public abstract CompletableFuture<Suggestions> listSuggestions(CommandContextBuilder<Src> context, SuggestionsBuilder builder);

    public void renderUsageMessage(
            StringBuilder prefix,
            StringBuilder content,
            boolean addSplitter,
            boolean includeChild,
            boolean includeAllChild,
            Src source
    ) {
        if (source != null && !requirement.test(source)) {
            return;
        }

        int finalLength = prefix.length();
        if (this instanceof ArgumentCommandNode) {
            prefix.append('<').append(getName()).append('>');
        } else {
            prefix.append(getName());
        }

        if (!includeChild) {
            content.append(prefix);
            if (preprocessHandler == null || redirect != null) {
                content.append("  ...");
            }
            if (description != null) {
                content.append("  -  ").append(description);
            }
            content.append('\n');

            prefix.setLength(finalLength);
            return;
        }

        if (preprocessHandler != null || description != null || redirect != null) print:{
            if (preprocessHandler != null) {
                content.append(prefix);
                if (description != null) {
                    content.append("  -  ").append(description);
                }
                content.append('\n');
            } else if (description != null) {
                content.append(prefix).append(" ...  -  ").append(description).append('\n');
                break print;
            }

            if (redirect != null) {
                content.append(prefix).append(" ...");
                if (description != null) {
                    content.append("  -  ").append(description);
                }
                content.append('\n');
            }
        }


        if (addSplitter) {
            prefix.append(' ');
        }

        Iterator<CommandNode<Src>> iterator = children.values().stream().sorted(comparator()).iterator();
        while (iterator.hasNext()) {
            iterator.next().renderUsageMessage(prefix, content, true, includeAllChild, true, source);
        }

        prefix.setLength(finalLength);
    }
}
