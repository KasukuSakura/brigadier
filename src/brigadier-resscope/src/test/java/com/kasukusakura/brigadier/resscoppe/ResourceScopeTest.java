/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.resscoppe;

import com.kasukusakura.brigadier.utils.StepAssertion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.ref.Cleaner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public class ResourceScopeTest {
    private static final Cleaner cleaner = Cleaner.create();

    @Test
    public void testScopeOnDispose() {
        var step = new StepAssertion();

        var scope = ResourceScope.openShared();
        scope.onDispose(() -> step.step(1));
        System.gc();
        step.step(0);

        scope.close();
        step.step(2);
    }

    @Test
    public void testFailedToCloseDuplicated() {
        var scope = ResourceScope.openShared();
        scope.close();

        Assertions.assertFalse(scope.isAlive());
        Assertions.assertThrows(Exception.class, scope::close).printStackTrace(System.out);
    }

    @Test
    public void testGCDispose() throws Throwable {
        var step = new StepAssertion();
        var scope = ResourceScope.openShared(cleaner);
        scope.onDispose(() -> {
            Thread.dumpStack();
            step.step(0);
        });

        scope = null;
        System.gc();
        System.gc();
        System.gc();

        Thread.sleep(1000L);

        step.steps(1);
    }

    @Test
    public void testWaitAndClose() {
        var scope = ResourceScope.openShared();
        var step = new StepAssertion();

        var ntvDoRun = new AtomicBoolean(true);

        var ntvThread = new Thread(() -> {
            scope.whileAlive(() -> {
                step.step(0);
                ntvDoRun.set(false);
                LockSupport.park();
                step.step(3);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("Done1");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            System.out.println("Done2");
        });

        ntvThread.start();

        while (ntvDoRun.get()) {
            Thread.onSpinWait();
        }
        step.step(1);

        var releaserThread = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            step.step(2);
            LockSupport.unpark(ntvThread);
        });
        scope.onDispose(() -> step.step(4));
        releaserThread.start();

        scope.waitAndClose();
        step.step(5);

        step.steps(6);

        Assertions.assertFalse(scope.isAlive());
    }
}
