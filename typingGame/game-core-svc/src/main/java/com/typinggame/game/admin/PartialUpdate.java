package com.typinggame.game.admin;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.util.Set;

public final class PartialUpdate {
    private PartialUpdate() {
    }

    public static void copyNonNull(Object source, Object target, String... ignored) {
        BeanWrapper incoming = new BeanWrapperImpl(source);
        BeanWrapper current = new BeanWrapperImpl(target);
        Set<String> skip = Set.of(ignored);
        for (PropertyDescriptor descriptor : incoming.getPropertyDescriptors()) {
            String name = descriptor.getName();
            if ("class".equals(name) || skip.contains(name) || !current.isWritableProperty(name)) continue;
            Object value = incoming.getPropertyValue(name);
            if (value != null) {
                current.setPropertyValue(name, value);
            }
        }
    }
}
