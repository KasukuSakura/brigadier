/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.utils;

import java.util.concurrent.atomic.AtomicInteger;

public class StepAssertion {
    private final AtomicInteger counter;

    public StepAssertion() {
        this.counter = new AtomicInteger();
    }

    public void step(int step) {
        if (counter.getAndIncrement() != step) {
            throw new AssertionError("Step not match: " + step);
        }
    }

    public void steps(int steps) {
        if (counter.get() != steps) {
            throw new AssertionError("Missing some step not executed. Excepted steps: " + steps + ", executed: " + counter.get());
        }
    }
}
