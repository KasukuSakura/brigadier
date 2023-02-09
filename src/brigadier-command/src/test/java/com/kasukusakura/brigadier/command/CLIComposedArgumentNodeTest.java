/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command;

import com.kasukusakura.brigadier.command.arguments.CLIComposedArgumentNode;
import com.kasukusakura.brigadier.command.arguments.EnumArgumentType;
import com.kasukusakura.brigadier.command.arguments.ReadAnyArgumentType;
import com.kasukusakura.brigadier.command.tree.CommandNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CLIComposedArgumentNodeTest {
    CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();

    @BeforeAll
    void init() {
        dispatcher.registerBuilder()
                .literal("cmd")
                .register(
                        new CLIComposedArgumentNode<Object>(
                                s -> {
                                    return v -> {
                                        System.out.println("arg1: " + v.getArgument("arg1", null));
                                        System.out.println("arg2: " + v.getArgument("arg2", null));
                                        System.out.println("arg3: " + v.getArgument("arg3", null));
                                        System.out.println("ICToken: " + v.getArgument(null, CLIComposedArgumentNode.IncompleteTokens.class));
                                    };
                                },
                                CommandNode.allowAll(),
                                null, null, false
                        ).register(dispatcher.newArgument()
                                .name("arg1")
                                .type(new EnumArgumentType<>(
                                        List.of("va1", "va2", "--vel")
                                ))
                                .build()
                        ).register(dispatcher.newArgument()
                                .name("arg2")
                                .type(new EnumArgumentType<>(
                                        List.of("vb1", "vb2", "--vel2")
                                ))
                                .build()
                        ).register(dispatcher.newArgument()
                                .name("arg3")
                                .type(ReadAnyArgumentType.INSTANCE)
                                .build()
                        )
                )
                .build();
    }

    @Test
    void assertSuggestion() {
        var a = new Object() {
            void suggest(Set<String> excepted, String command, int cursor) {
                var sugs = new HashSet<String>();

                var parsed = dispatcher.parse(command, this);
                var suggestions = dispatcher.getCompletionSuggestions(
                        parsed,
                        cursor == -1 ? command.length() : cursor
                ).join();
                for (var s : suggestions.suggestions) {
                    sugs.add(s.text);
                }

                Assertions.assertEquals(excepted, sugs);
            }
        };

        a.suggest(Set.of(), "cmd", -1);
        a.suggest(Set.of("--arg1", "--arg2", "--arg3", "va1", "va2", "--vel"), "cmd ", -1);
        a.suggest(Set.of("--arg1", "--arg2", "--arg3", "--vel"), "cmd --", -1);
        a.suggest(Set.of("--vel"), "cmd --v", -1);
        a.suggest(Set.of(), "cmd --vel", -1);
        a.suggest(Set.of("--arg2", "--arg3", "--vel2", "vb1", "vb2"), "cmd --vel ", -1);


        a.suggest(Set.of("--arg2", "--arg3", "vb1", "vb2", "--vel2"), "cmd --vel ", -1);
        a.suggest(Set.of(), "cmd --vel ", 9);


        a.suggest(Set.of("--arg1", "--arg2"), "cmd --arg3 Hello ", -1);
        a.suggest(Set.of("--arg1", "--arg2"), "cmd --arg3 Hello --arg", -1);
        a.suggest(Set.of(), "cmd --arg3 Hello --arg2", -1);
        a.suggest(Set.of("--vel2", "vb1", "vb2"), "cmd --arg3 Hello --arg2 ", -1);
        a.suggest(Set.of("vb1", "vb2"), "cmd --arg3 Hello --arg2 v", -1);
    }

    @Test
    void test() {
        dispatcher.execute("cmd va1 vb2 v3", this);
    }
}
