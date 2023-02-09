/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.console;

import com.kasukusakura.brigadier.command.CommandDispatcher;
import com.kasukusakura.brigadier.command.ParsedResults;
import com.kasukusakura.brigadier.command.suggestion.Suggestion;
import com.kasukusakura.brigadier.command.suggestion.Suggestions;
import com.kasukusakura.brigadier.reader.AnyValueReader;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BrigadierCompleter<Src> implements Completer {
    private final CommandDispatcher<Src> dispatcher;
    private final Src theConsole;

    public BrigadierCompleter(CommandDispatcher<Src> dispatcher, Src theConsole) {
        this.dispatcher = dispatcher;
        this.theConsole = theConsole;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        ParsedResults<Src> results = dispatcher.parse(new AnyValueReader(line.line()), theConsole);
        CompletableFuture<Suggestions> completionSuggestions = dispatcher.getCompletionSuggestions(results, line.cursor());

        Suggestions suggestions = completionSuggestions.join();
        for (Suggestion sug : suggestions.suggestions) {
            candidates.add(new Candidate(
                    sug.text, sug.text, null, sug.tooltip, null, null, false
            ));
        }
    }
}
