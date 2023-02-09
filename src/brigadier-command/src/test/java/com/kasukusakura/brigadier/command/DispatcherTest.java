/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.command;

import com.kasukusakura.brigadier.command.arguments.EnumArgumentType;
import com.kasukusakura.brigadier.command.arguments.ReadAnyArgumentType;
import com.kasukusakura.brigadier.command.arguments.StringArgumentType;
import com.kasukusakura.brigadier.command.builder.LiteralCommandNodeBuilder;
import com.kasukusakura.brigadier.command.context.CommandContext;
import com.kasukusakura.brigadier.command.exceptions.CommandSyntaxException;
import com.kasukusakura.brigadier.reader.AnyValueReader;
import org.junit.jupiter.api.*;

import java.util.*;

@SuppressWarnings("CodeBlock2Expr")
public class DispatcherTest {
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class SuggestionsTest {
        CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();


        @BeforeAll
        void init() {
            dispatcher.registerBuilder().literal("command1").build();
            dispatcher.registerBuilder().literal("command2").build();

            Map<String, String> enums = new HashMap<>();
            for (String s : new String[]{
                    "e1", "e2", "e3", "e4",
                    "s1", "s2", "s3", "s4",
            }) {
                enums.put(s, s);
            }
            dispatcher.registerBuilder()
                    .literal("args")
                    .addArgument(eval -> eval.name("val").type(new EnumArgumentType<>(enums)))
                    .build();

            dispatcher.registerBuilder()
                    .literal("child")
                    .redirect(dispatcher.root)
                    .build();
        }

        private Set<String> suggest(String line, int cursor) {
            var parsed = dispatcher.parse(new AnyValueReader(line), this);
            var sgs = dispatcher.getCompletionSuggestions(parsed, cursor == -1 ? line.length() : cursor).join();
            var rsp = new HashSet<String>();
            for (var sug : sgs.suggestions) {
                rsp.add(sug.text);
            }
            return rsp;
        }

        @Test
        void assertRootSuggest() {
            Assertions.assertEquals(Set.of("command1", "command2", "args", "child"), suggest("", -1));
            Assertions.assertEquals(Set.of("command1", "command2", "child"), suggest("c", -1));
            Assertions.assertEquals(Set.of("command1", "command2"), suggest("com", -1));
            Assertions.assertEquals(Set.of(), suggest("command1", -1));
            Assertions.assertEquals(Set.of("command1", "command2"), suggest("command1", 3));

            Assertions.assertEquals(Set.of("args"), suggest("a", -1));
            Assertions.assertEquals(Set.of(), suggest("args", -1));
        }

        @Test
        void assertEnumMap() {
            Assertions.assertEquals(Set.of("s1", "s2", "s3", "s4", "e1", "e2", "e3", "e4"), suggest("args ", -1));
            Assertions.assertEquals(Set.of("s1", "s2", "s3", "s4"), suggest("args s", -1));
            Assertions.assertEquals(Set.of("e1", "e2", "e3", "e4"), suggest("args e", -1));
            Assertions.assertEquals(Set.of(), suggest("args e4", -1));
            Assertions.assertEquals(Set.of(), suggest("args e5", -1));
        }

        @Test
        void assertChildSuggest() {
            Assertions.assertEquals(Set.of(), suggest("child", -1));
            Assertions.assertEquals(Set.of("command1", "command2", "args", "child"), suggest("child ", -1));
            Assertions.assertEquals(Set.of("command1", "command2"), suggest("child com", -1));
            Assertions.assertEquals(Set.of("child"), suggest("child com", 2));
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class ExecutionTest {
        CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();

        Object response;
        CommandContext<Object> lastContext;

        CommandHandler<Object> response(Object value) {
            return s -> {
                lastContext = s;
                response = value;
            };
        }

        Object response() {
            var rsp = response;
            response = null;
            return rsp;
        }

        @BeforeAll
        void init() {
            dispatcher.registerBuilder()
                    .literal("cmd1")
                    .command(response("cmd1 empty arg"))

                    .addArgument(arg -> arg.name("myarg")
                            .type(StringArgumentType.INSTANCE)
                            .command(response("cmd1 with arg"))
                    )

                    .inheritCommandHandlerForChild()
                    .addLiteral(lit -> lit.literal("sublit"))
                    .addLiteral(lit -> lit.literal("sublit2").command(response("cmd1 sublit2")))
                    .addLiteral(lit -> lit.literal("notyetcomplete").inheritCommandHandlerForChild(false))
                    .build();

            dispatcher.registerBuilder()
                    .literal("child")
                    .redirect(dispatcher.root)
                    .modifier(s -> Collections.singleton(dispatcher))
                    .build();

            dispatcher.registerBuilder()
                    .literal("child2")
                    .command(response("child2"))
                    .redirect(dispatcher.root)
                    .modifier(s -> Collections.singleton(dispatcher))
                    .build();
        }

        @Test
        void testCmd1Executing() {
            dispatcher.execute("cmd1", this);
            Assertions.assertEquals("cmd1 empty arg", response());


            dispatcher.execute("cmd1 sublit", this);
            Assertions.assertEquals("cmd1 empty arg", response());
            Assertions.assertEquals(2, lastContext.getNodes().size());

            dispatcher.execute("cmd1 sublit2", this);
            Assertions.assertEquals("cmd1 sublit2", response());


            Assertions.assertThrows(CommandSyntaxException.class, () -> {
                dispatcher.execute("child cmd1 notyetcomplete", this);
            }).printStackTrace(System.out);

            Assertions.assertThrows(CommandSyntaxException.class, () -> {
                dispatcher.execute("c", this);
            }).printStackTrace(System.out);
        }

        @Test
        void testChildExecuting() {
            dispatcher.execute("child cmd1", this);
            Assertions.assertEquals("cmd1 empty arg", response());
            Assertions.assertEquals(dispatcher, lastContext.getSource());


            dispatcher.execute("child cmd1 sublit", this);
            Assertions.assertEquals("cmd1 empty arg", response());
            Assertions.assertEquals(2, lastContext.getNodes().size());
            Assertions.assertEquals(dispatcher, lastContext.getSource());


            dispatcher.execute("child cmd1 sublit2", this);
            Assertions.assertEquals("cmd1 sublit2", response());
            Assertions.assertEquals(dispatcher, lastContext.getSource());

            Assertions.assertThrows(CommandSyntaxException.class, () -> {
                dispatcher.execute("child cmd1 notyetcomplete", this);
            }).printStackTrace(System.out);


            dispatcher.execute("child2", this);
            Assertions.assertEquals("child2", response());


            dispatcher.execute("child2 cmd1", this);
            Assertions.assertEquals("cmd1 empty arg", response());
            Assertions.assertEquals(dispatcher, lastContext.getSource());


            dispatcher.execute("child2 cmd1 sublit", this);
            Assertions.assertEquals("cmd1 empty arg", response());
            Assertions.assertEquals(2, lastContext.getNodes().size());
            Assertions.assertEquals(dispatcher, lastContext.getSource());


            dispatcher.execute("child2 cmd1 sublit2", this);
            Assertions.assertEquals("cmd1 sublit2", response());
            Assertions.assertEquals(dispatcher, lastContext.getSource());

            Assertions.assertThrows(CommandSyntaxException.class, () -> {
                dispatcher.execute("child2 cmd1 notyetcomplete", this);
            }).printStackTrace(System.out);


            Assertions.assertThrows(CommandSyntaxException.class, () -> {
                dispatcher.execute("child", this);
            }).printStackTrace(System.out);

            Assertions.assertThrows(CommandSyntaxException.class, () -> {
                dispatcher.execute("child c", this);
            }).printStackTrace(System.out);
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class UsageRenderTest {
        CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        CommandHandler<Object> noop = s -> {
        };

        @BeforeAll
        void initDispatcher() {
            dispatcher.registerBuilder()
                    .literal("mycommand").description("My Command")
                    .addLiteral(subcmd -> {
                        subcmd.literal("subcmd").description("Sub Command").command(noop);
                    })
                    .addLiteral(subcmd -> {
                        subcmd.literal("subcmdn").command(noop);
                    })
                    .build();

            dispatcher.registerBuilder()
                    .literal("redirect")
                    .redirect(dispatcher.root)
                    .description("Redirect command to root")
                    .build();

            dispatcher.registerBuilder()
                    .literal("myc2")
                    .addLiteral(subc -> {
                        subc.literal("hello").description("!").command(noop);
                    })
                    .addLiteral(subc -> {
                        subc.literal("nousage").command(noop);
                    })
                    .build();

            dispatcher.registerBuilder()
                    .literal("args")
                    .description("Args Command")

                    .addLiteral(arg1 -> {
                        arg1.literal("arg1").addArgument(farg -> {
                            farg.name("farg").type(ReadAnyArgumentType.INSTANCE)
                                    .description("FArg")
                                    .addArgument(sarg -> {
                                        sarg.name("SArg").type(ReadAnyArgumentType.INSTANCE)
                                                .description("SArg")
                                                .command(noop);
                                    });
                        });
                    })

                    .addLiteral(arg2 -> {
                        arg2.literal("arg2").addArgument(farg -> {
                            farg.name("farg").type(ReadAnyArgumentType.INSTANCE)
                                    .addArgument(sarg -> {
                                        sarg.name("SArg").type(ReadAnyArgumentType.INSTANCE)
                                                .command(noop);
                                    });
                        });
                    })

                    .build();
        }

        private void assertRender(String msg, String command) {
            var rendered = dispatcher.renderHelpUsage(dispatcher.parse(command, this)).toString();
            System.out.println(rendered);
            Assertions.assertEquals(msg.trim(), rendered.trim());
        }

        @Test
        void testRootPrint() {
            assertRender("""
                            args  ...  -  Args Command
                            myc2  ...
                            mycommand  ...  -  My Command
                            redirect  ...  -  Redirect command to root
                            """,
                    ""
            );
        }

        @Test
        void testRedirect() {
            assertRender("""
                    redirect args  ...  -  Args Command
                    redirect myc2  ...
                    redirect mycommand  ...  -  My Command
                    redirect redirect  ...  -  Redirect command to root
                    """, "redirect ");

            assertRender("""
                    redirect ...  -  Redirect command to root
                    """, "redirect");
        }

        @Test
        void testLiterChild() {
            assertRender("""
                            mycommand ...  -  My Command
                            mycommand subcmd  -  Sub Command
                            mycommand subcmdn
                            """,
                    "mycommand"
            );
            assertRender("""
                            redirect mycommand ...  -  My Command
                            redirect mycommand subcmd  -  Sub Command
                            redirect mycommand subcmdn
                            """,
                    "redirect mycommand");
        }

        @Test
        void testArgChild() {
            assertRender("""
                    args ...  -  Args Command
                    args arg1 <farg> ...  -  FArg
                    args arg1 <farg> <SArg>  -  SArg
                    args arg2 <farg> <SArg>
                    """, "args");
        }
    }

    @Test
    void testDispatcher() {
        var dispatcher = new CommandDispatcher<>();

        dispatcher.root.register(
                new LiteralCommandNodeBuilder<>()
                        .literal("hello")
                        /*.addArgument(myarg -> myarg.name("thearg").type(new ArgumentType<Object>() {
                            @Override
                            public Object parse(AnyValueReader reader) throws CommandSyntaxException {
                                return reader.readAny();
                            }
                        }))*/
                        .addLiteral(subarx -> {
                            subarx.literal("holx").command(s -> {
                                System.out.println("HOLX!");
                            });
                        })
                        .command(s -> {
                            System.out.println("! my arg = " + s.getArgument("thearg", null));
                            System.out.println("source: " + s.getSource());
                        })
                        .build()
        );

        var result = dispatcher.parse(new AnyValueReader("hello holx"), new Object());
        System.out.println(result);
        dispatcher.execute(result);
    }
}
