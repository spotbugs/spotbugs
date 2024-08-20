/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004, University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.ba.generic;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import com.github.spotbugs.java.lang.classfile.Signature.TypeArg.WildcardIndicator;

import edu.umd.cs.findbugs.ba.ObjectTypeFactory;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * Extension to ObjectType that includes additional information about the
 * generic signature.
 * <p>
 *
 * A GenericObjectType is either a parameterized type e.g.
 * <code>List&lt;String&gt;</code>, or a type variable e.g. <code>T</code>.
 * <p>
 *
 * This class cannot be initialized directly. Instead, create a
 * GenericObjectType by calling GenericUtilities.getType(String) and passing in
 * the bytecode signature for the type.
 *
 * @author Nat Ayewah
 */
public class GenericObjectType extends ObjectType {

    final List<? extends ReferenceType> parameters;

    final @CheckForNull String variable;

    final @CheckForNull WildcardIndicator wildcardIndicator;

    final List<? extends ReferenceType> extensions;

    final String genericSignature;

    public ReferenceType produce() {
        return getTypeCategory().produce(this);
    }

    @Override
    public int hashCode() {
        return 13 * super.hashCode()
                + 9 * Objects.hashCode(parameters)
                + 7 * Objects.hashCode(variable)
                + 7 * Objects.hashCode(wildcardIndicator)
                + Objects.hashCode(extensions);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GenericObjectType)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        GenericObjectType that = (GenericObjectType) o;
        // Self referencing generics will cause a stack overflow when calling Objects.equals(parameters, that.parameters)
        // For instance: <C extends Map<X, C>, X extends Number>
        // Instead we compare the generic signature when we know it
        if (genericSignature != null && genericSignature.equals(that.genericSignature)) {
            return true;
        }
        return Objects.equals(parameters, that.parameters)
                && Objects.equals(variable, that.variable)
                && Objects.equals(wildcardIndicator, that.wildcardIndicator)
                && Objects.equals(extensions, that.extensions);
    }

    public Type getUpperBound() {
        if (WildcardIndicator.EXTENDS == wildcardIndicator) {
            return extensions.get(0);
        }
        return this;
    }

    /**
     * @return Returns the extension.
     */
    public Type getExtension() {
        if (extensions.isEmpty()) {
            return null;
        }

        return extensions.get(0);
    }

    /**
     * @return Returns the variable.
     */
    public String getVariable() {
        return variable;
    }

    public WildcardIndicator getWildcardIndicator() {
        return wildcardIndicator;
    }

    /**
     * @return Returns the generic signature
     */
    public String getGenericSignature() {
        return genericSignature;
    }

    /**
     * Get the TypeCategory that represents this Object
     *
     * @see GenericUtilities.TypeCategory
     */
    public GenericUtilities.TypeCategory getTypeCategory() {
        if (hasParameters() && wildcardIndicator == null && extensions.isEmpty()) {
            return GenericUtilities.TypeCategory.PARAMETERIZED;

        } else if (!hasParameters() && variable != null && extensions.isEmpty()) {
            if ("*".equals(variable)) {
                return GenericUtilities.TypeCategory.WILDCARD;
            } else {
                return GenericUtilities.TypeCategory.TYPE_VARIABLE;
            }

        } else if (!hasParameters() && wildcardIndicator != null && extensions.isEmpty()) {
            if (WildcardIndicator.UNBOUNDED == wildcardIndicator) {
                return GenericUtilities.TypeCategory.WILDCARD;
            } else {
                return GenericUtilities.TypeCategory.TYPE_VARIABLE;
            }

        } else if (!hasParameters() && wildcardIndicator != null && !extensions.isEmpty()) {
            if (WildcardIndicator.EXTENDS == wildcardIndicator) {
                return GenericUtilities.TypeCategory.WILDCARD_EXTENDS;
            } else if (WildcardIndicator.SUPER == wildcardIndicator) {
                return GenericUtilities.TypeCategory.WILDCARD_SUPER;
            }

        }
        // this should never happen
        throw new IllegalStateException("The Generic Object Type is badly initialized");
    }

    /**
     * @return true if this GenericObjectType represents a parameterized type
     *         e.g. <code>List&lt;String&gt;</code>. This implies that
     *         isVariable() is falses
     */
    public boolean hasParameters() {
        return parameters != null && parameters.size() > 0;
    }

    /**
     * @return the number of parameters if this is a parameterized class, 0
     *         otherwise
     */
    public int getNumParameters() {
        return parameters != null ? parameters.size() : 0;
    }

    /**
     * @param index
     *            should be less than getNumParameters()
     * @return the type parameter at index
     */
    public ReferenceType getParameterAt(int index) {
        if (index < getNumParameters()) {
            return parameters.get(index);
        } else {
            throw new IndexOutOfBoundsException("The index " + index + " is too large for " + this);
        }
    }

    public @CheckForNull List<? extends ReferenceType> getParameters() {
        if (parameters == null) {
            return null;
        }
        return Collections.unmodifiableList(parameters);
    }

    // Package Level constructors

    /**
     * Create a GenericObjectType that represents a Simple Type Variable or a
     * simple wildcard with no extensions
     *
     * @param variable
     *            the type variable e.g. <code>T</code>
     */
    GenericObjectType(@Nonnull String variable) {
        super(Type.OBJECT.getClassName());
        this.variable = variable;
        this.wildcardIndicator = null;
        this.extensions = Collections.emptyList();
        this.parameters = null;
        this.genericSignature = null;
    }

    /**
     * Create a GenericObjectType that represents a Wildcard with extensions
     *
     */
    GenericObjectType(@Nonnull WildcardIndicator wildcardIndicator, @CheckForNull ReferenceType extension) {
        super(Type.OBJECT.getClassName());
        this.variable = null;
        this.wildcardIndicator = wildcardIndicator;
        this.extensions = extension != null ? Collections.singletonList(extension) : Collections.emptyList();
        this.parameters = null;
        this.genericSignature = null;
    }

    /**
     * Create a GenericObjectType that represents a Wildcard with extensions
     *
     */
    GenericObjectType(String variable, @Nonnull WildcardIndicator wildcardIndicator, List<? extends ReferenceType> extensions) {
        super(Type.OBJECT.getClassName());
        this.variable = variable;
        this.wildcardIndicator = wildcardIndicator;
        this.extensions = extensions;
        this.parameters = null;
        this.genericSignature = null;
    }

    /**
     * Create a GenericObjectType that represents a parameterized class
     *
     * @param className
     *            the class that is parameterized. e.g.
     *            <code>java.util.List</code>
     * @param parameters
     *            the parameters of this class
     */
    GenericObjectType(@DottedClassName String className, List<? extends ReferenceType> parameters, String genericSignature) {
        super(className);
        this.variable = null;
        this.wildcardIndicator = null;
        this.extensions = Collections.emptyList();
        this.parameters = parameters;
        this.genericSignature = genericSignature;
    }

    /**
     * Create a GenericObjectType that represents a parameterized class
     *
     * @param className
     *            the class that is parameterized. e.g.
     *            <code>java.util.List</code>
     * @param parameters
     *            the parameters of this class
     */
    GenericObjectType(@DottedClassName String className, WildcardIndicator wildcardIndicator, List<? extends ReferenceType> parameters,
            String genericSignature) {
        super(className);
        this.variable = null;
        this.wildcardIndicator = wildcardIndicator;
        this.extensions = Collections.emptyList();
        this.parameters = parameters;
        this.genericSignature = genericSignature;
    }

    /**
     * @return the underlying ObjectType for this Generic Object
     */
    public ObjectType getObjectType() {
        @DottedClassName
        String cName = ClassName.fromFieldSignatureToDottedClassName(getSignature());
        if (cName == null) {
            throw new IllegalStateException("Can't provide ObjectType for " + this);
        }
        return ObjectTypeFactory.getInstance(cName);
    }

    /**
     * Return a string representation of this object. (I do not override
     * <code>toString()</code> in case any existing code assumes that this
     * object is an ObjectType and expects similar string representation. i.e.
     * <code>toString()</code> is equivalent to <code>toString(false)</code>)
     *
     * @param includeGenerics
     *            if true then the string includes generic information in this
     *            object. Otherwise this returns the same value as
     *            ObjectType.toString()
     */
    public String toString(boolean includeGenerics) {
        // if (!includeGenerics) return super.toString();

        return getTypeCategory().asString(this);
    }

    @Override
    public String toString() {
        return getTypeCategory().asString(this);
    }

    public String toPlainString() {
        return super.toString();
    }

    public String getGenericParametersAsString() {
        if (getTypeCategory() != GenericUtilities.TypeCategory.PARAMETERIZED) {
            throw new IllegalStateException(toString() + " doesn't have generic parameters");
        }
        String baseStringValue = super.toString();
        String fullStringValue = toString();
        return fullStringValue.substring(baseStringValue.length());
    }
}
