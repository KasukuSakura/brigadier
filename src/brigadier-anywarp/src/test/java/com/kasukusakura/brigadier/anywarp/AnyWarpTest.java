/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.anywarp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.atomic.AtomicInteger;


public class AnyWarpTest {
    public static CallSite cinvoke(MethodHandles.Lookup lookup, String name, MethodType type) throws Throwable {
        var rp1 = lookup.findGetter(lookup.lookupClass(), "arg0", Runnable.class);
        System.out.println(rp1);
        var invoke0 = lookup.findVirtual(Runnable.class, "run", MethodType.methodType(void.class));

        return new ConstantCallSite(
                MethodHandles.filterReturnValue(rp1, invoke0).asType(type)
        );
    }

    public interface Interface1 {
        public void met1();
    }

    @Test
    void testInterface() throws Throwable {
        var fncx = new AtomicInteger();
        var impl = new Interface1() {
            @Override
            public void met1() {
                fncx.getAndIncrement();
            }
        };

        var itfx = AnyWarpBuilder.builder()
                .types(Interface1.class)
                .constructors(Interface1.class)
                .resolver((resolver, method, desc, modifiers) -> resolver.redirectToField(0))
                .definer(MethodHandles.lookup())
                .doDefine();

        System.out.println(itfx);

        var instance = (Interface1) itfx.findConstructor(itfx.lookupClass(), MethodType.methodType(void.class, Interface1.class)).invoke(impl);

        System.out.println(instance);
        instance.met1();

        Assertions.assertEquals(1, fncx.get());
    }

    @Test
    void testInvokeDynamic() throws Throwable {
        var fncx = new AtomicInteger();
        Runnable tsk = fncx::getAndIncrement;

        var itfx = AnyWarpBuilder.builder()
                .types(Interface1.class)
                .constructors(Runnable.class)
                .resolver((resolver, method, desc, modifiers) -> {
                    return resolver.invokeDynamic(
                            AnyWarpTest.class.getName().replace('.', '/'),
                            "cinvoke",
                            MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class).toMethodDescriptorString(),
                            null
                    );
                })
                .definer(MethodHandles.lookup())
                .doDefine();
        System.out.println(itfx);

        var rps = (Interface1) itfx.findConstructor(itfx.lookupClass(), MethodType.methodType(void.class, Runnable.class)).invoke(tsk);
        System.out.println(rps);
        rps.met1();

        Assertions.assertEquals(1, fncx.get());
    }
}
