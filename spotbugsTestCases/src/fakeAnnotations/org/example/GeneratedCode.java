package org.example;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import edu.umd.cs.findbugs.bytecode.MemberUtils;

/**
 * {@link MemberUtils} captures annotations ending with "Generated", this annotation is named so the bugs are not ignored.
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface GeneratedCode {}
