/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.arguments;

import com.kasukusakura.brigadier.command.CommandPreprocessHandler;
import com.kasukusakura.brigadier.command.RedirectModifier;
import com.kasukusakura.brigadier.command.context.CommandContextBuilder;
import com.kasukusakura.brigadier.command.context.ParsedCommandNode;
import com.kasukusakura.brigadier.command.context.StringRange;
import com.kasukusakura.brigadier.command.context.SuggestionContext;
import com.kasukusakura.brigadier.command.exceptions.CommandSyntaxException;
import com.kasukusakura.brigadier.command.suggestion.SuggestionInterpreter;
import com.kasukusakura.brigadier.command.suggestion.Suggestions;
import com.kasukusakura.brigadier.command.suggestion.SuggestionsBuilder;
import com.kasukusakura.brigadier.command.tree.ArgumentCommandNode;
import com.kasukusakura.brigadier.command.tree.CommandNode;
import com.kasukusakura.brigadier.reader.AnyValueReader;
import com.kasukusakura.brigadier.utils.EmptyStringUtil;
import com.kasukusakura.brigadier.utils.Pair;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class CLIComposedArgumentNode<Src> extends CommandNode<Src> {
    public static class IncompleteTokens<Src> {
        public boolean canContinueSequenceProcess;
        public Map<String, ArgumentCommandNode<Src, ?>> pending;
        public Deque<Pair<String, ArgumentCommandNode<Src, ?>>> sequences;

        @Override
        public String toString() {
            return "IncompleteTokens{" +
                    "canContinueSequenceProcess=" + canContinueSequenceProcess +
                    ", pending=" + pending +
                    ", sequences=" + sequences +
                    '}';
        }
    }

    private final List<Pair<String, ArgumentCommandNode<Src, ?>>> sequences = new ArrayList<>();
    protected final Map<String, ArgumentCommandNode<Src, ?>> arguments = new HashMap<>();

    public CLIComposedArgumentNode(
            CommandPreprocessHandler<Src> command,
            Predicate<Src> req,
            CommandNode<Src> redirect,
            RedirectModifier<Src> modifier,
            boolean fork
    ) {
        super(command, req, redirect, modifier, fork);
    }

    @Override
    public CommandNode<Src> register(CommandNode<Src> node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public void parse(CommandContextBuilder<Src> contextBuilder, AnyValueReader reader) throws CommandSyntaxException {
        {
            contextBuilder.withNode(new AfterArgumentPlaceholder(
                    arguments,
                    sequences.isEmpty() ? null : sequences.get(0).v
            ), StringRange.at(reader.getCursor()));
        }

        Map<String, ArgumentCommandNode<Src, ?>> pending = new HashMap<>(CLIComposedArgumentNode.this.arguments);
        Deque<Pair<String, ArgumentCommandNode<Src, ?>>> sequences = new LinkedList<>(CLIComposedArgumentNode.this.sequences);

        argumentProcess:
        {

            sequenceProcess:
            {
                while (true) {
                    if (sequences.isEmpty()) break argumentProcess; // Done

                    Object nextToken = reader.peekAny();
                    if (nextToken == null) break argumentProcess; // Done

                    String asStr = reader.toCharSequence(nextToken).toString();
                    if (asStr.startsWith("--")) {
                        ArgumentCommandNode<Src, ?> theArg = pending.get(asStr.substring(2));
                        if (theArg != null) {
                            break sequenceProcess; // enter argument parsing
                        }
                    }

                    Pair<String, ArgumentCommandNode<Src, ?>> pair = sequences.removeFirst();


                    pending.remove(pair.k);
                    pair.v.parse(contextBuilder, reader);


                    if (reader.canRead()) {
                        if (!Character.isWhitespace(reader.peekChar())) {
                            throw contextBuilder.getDispatcher().newCommandSyntaxException("Dispatcher excepted arguments");
                        }
                        reader.readChar();
                    }

                    {
                        Pair<String, ArgumentCommandNode<Src, ?>> nextNode = sequences.peekFirst();
                        contextBuilder.withNode(new AfterArgumentPlaceholder(
                                pending,
                                nextNode == null ? null : nextNode.v
                        ), StringRange.at(reader.getCursor()));
                    }

                }

            }

            // Argument parsing mode. Only entered if peek `--XXXX` in sequence parsing
            while (!pending.isEmpty()) {
                int originCursor = reader.getCursor();

                Object nextToken = reader.readAny();
                if (nextToken == null) {
                    reader.setCursor(originCursor);
                    break argumentProcess; // Done
                }
                String asStr = reader.toCharSequence(nextToken).toString();
                if (!asStr.startsWith("--")) {
                    reader.setCursor(originCursor);
                    break argumentProcess; // Not argument parsing flag
                }

                ArgumentCommandNode<Src, ?> theArg = pending.remove(asStr.substring(2));
                if (theArg == null) {
                    reader.setCursor(originCursor);
                    break argumentProcess; // Unknown flag
                }

                if (reader.canRead()) {
                    // --XXX value
                    //      |- this space

                    if (!Character.isWhitespace(reader.peekChar())) {
                        throw contextBuilder.getDispatcher().newCommandSyntaxException("Dispatcher excepted arguments");
                    }
                    reader.readChar();

                    contextBuilder.withNode(
                            new OptionCompletingPlaceholder(theArg),
                            StringRange.between(originCursor, reader.getCursor())
                    );

                    theArg.parse(contextBuilder, reader);
                } else {
                    contextBuilder.withNode(
                            new OptionCompletingPlaceholder(null),
                            StringRange.between(originCursor, reader.getCursor())
                    );
                    throw contextBuilder.getDispatcher().newCommandSyntaxException("Dispatcher excepted arguments");
                }

                if (reader.canRead()) { // Process the space after value
                    if (!Character.isWhitespace(reader.peekChar())) {
                        throw contextBuilder.getDispatcher().newCommandSyntaxException("Dispatcher excepted arguments");
                    }
                    reader.readChar();

                    contextBuilder.withNode(new AfterArgumentPlaceholder(
                            pending, null
                    ), StringRange.at(reader.getCursor()));
                }


            }

        }

        if (!pending.isEmpty()) {
            IncompleteTokens<Src> tokens = new IncompleteTokens<>();
            //noinspection ConstantValue
            tokens.canContinueSequenceProcess = pending.size() == sequences.size() && !sequences.isEmpty();
            tokens.pending = pending;
            tokens.sequences = sequences;
            contextBuilder.withArgument(null, IncompleteTokens.class, tokens);
        }

        contextBuilder.withCommand(getPreprocessHandler().parse(contextBuilder.dropSource()));
    }

    public CLIComposedArgumentNode<Src> register(ArgumentCommandNode<Src, ?> argument) {
        this.arguments.put(argument.getName(), argument);
        this.sequences.add(new Pair<>(argument.getName(), argument));
        return this;
    }

    @Override
    public CompletableFuture<Suggestions> listSuggestions(CommandContextBuilder<Src> context, SuggestionsBuilder builder) {
        return Suggestions.empty();
    }

    @Override
    public void renderUsageMessage(StringBuilder prefix, StringBuilder content, boolean addSplitter, boolean includeChild, boolean includeAllChild, Src source) {
        int prefixLength = prefix.length();
        int argsPadding = 0;

        content.append(prefix);
        for (Pair<String, ArgumentCommandNode<Src, ?>> arg : sequences) {
            content.append('<').append(arg.k).append('>').append(' ');
            argsPadding = Math.max(argsPadding, arg.k.length());
        }

        if (description() != null) {
            content.append(" -  ").append(description());
        }
        content.append('\n');

        for (Pair<String, ArgumentCommandNode<Src, ?>> arg : sequences) {
            EmptyStringUtil.appendEmptyString(content, prefixLength);

            content.append("--").append(arg.k);
            EmptyStringUtil.appendEmptyString(content, argsPadding - arg.k.length());
            content.append(" <").append(arg.k).append('>');
            if (arg.v.description() != null) {
                EmptyStringUtil.appendEmptyString(content, argsPadding - arg.k.length());

                EmptyStringUtil.appendEmptyString(content, 5);
                content.append(arg.v.description());
            }
            content.append('\n');
        }
    }

    public CLIComposedArgumentNode<Src> desc(String description) {
        description(description);
        return this;
    }

    private abstract class StubArgumentNode extends CommandNode<Src> {
        protected StubArgumentNode() {
            super(null, allowAll(), null, null, false);
        }

        @Override
        public CommandPreprocessHandler<Src> getPreprocessHandler() {
            return CLIComposedArgumentNode.this.getPreprocessHandler();
        }

        @Override
        public String getName() {
            return "";
        }

        @Override
        public void parse(CommandContextBuilder<Src> contextBuilder, AnyValueReader reader) throws CommandSyntaxException {
        }
    }


    private class AfterArgumentPlaceholder extends StubArgumentNode implements SuggestionInterpreter<Src> {
        private final Map<String, ArgumentCommandNode<Src, ?>> availableArguments;
        private final ArgumentCommandNode<Src, ?> nan;

        AfterArgumentPlaceholder(Map<String, ArgumentCommandNode<Src, ?>> availableArguments, ArgumentCommandNode<Src, ?> nextArgNode) {
            this.availableArguments = new HashMap<>();
            this.nan = nextArgNode;

            for (Map.Entry<String, ArgumentCommandNode<Src, ?>> entry : availableArguments.entrySet()) {
                this.availableArguments.put("--" + entry.getKey(), entry.getValue());
            }
        }

        @Override
        public CompletableFuture<Suggestions> listSuggestions(CommandContextBuilder<Src> context, SuggestionsBuilder builder) {
            String remaining = builder.remainingAsString;
            if ("--".startsWith(remaining) || remaining.startsWith("--")) {
                for (Map.Entry<String, ArgumentCommandNode<Src, ?>> entry : availableArguments.entrySet()) {
                    if (entry.getKey().startsWith(remaining)) {
                        builder.suggest(entry.getKey(), entry.getValue().description());
                    }
                }
                if (nan == null) return builder.buildFuture();


                return nan.listSuggestions(context, builder.newClean()).thenApply(rsp ->
                        Suggestions.merge(Arrays.asList(builder.build(), rsp))
                );
            }

            if (nan == null) return Suggestions.empty();
            return nan.listSuggestions(context, builder);
        }
    }

    private class OptionCompletingPlaceholder extends StubArgumentNode implements SuggestionInterpreter<Src> {
        private final ArgumentCommandNode<Src, ?> nan;

        private OptionCompletingPlaceholder(ArgumentCommandNode<Src, ?> nan) {
            this.nan = nan;
        }

        @Override
        public CompletableFuture<Suggestions> listSuggestions(CommandContextBuilder<Src> context, SuggestionsBuilder builder) {
            if (nan == null) return Suggestions.empty();
            return nan.listSuggestions(context, builder);
        }

        @Override
        public SuggestionContext<Src> findSuggestionContext(ParsedCommandNode<Src> last) {
            return new SuggestionContext<>(last.node, last.range.end);
        }
    }
}
