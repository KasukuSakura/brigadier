/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.suggestion;

import com.kasukusakura.brigadier.command.context.ParsedCommandNode;
import com.kasukusakura.brigadier.command.context.SuggestionContext;

/**
 * Used for CommandNode means that use current command node for completion not prev one
 */
public interface SuggestionInterpreter<Src> {

    default SuggestionContext<Src> findSuggestionContext(ParsedCommandNode<Src> last) {
        return new SuggestionContext<>(last.node, last.range.start);
    }
}
