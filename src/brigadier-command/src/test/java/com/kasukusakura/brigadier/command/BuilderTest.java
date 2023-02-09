/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command;

import com.kasukusakura.brigadier.command.arguments.ArgumentType;
import com.kasukusakura.brigadier.command.builder.RootCommandNodeBuilder;
import com.kasukusakura.brigadier.command.exceptions.CommandSyntaxException;
import com.kasukusakura.brigadier.reader.AnyValueReader;
import org.junit.jupiter.api.Test;

public class BuilderTest {
    @Test
    void testBuilder() {
        var builder = new RootCommandNodeBuilder<>();

        builder.addLiteral($$$ -> {
            $$$.literal("testc");
            $$$.command(s -> {
                System.out.println("!!");
            });

            $$$.addArgument($$$$ -> {
                $$$$.name("tst").type(new ArgumentType<Object>() {
                    @Override
                    public Object parse(CommandDispatcher<?> dispatcher, AnyValueReader reader) throws CommandSyntaxException {
                        return reader.readAny();
                    }
                }).command(s -> {
                    System.out.println("!!");
                });
            });
        });

        var cmap = builder.build();

        System.out.println(cmap);
    }
}
