/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.context;

import com.kasukusakura.brigadier.command.CommandDispatcher;
import com.kasukusakura.brigadier.command.CommandHandler;
import com.kasukusakura.brigadier.command.ParsedResults;
import com.kasukusakura.brigadier.command.RedirectModifier;
import com.kasukusakura.brigadier.command.suggestion.SuggestionInterpreter;
import com.kasukusakura.brigadier.command.tree.CommandNode;

import java.util.ArrayList;
import java.util.List;

class CommandContextBuilderImpl<Src> implements CommandContextBuilder<Src> {
    private final CommandDispatcher<Src> dispatcher;
    private Src source;
    private final CommandNode<Src> root;
    private StringRange range;

    private CommandContextBuilder<Src> child;
    private CommandHandler<Src> commandHandler;
    private CommandNode<Src> lastNode;
    private boolean inheritCommandHandlerForChild = false;
    private boolean doExecuteChild = true;
    private ParsedResults<Src> results;

    private final List<ParsedCommandNode<Src>> nodes = new ArrayList<>();
    private final List<MetadataValue> metadataValues = new ArrayList<>();
    private final List<MetadataValue> arguments = new ArrayList<>();

    CommandContextBuilderImpl(
            CommandDispatcher<Src> dispatcher,
            Src source,
            CommandNode<Src> rootNode,
            int start) {
        this.dispatcher = dispatcher;
        this.source = source;
        this.root = rootNode;
        this.range = StringRange.at(start);
        lastNode = rootNode;
    }

    @Override
    public Src getSource() {
        if (source == null) throw new UnsupportedOperationException();
        return source;
    }

    @Override
    public CommandContext<?> dropSource() {
        return copy().withSource(null);
    }

    @Override
    public CommandContextBuilder<Src> getChild() {
        return child;
    }

    @Override
    public List<ParsedCommandNode<Src>> getNodes() {
        return nodes;
    }

    @Override
    public List<MetadataValue> getAllMetadata() {
        return metadataValues;
    }

    @SuppressWarnings("unchecked")
    private static <T> T get(List<MetadataValue> values, String name, Class<T> type) {
        for (MetadataValue value : values) {
            if (name != null && name.equals(value.name)) return (T) value.value;
            if (type != null && type.equals(value.type)) return (T) value.value;
        }
        return null;
    }

    @Override
    public <T> T getMetadata(String name, Class<T> type) {
        return get(metadataValues, name, type);
    }

    @Override
    public <T> T getArgument(String name, Class<T> type) {
        return get(arguments, name, type);
    }

    @Override
    public CommandContextBuilderImpl<Src> copy() {
        CommandContextBuilderImpl<Src> newContext = new CommandContextBuilderImpl<>(dispatcher, source, root, 0);
        newContext.range = range;
        newContext.nodes.addAll(nodes);
        newContext.child = child;
        newContext.metadataValues.addAll(metadataValues);
        newContext.arguments.addAll(arguments);
        newContext.commandHandler = commandHandler;
        newContext.inheritCommandHandlerForChild = inheritCommandHandlerForChild;
        newContext.doExecuteChild = doExecuteChild;
        newContext.results = results;
        return newContext;
    }

    @Override
    public CommandContextBuilder<Src> withCommand(CommandHandler<Src> handler) {
        this.commandHandler = handler;
        return this;
    }

    @Override
    public CommandHandler<Src> getCommand() {
        return commandHandler;
    }

    @Override
    public StringRange getRange() {
        return range;
    }

    @Override
    public CommandContextBuilder<Src> withNode(CommandNode<Src> node, StringRange range) {
        this.nodes.add(new ParsedCommandNode<>(node, range));
        this.range = StringRange.encompassing(this.range, range);
        this.lastNode = node;
        return this;
    }

    @Override
    public CommandContextBuilder<Src> withChild(CommandContextBuilder<Src> child) {
        this.child = child;
        return this;
    }

    @Override
    public CommandContextBuilder<Src> withSource(Src source) {
        this.source = source;
        return this;
    }

    @Override
    public CommandContextBuilder<Src> withMetadata(String name, Class<?> type, Object metadata) {
        metadataValues.add(new MetadataValue(name, type, metadata));
        return this;
    }

    @Override
    public CommandContextBuilder<Src> withArgument(String name, Class<?> type, Object value) {
        arguments.add(new MetadataValue(name, type, value));
        return this;
    }

    @Override
    public RedirectModifier<Src> getRedirectModifier() {
        return lastNode.getRedirectModifier();
    }

    @Override
    public CommandContextBuilder<Src> copyFor(Src source) {
        return copy().withSource(source);
    }

    @Override
    public CommandDispatcher<Src> getDispatcher() {
        return dispatcher;
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public SuggestionContext<Src> findSuggestionContext(int cursor) {
        if (range.start > cursor) {
            throw new IllegalArgumentException("Can't find node before cursor");
        }

        if (range.end < cursor) {
            if (child != null) return child.findSuggestionContext(cursor);

            if (nodes.isEmpty()) {
                return new SuggestionContext<>(root, range.start);
            }

            ParsedCommandNode<Src> last = nodes.get(nodes.size() - 1);
            if (last.node instanceof SuggestionInterpreter) {
                return ((SuggestionInterpreter) last.node).findSuggestionContext(last);
            }
            return new SuggestionContext<>(last.node, last.range.end + 1);
        }

        CommandNode<Src> prev = root;
        for (ParsedCommandNode<Src> node : nodes) {
            if (node.range.start <= cursor && cursor <= node.range.end) {
                if (node.node instanceof SuggestionInterpreter) {
                    return ((SuggestionInterpreter) node.node).findSuggestionContext(node);
                }
                return new SuggestionContext<>(prev, node.range.start);
            }
            prev = node.node;
        }

        if (prev == null) throw new IllegalStateException("Can't find node before cursor");

        return new SuggestionContext<>(prev, range.start);
    }

    @Override
    public boolean inheritCommandHandlerForChild() {
        return inheritCommandHandlerForChild;
    }

    @Override
    public CommandContextBuilder<Src> inheritCommandHandlerForChild(boolean value) {
        inheritCommandHandlerForChild = value;
        return this;
    }

    @Override
    public boolean doExecuteChild() {
        return doExecuteChild;
    }

    @Override
    public CommandContextBuilder<Src> doExecuteChild(boolean value) {
        doExecuteChild = value;
        return this;
    }

    @Override
    public ParsedResults<Src> getResults() {
        return results;
    }

    @Override
    public CommandContextBuilder<Src> withResults(ParsedResults<Src> results) {
        this.results = results;
        return this;
    }
}
