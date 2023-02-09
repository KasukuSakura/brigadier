/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command;

import com.kasukusakura.brigadier.command.arguments.CLIComposedArgumentNode;
import com.kasukusakura.brigadier.command.builder.ArgumentCommandNodeBuilder;
import com.kasukusakura.brigadier.command.builder.LiteralCommandNodeBuilder;
import com.kasukusakura.brigadier.command.context.CommandContextBuilder;
import com.kasukusakura.brigadier.command.context.ParsedCommandNode;
import com.kasukusakura.brigadier.command.context.StringRange;
import com.kasukusakura.brigadier.command.context.SuggestionContext;
import com.kasukusakura.brigadier.command.exceptions.CommandNotFoundException;
import com.kasukusakura.brigadier.command.exceptions.CommandSyntaxException;
import com.kasukusakura.brigadier.command.suggestion.SuggestionInterpreter;
import com.kasukusakura.brigadier.command.suggestion.Suggestions;
import com.kasukusakura.brigadier.command.suggestion.SuggestionsBuilder;
import com.kasukusakura.brigadier.command.tree.ArgumentCommandNode;
import com.kasukusakura.brigadier.command.tree.CommandNode;
import com.kasukusakura.brigadier.command.tree.LiteralCommandNode;
import com.kasukusakura.brigadier.command.tree.RootCommandNode;
import com.kasukusakura.brigadier.reader.AnyValueReader;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class CommandDispatcher<Src> {
    public final RootCommandNode<Src> root;
    public boolean enableStackTrace = true;

    public CommandDispatcher() {
        this(new RootCommandNode<>());
    }

    public CommandDispatcher(RootCommandNode<Src> root) {
        this.root = Objects.requireNonNull(root, "root");
    }

    public ParsedResults<Src> parse(AnyValueReader reader, Src source) {
        return parse(reader, source, null);
    }

    public ParsedResults<Src> parse(String command, Src source) {
        return parse(new AnyValueReader(command), source);
    }

    public ParsedResults<Src> parse(AnyValueReader reader, Src source, Consumer<CommandContextBuilder<Src>> setup) {
        final CommandContextBuilder<Src> context = CommandContextBuilder.newBuilder(this, source, root, reader.getCursor());
        if (setup != null) setup.accept(context);
        return parse(root, reader, context);
    }

    public ParsedResults<Src> parse(CommandNode<Src> startNode, AnyValueReader originalReader, CommandContextBuilder<Src> contextSoFar) {
        Map<CommandNode<Src>, CommandSyntaxException> exceptions = null;
        final List<ParsedResults<Src>> potentials = new ArrayList<>();

        final Src source = contextSoFar.getSource();

        for (CommandNode<Src> child : startNode.getRelevantNodes(originalReader)) {
            if (!child.getRequirement().test(source)) continue;

            CommandContextBuilder<Src> context = contextSoFar.copy();
            AnyValueReader reader = originalReader.copy();

            if (child instanceof CLIComposedArgumentNode) { // special implementation
                try {
                    child.parse(context, reader);
                    return new ParsedResults<>(context, reader, null);
                } catch (CommandSyntaxException exception) {

                    if (exceptions == null) exceptions = new HashMap<>();
                    exceptions.put(child, exception);

                    return new ParsedResults<>(context, reader, exceptions);
                }
            }

            try {
                try {
                    child.parse(context, reader);
                } catch (CommandSyntaxException syntaxException) {
                    throw syntaxException;
                } catch (Exception runtimeException) {
                    throw newCommandSyntaxException(runtimeException);
                }

                if (reader.canRead()) {
                    if (!Character.isWhitespace(reader.peekChar())) {
                        throw newCommandSyntaxException("Dispatcher excepted arguments");
                    }
                }

                CommandPreprocessHandler<Src> preprocessed = child.getPreprocessHandler();
                Boolean newSetting = child.inheritCommandHandlerForChild();

                if (newSetting != null) context.inheritCommandHandlerForChild(newSetting);

                if (preprocessed != null) {
                    context.withCommand(preprocessed.parse(context.dropSource()));
                } else if (!context.inheritCommandHandlerForChild()) {
                    context.withCommand(null);
                }

            } catch (CommandSyntaxException syntaxException) {
                if (exceptions == null) exceptions = new LinkedHashMap<>();

                exceptions.put(child, syntaxException);
                continue;
            }

            potentialsCheck:
            {
                if (reader.canRead()) {
                    reader.readChar(); // splitter

                    if (child.getRedirect() != null) {
                        final CommandContextBuilder<Src> childContext = context.newBuilderForChild(this, source, child.getRedirect(), reader.getCursor());
                        final ParsedResults<Src> parse = parse(child.getRedirect(), reader, childContext);
                        context.withChild(parse.context);
                        return new ParsedResults<>(context, parse.reader, parse.exceptions);
                    } else {
                        ParsedResults<Src> parsed = parse(child, reader, context);
                        potentials.add(parsed);
                        break potentialsCheck;
                    }
                }

                potentials.add(new ParsedResults<>(context, reader, exceptions));
            }
        }

        if (!potentials.isEmpty()) {
            potentials.sort((a, b) -> {
                if (!a.reader.canRead() && b.reader.canRead()) {
                    return -1;
                }
                if (a.reader.canRead() && !b.reader.canRead()) {
                    return 1;
                }
                if (a.exceptions.isEmpty() && !b.exceptions.isEmpty()) {
                    return -1;
                }
                if (!a.exceptions.isEmpty() && b.exceptions.isEmpty()) {
                    return 1;
                }
                return 0;
            });
            return potentials.get(0);
        }

        return new ParsedResults<>(contextSoFar, originalReader, exceptions);
    }

    public CommandSyntaxException newCommandSyntaxException(String message) {
        return newCommandSyntaxException(message, null);
    }

    public CommandSyntaxException newCommandSyntaxException(Throwable cause) {
        return newCommandSyntaxException(null, cause);
    }

    public CommandSyntaxException newCommandSyntaxException(String message, Throwable cause) {
        return new CommandSyntaxException(message, cause, true, enableStackTrace);
    }

    public CommandNotFoundException newCommandNotFoundException(String message) {
        return newCommandNotFoundException(message, null);
    }

    public CommandNotFoundException newCommandNotFoundException(Throwable cause) {
        return newCommandNotFoundException(null, cause);
    }

    public CommandNotFoundException newCommandNotFoundException(String message, Throwable cause) {
        return new CommandNotFoundException(message, cause, true, enableStackTrace);
    }

    public void execute(ParsedResults<Src> results) throws CommandSyntaxException {
        if (results.reader.canRead() || (results.exceptions != null && !results.exceptions.isEmpty())) {
            if (results.exceptions != null && !results.exceptions.isEmpty()) {
                if (results.exceptions.size() == 1) {
                    throw results.exceptions.values().iterator().next();
                }
            }
            if (results.context.getRange().isEmpty()) {
                throw newCommandSyntaxException("Unknown command: " + results.reader.fetchContent(0, Integer.MAX_VALUE));
            }
            throw ArgumentCommandNode.incorrectArgument(results.reader, this, null);
        }


        List<CommandContextBuilder<Src>> contexts = Collections.singletonList(results.context.withResults(results));
        List<CommandContextBuilder<Src>> next = null;
        boolean foundCommand = false;

        while (contexts != null) {
            for (CommandContextBuilder<Src> context : contexts) {
                CommandContextBuilder<Src> child = context.getChild();
                if (child != null && context.doExecuteChild()) {
                    if (child.hasNodes()) {
                        RedirectModifier<Src> modifier = context.getRedirectModifier();
                        if (modifier == null) {
                            if (next == null) next = new ArrayList<>(1);

                            next.add(child.copyFor(context.getSource()).withResults(results));
                        } else {
                            Collection<Src> redirectedExecutors = modifier.apply(context);
                            if (redirectedExecutors != null && !redirectedExecutors.isEmpty()) {
                                if (next == null) next = new ArrayList<>(redirectedExecutors.size());

                                for (Src source : redirectedExecutors) {
                                    next.add(child.copyFor(source).withResults(results));
                                }
                            }
                        }
                    }
                } else if (context.getCommand() != null) {
                    foundCommand = true;

                    context.getCommand().process(context);
                } else {
                    throw newCommandNotFoundException("Unknown or incomplete command: " + results.reader.fetchContent(0, Integer.MAX_VALUE));
                }
            }

            contexts = next;
            next = null;
        }

        if (!foundCommand) {
            throw newCommandNotFoundException("Unknown or incomplete command: " + results.reader.fetchContent(0, Integer.MAX_VALUE));
        }

    }

    public CompletableFuture<Suggestions> getCompletionSuggestions(ParsedResults<Src> results, int cursor) {
        CommandContextBuilder<Src> context = results.context;

        SuggestionContext<Src> nodeBeforeCursor = context.findSuggestionContext(cursor);
        CommandNode<Src> parent = nodeBeforeCursor.parent;
        int start = Math.min(nodeBeforeCursor.startPos, cursor);

        CharSequence currentValue = results.reader.fetchContent(start, cursor);
        String remainingAsString = currentValue.toString();
        String remainingAsStringLowercase = remainingAsString.toLowerCase();

        @SuppressWarnings("unchecked") CompletableFuture<Suggestions>[] futures = new CompletableFuture[
                parent instanceof SuggestionInterpreter
                        ? 1
                        : parent.getChildren().size()
                ];

        int i = 0;
        for (final CommandNode<Src> node : (
                parent instanceof SuggestionInterpreter
                        ? Collections.singletonList(parent)
                        : parent.getChildren()
        )) {
            CompletableFuture<Suggestions> future = null;
            try {
                future = node.listSuggestions(results.context, new SuggestionsBuilder(
                        currentValue, remainingAsString, remainingAsStringLowercase, start
                ));
            } catch (CommandSyntaxException ignored) {
            }

            futures[i++] = future == null ? Suggestions.empty() : future;
        }

        final CompletableFuture<Suggestions> result = new CompletableFuture<>();
        CompletableFuture.allOf(futures).thenRun(() -> {
            final List<Suggestions> suggestions = new ArrayList<>();
            for (final CompletableFuture<Suggestions> future : futures) {
                suggestions.add(future.join());
            }
            result.complete(Suggestions.merge(suggestions));
        });

        return result;
    }

    public LiteralCommandNodeBuilder<Src> newLiteral() {
        return new LiteralCommandNodeBuilder<>();
    }

    public ArgumentCommandNodeBuilder<Src, ?> newArgument() {
        return new ArgumentCommandNodeBuilder<>();
    }

    public LiteralCommandNodeBuilder<Src> registerBuilder() {
        return new LiteralCommandNodeBuilder<Src>() {
            @Override
            public LiteralCommandNode<Src> build() {
                LiteralCommandNode<Src> commandNode = super.build();
                root.register(commandNode);
                return commandNode;
            }
        };
    }

    public void execute(String command, Src source) {
        execute(parse(new AnyValueReader(command), source));
    }

    public StringBuilder renderHelpUsage(StringBuilder prefix, CommandNode<Src> node, Src source) {
        StringBuilder content = new StringBuilder();
        boolean isRoot = node instanceof RootCommandNode;
        node.renderUsageMessage(prefix, content, !isRoot, true, !isRoot, source);
        return content;
    }

    public StringBuilder renderHelpUsage(CommandNode<Src> node, Src source) {
        return renderHelpUsage(new StringBuilder(), node, source);
    }

    public StringBuilder renderHelpUsage(ParsedResults<Src> results) {
        return renderHelpUsage(new StringBuilder(), results);
    }

    public StringBuilder renderHelpUsage(StringBuilder prefix, ParsedResults<Src> results) {
        StringRange startRange = results.context.getRange();
        CommandNode<Src> theRoot = this.root;
        int fetchEnd = startRange.start;
        CommandContextBuilder<Src> lastContext = results.context;
        while (true) {
            CommandContextBuilder<Src> next = lastContext.getChild();
            if (next == null) {
                break;
            }
            fetchEnd = next.getRange().start;
            theRoot = lastContext.getNodes().get(lastContext.getNodes().size() - 1).node.getRedirect();
            lastContext = next;

        }

        prefix.append(results.reader.fetchContent(startRange.start, fetchEnd));

        List<ParsedCommandNode<Src>> nodes = lastContext.getNodes();
        return renderHelpUsage(prefix, nodes.isEmpty() ? theRoot : nodes.get(nodes.size() - 1).node, results.context.getSource());
    }
}
