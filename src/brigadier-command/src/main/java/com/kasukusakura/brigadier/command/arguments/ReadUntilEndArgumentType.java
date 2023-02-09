/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.arguments;

import com.kasukusakura.brigadier.command.CommandDispatcher;
import com.kasukusakura.brigadier.command.exceptions.CommandSyntaxException;
import com.kasukusakura.brigadier.reader.AnyValueReader;

public class ReadUntilEndArgumentType implements ArgumentType<CharSequence> {

    @Override
    public CharSequence parse(CommandDispatcher<?> dispatcher, AnyValueReader reader) throws CommandSyntaxException {
        return reader.fetchContent(reader.getCursor(), Integer.MAX_VALUE);
    }
}
