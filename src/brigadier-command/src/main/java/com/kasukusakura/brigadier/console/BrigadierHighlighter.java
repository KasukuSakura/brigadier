/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.console;

import com.kasukusakura.brigadier.command.CommandDispatcher;
import com.kasukusakura.brigadier.command.ParsedResults;
import com.kasukusakura.brigadier.command.context.CommandContextBuilder;
import com.kasukusakura.brigadier.command.context.ParsedCommandNode;
import com.kasukusakura.brigadier.command.tree.ArgumentCommandNode;
import com.kasukusakura.brigadier.command.tree.LiteralCommandNode;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.regex.Pattern;

public class BrigadierHighlighter<Src> implements Highlighter {
    private final CommandDispatcher<Src> dispatcher;
    private final Src theConsole;
    static final int[] COLORS = new int[]{
            AttributedStyle.CYAN,
            AttributedStyle.YELLOW,
            AttributedStyle.GREEN,
            AttributedStyle.MAGENTA,
            AttributedStyle.BLUE,
    };

    public BrigadierHighlighter(CommandDispatcher<Src> dispatcher, Src theConsole) {
        this.dispatcher = dispatcher;
        this.theConsole = theConsole;
    }

    @Override
    public AttributedString highlight(LineReader reader, String buffer) {
        if (buffer.isEmpty()) {
            return AttributedString.EMPTY;
        }

        ParsedResults<Src> results = dispatcher.parse(buffer, theConsole);

        int components = 0;
        int pos = 0;
        AttributedStringBuilder builder = new AttributedStringBuilder();

        CommandContextBuilder<Src> context = results.context;
        while (context != null) {
            for (ParsedCommandNode<Src> node : context.getNodes()) {
                if (node.range.start >= buffer.length()) break;

                final int start = node.range.start;
                final int end = node.range.end;

                if (start > pos) {
                    builder.style(AttributedStyle.DEFAULT);
                    builder.append(buffer, pos, start);
                }

                if (node.node instanceof LiteralCommandNode) {
                    builder.style(AttributedStyle.DEFAULT);
                    builder.append(buffer, start, end);
                } else {
                    builder.style(AttributedStyle.DEFAULT.foreground(COLORS[components]));
                    builder.append(buffer, start, end);

                    if (node.node instanceof ArgumentCommandNode) {
                        if (++components >= COLORS.length) {
                            components = 0;
                        }
                    }
                }
                pos = end;
            }

            components = 0;
            context = context.getChild();
        }

        if (pos < buffer.length()) {
            builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
            builder.append(buffer, pos, buffer.length());
        }

        if (results.reader.canRead() || (results.exceptions != null && !results.exceptions.isEmpty())) {

            String message;

            fetchMsg:
            {
                if (results.exceptions != null && !results.exceptions.isEmpty()) {
                    if (results.exceptions.size() == 1) {
                        message = results.exceptions.values().iterator().next().getLocalizedMessage();
                        break fetchMsg;
                    }
                }

                //noinspection DataFlowIssue
                if (results.context.getRange().isEmpty()) {
                    message = "Unknown command: " + results.reader.fetchContent(0, Integer.MAX_VALUE);
                } else {
                    message = ArgumentCommandNode.incorrectArgumentMessage(results.reader);
                }
            }

            builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)).append('\n').append(message);
        }

        return builder.toAttributedString();
    }

    @Override
    public void setErrorPattern(Pattern errorPattern) {
    }

    @Override
    public void setErrorIndex(int errorIndex) {
    }

}
