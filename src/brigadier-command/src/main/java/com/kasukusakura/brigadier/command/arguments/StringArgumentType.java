/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.arguments;

import com.kasukusakura.brigadier.command.CommandDispatcher;
import com.kasukusakura.brigadier.command.exceptions.CommandSyntaxException;
import com.kasukusakura.brigadier.command.tree.ArgumentCommandNode;
import com.kasukusakura.brigadier.reader.AnyValueReader;

public class StringArgumentType implements ArgumentType<String> {
    public static final StringArgumentType INSTANCE = new StringArgumentType();

    @Override
    public String parse(CommandDispatcher<?> dispatcher, AnyValueReader reader) throws CommandSyntaxException {
        while (true) {
            char first = reader.peekChar();
            if (Character.isWhitespace(first)) {
                reader.readChar();
                continue;
            }
            if (first == '\u0000') {
                throw ArgumentCommandNode.incorrectArgument(reader, dispatcher, null);
            }

            if (first == '\"' || first == '\'') {
                reader.readChar();
                return readUntil(reader, dispatcher, first);
            }
            Object value = reader.readAny();
            if (value == null) throw dispatcher.newCommandSyntaxException("Excepted an argument");

            return value.toString();
        }
    }

    private String readUntil(AnyValueReader reader, CommandDispatcher<?> dispatcher, char terminator) {
        StringBuilder sb = new StringBuilder();

        boolean escaped = false;
        while (true) {
            char next = reader.peekChar();
            if (next == '\u0000') {
                throw dispatcher.newCommandSyntaxException("Expected end of quote");
            }
            if (escaped) {
                if (next == terminator || next == '\\') {
                    sb.append(next);
                    escaped = false;
                    reader.readChar();
                } else {
                    throw new RuntimeException("readInvalidEscape");
                }
            } else if (next == '\\') {
                escaped = true;
                reader.readChar();
            } else if (next == terminator) {
                reader.readChar();
                return sb.toString();
            } else {
                sb.append(reader.readChar());
            }
        }
    }
}
