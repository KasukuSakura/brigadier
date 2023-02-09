/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.console;

import com.kasukusakura.brigadier.command.CommandDispatcher;
import com.kasukusakura.brigadier.command.arguments.CLIComposedArgumentNode;
import com.kasukusakura.brigadier.command.arguments.EnumArgumentType;
import com.kasukusakura.brigadier.command.arguments.ReadAnyArgumentType;
import com.kasukusakura.brigadier.command.builtin.HelpCommand;
import com.kasukusakura.brigadier.command.tree.CommandNode;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

public class JConsoleTest {
    public static void main(String[] args) throws Throwable {
        Terminal terminal;
        try {
            terminal = TerminalBuilder.builder().dumb(false).build();
        } catch (Throwable error) {
            if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                launchCMD();
            }
            throw error;
        }

        var console = new Object();
        var dispatcher = new CommandDispatcher<>();

        var reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .highlighter(new BrigadierHighlighter<>(dispatcher, console))
                .completer(new BrigadierCompleter<>(dispatcher, console))
                .build();


        dispatcher.registerBuilder()
                .literal("cmd")
                .description("My Test command")
                .register(
                        new CLIComposedArgumentNode<Object>(
                                s -> {
                                    return v -> {
                                        System.out.println("arg1: " + v.getArgument("arg1", null));
                                        System.out.println("arg2: " + v.getArgument("arg2", null));
                                        System.out.println("arg3: " + v.getArgument("arg114547", null));
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
                                .description("The first argument")
                                .build()
                        ).register(dispatcher.newArgument()
                                .name("arg2")
                                .type(new EnumArgumentType<>(
                                        List.of("vb1", "vb2", "--vel2")
                                ))
                                .description("The second argument")
                                .build()
                        ).register(dispatcher.newArgument()
                                .name("arg114547")
                                .description("The third argument")
                                .type(ReadAnyArgumentType.INSTANCE)
                                .build()
                        )
                )
                .build();

        dispatcher.registerBuilder()
                .literal("stop")
                .description("Stop the console")
                .command(s -> System.exit(0))
                .build();

        dispatcher.root.register(new HelpCommand<>(
                CommandNode.allowAll(), null, false, (s, v) -> {
            System.out.println(v);
        }
        ).attach(dispatcher.root));

        try {
            while (true) {
                var nextLine = reader.readLine("> ");
                try {
                    dispatcher.execute(nextLine, console);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            LockSupport.park();
        }
    }

    private static void launchCMD() throws Throwable {
        var builder = new ProcessBuilder(
                "cmd", "/c", "start",
                new File(System.getProperty("java.home"), "/bin/java.exe").getAbsolutePath(),
                "-cp", ManagementFactory.getRuntimeMXBean().getClassPath(),
                "com.kasukusakura.brigadier.console.JConsoleTest"
        ).inheritIO();
        builder.start().waitFor();
    }
}
