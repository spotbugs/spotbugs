package org.apache.commons.lang3.builder;

import java.util.Collection;

public class EqualsBuilder {

    public static boolean reflectionEquals(Object lhs, Object rhs) {
        throw new UnsupportedOperationException();
    }

    public static boolean reflectionEquals(Object lhs, Object rhs, Collection excludeFields) {
        throw new UnsupportedOperationException();
    }

    public static boolean reflectionEquals(Object lhs, Object rhs, String[] excludeFields) {
        throw new UnsupportedOperationException();
    }

    public static boolean reflectionEquals(Object lhs, Object rhs, boolean testTransients) {
        throw new UnsupportedOperationException();
    }

    public static boolean reflectionEquals(Object lhs, Object rhs, boolean testTransients, Class reflectUpToClass) {
        throw new UnsupportedOperationException();
    }

    public static boolean reflectionEquals(Object lhs, Object rhs, boolean testTransients, Class reflectUpToClass,
            String[] excludeFields) {

        throw new UnsupportedOperationException();
    }

    public EqualsBuilder() {
        // do nothing for now.
    }

    public EqualsBuilder appendSuper(boolean superEquals) {
        return this;
    }

    public EqualsBuilder append(Object lhs, Object rhs) {
        return this;
    }

    public boolean isEquals() {
        throw new UnsupportedOperationException();
    }
}
