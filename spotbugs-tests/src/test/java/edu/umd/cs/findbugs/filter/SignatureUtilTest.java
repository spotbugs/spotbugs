/*
 * Contributions to SpotBugs
 * Copyright (C) 2017, kengo
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
package edu.umd.cs.findbugs.filter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

/**
 * @since 3.1
 */
public class SignatureUtilTest {
    /**
     * Return {@code null} if given parameter is null, otherwise signature of given type.
     */
    @Test
    public void testCreateFieldSignature() {
        assertThat(SignatureUtil.createFieldSignature(null), is(nullValue()));
        assertThat(SignatureUtil.createFieldSignature("int"), is("I"));
        assertThat(SignatureUtil.createFieldSignature("double[]"), is("[D"));
        assertThat(SignatureUtil.createFieldSignature("short[][]"), is("[[S"));
    }

    /**
     * First parameter is comma-separated value. It is possible to contain space, tab, line-break or line-feed around
     * comma. Even though first parameter contains multiple values, generated signature does not separate them by comma.
     */
    @Test
    public void testCreateMethodSignature() {
        assertThat(SignatureUtil.createMethodSignature("", "void"), is("()V"));
        assertThat(SignatureUtil.createMethodSignature("byte,\r\nchar, \tboolean", "void"), is("(BCZ)V"));
        assertThat(SignatureUtil.createMethodSignature("float", "java.lang.String[]"), is("(F)[Ljava/lang/String;"));
    }

    /**
     * If both parameter is null, {@link SignatureUtil#createMethodSignature(String, String)} returns null. However, its
     * return value should start with {@code ~}, which means regexp.
     *
     * @see NameMatch This class uses {@code ~} to judge signature is regexp or not.
     */
    @Test
    public void testCreateMethodSignatureWithNull() {
        assertThat(SignatureUtil.createMethodSignature(null, null), is(nullValue()));
        assertThat(SignatureUtil.createMethodSignature(null, "long"), is("~\\(.*\\)J"));
        assertThat(SignatureUtil.createMethodSignature("", null), is("~\\(\\).*"));
    }
}
