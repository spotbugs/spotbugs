/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package nullnessAnnotations.relax;

import annotations.DetectorUnderTest;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.detect.CheckRelaxingNullnessAnnotation;

/**
 * @author nisticoa
 */
@DetectorUnderTest(CheckRelaxingNullnessAnnotation.class)
public class TestNullnessRelaxation2 {
	static interface I<T extends Number> {
		@NonNull
		Object get();

		Number set(@CheckForNull Number o);

		@NonNull
		T set2(@CheckForNull T o);
	}

	static interface SI2 extends I<Integer> {
		@CheckForNull
		@ExpectWarning("NP_METHOD_RETURN_RELAXING_ANNOTATION")
		String get();

		@ExpectWarning("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION")
		public Integer set(@NonNull Number o);

		@CheckForNull
		@ExpectWarning("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION,NP_METHOD_RETURN_RELAXING_ANNOTATION")
		public Integer set2(@NonNull Integer o);
	}

    static class SimpleClazz implements I<Integer> {
        @CheckForNull
        @ExpectWarning("NP_METHOD_RETURN_RELAXING_ANNOTATION")
        public String get(){
            return null;
        }

        @ExpectWarning("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION")
        public Integer set(@NonNull Number o){
            return null;
        }

        @CheckForNull
        @ExpectWarning("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION,NP_METHOD_RETURN_RELAXING_ANNOTATION")
        public Integer set2(@NonNull Integer o){
            return null;
        }
    }

    static interface SI3 extends I<Integer> {}
    static interface SI4 extends SI3, SI2 {}
    abstract static class Clazz1 implements SI4 {}
    abstract static class Clazz2 extends Clazz1 {}

    static class Clazz extends Clazz2 {
        @CheckForNull
        @ExpectWarning("NP_METHOD_RETURN_RELAXING_ANNOTATION")
        public String get(){
            return null;
        }

        @ExpectWarning("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION")
        public Integer set(@NonNull Number o){
            return null;
        }

        @CheckForNull
        @ExpectWarning("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION,NP_METHOD_RETURN_RELAXING_ANNOTATION")
        public Integer set2(@NonNull Integer o){
            return null;
        }
    }
}
