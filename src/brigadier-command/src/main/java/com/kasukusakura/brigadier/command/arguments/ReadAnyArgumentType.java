/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command.arguments;

import com.kasukusakura.brigadier.command.CommandDispatcher;
import com.kasukusakura.brigadier.command.exceptions.CommandSyntaxException;
import com.kasukusakura.brigadier.reader.AnyValueReader;

public class ReadAnyArgumentType implements ArgumentType<Object> {
    public static final ReadAnyArgumentType INSTANCE = new ReadAnyArgumentType();


    @Override
    public Object parse(CommandDispatcher<?> dispatcher, AnyValueReader reader) throws CommandSyntaxException {
        Object rsp = reader.readAny();
        if (rsp == null) throw dispatcher.newCommandSyntaxException("Expected an argument");
        return rsp;
    }
}
