/*
 * Copyright (c) KasukuSakura Technologies. All rights reserved.
 * Licensed under the MIT license.
 */

package com.kasukusakura.brigadier.anywarp;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.Objects;

class MethodInfo {
    String name;
    String desc;
    int modifiers;

    public MethodInfo(String name, String desc, int modifiers) {
        this.name = name;
        this.desc = desc;
        this.modifiers = modifiers;
    }

    public MethodInfo(Method method) {
        this.name = method.getName();
        this.desc = Type.getMethodDescriptor(method);
        this.modifiers = method.getModifiers();

        modifiers &= ~Opcodes.ACC_ABSTRACT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodInfo)) return false;
        MethodInfo that = (MethodInfo) o;
        if (Objects.equals(name, that.name) && Objects.equals(desc, that.desc)) {
            return (modifiers & Opcodes.ACC_STATIC) == (that.modifiers & Opcodes.ACC_STATIC);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode() ^ desc.hashCode();
    }
}
