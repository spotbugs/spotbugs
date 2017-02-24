package npe;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Test cases extracted from
 * org.eclipse.jdt.core.tests.compiler.regression.NullReferenceTest, version
 * 1.39
 */
@edu.umd.cs.findbugs.annotations.SuppressWarnings({ "DLS", "UuF", "SIC", "UrF", "UwF", "IP", "DB", "RV", "Dm", "DE", "SP", "UCF",
        "IL", "SF", "Se", "RC", "NS", "ST", "OS", "NP_UNWRITTEN_FIELD" })
public class EclipseNullReferenceTestCases {

    // test0001_simple_local
    static class Report1 {
        static class X {
            void foo() {
                Object o = null;
                o.toString();
            }
        }

    }

    // test0002_simple_field
    static class Report2 {
        static class X {
            Object o;

            void foo() {
                o = null;
                o.toString();
            }
        }

    }

    // test0003_simple_parameter
    static class Report3 {
        static class X {
            void foo(Object o) {
                o = null;
                o.toString();
            }
        }

    }

    // test0004_final_local
    static class Report4 {
        static class X {
            void foo() {
                final Object o = null;
                o.toString();
            }
        }

    }

    // test0005_final_local
    // static class Report5 {
    // static class X {
    // void foo() {
    // final Object o;
    // o.toString();
    // }
    // }

    // }

    // test0006_final_local
    static class Report6 {
        static class X {
            void foo() {
                final Object o = null;
                if (o != null) { /* */
                }
            }
        }

    }

    // test0007_local_with_member
    static class Report7 {
        static class X {
            public Object m;

            void foo() {
                X x = null;
                x.m.toString();
            }
        }

    }

    // test0008_local_with_member
    static class Report8 {
        static class X {
            public Object m;

            void foo() {
                X x = null;
                System.out.println(x.m);
            }
        }

    }

    // test0009_local_with_member
    static class DoNotReport1 {
        static class X {
            public Object m;

            void foo(X x) {
                x.m.toString();
            }
        }

    }

    // test0010_field_with_method_call
    static class DoNotReport2 {
        static class X {
            Object o;

            void foo() {
                o = null;
                bar();
                o.toString();
            }

            void bar() {
            }
        }

    }

    // test0011_field_with_method_call
    static class DoNotReport3 {
        static class X {
            static Object o;

            void foo() {
                o = null;
                bar();
                o.toString();
            }

            static void bar() {
            }
        }

    }

    // test0012_field_with_method_call
    static class DoNotReport4 {
        static class X {
            Object o;

            void foo() {
                o = null;
                bar();
                o.toString();
            }

            static void bar() {
            }
        }

    }

    // test0013_field_with_method_call
    static class DoNotReport5 {
        static class X {
            Object o;

            void foo() {
                o = null;
                bar();
                o.toString();
            }

            void bar() {
            }
        }

    }

    // test0014_field_with_explicit_this_access
    static class Report9 {
        static class X {
            Object o;

            void foo() {
                o = null;
                this.o.toString();
            }
        }

    }

    // test0015_field_with_explicit_this_access
    static class Report10 {
        static class X {
            Object o;

            void foo() {
                this.o = null;
                o.toString();
            }
        }

    }

    // test0016_field_of_another_object
    static class DoNotReport6 {
        static class X {
            Object o;

            void foo() {
                X other = new X();
                other.o = null;
                other.o.toString();
            }
        }

    }

    // test0017_field_of_another_object
    static class DoNotReport7 {
        static class X {
            Object o;

            void foo() {
                X other = this;
                o = null;
                other.o.toString();
            }
        }

    }

    // test0018_field_of_enclosing_object
    static class Report11 {
        static class X {
            Object o;

            class Y {
                void foo() {
                    X.this.o = null;
                    X.this.o.toString();
                }
            }
        }

    }

    // test0019_field_synchronized
    static class Report12 {
        static class X {
            Object o;

            public synchronized void foo() {
                o = null;
                o.toString();
            }

            void bar() {/* */
            }
        }

    }

    // test0020_final_field
    static class Report13 {
        static class X {
            final Object o = null;

            public synchronized void foo() {
                bar();
                o.toString();
            }

            void bar() {/* */
            }
        }

    }

    // test0021_final_field
    static class Report14 {
        static class X {
            final Object o = null;

            X() {
                bar();
                o.toString();
            }

            void bar() {/* */
            }
        }

    }

    // test0022_final_field
    static class Report15 {
        static class X {
            final Object o = new Object();

            X() {
                bar();
                if (o == null) { /* empty */
                }
            }

            void bar() {/* */
            }
        }

    }

    // test0023_field_assignment
    static class DoNotReport8 {
        static class X {
            public Object m;

            void foo(X x) {
                Object o = x.m;
                if (o == null) { /* */
                }
                ;
            }
        }

    }

    // test0024_field_cast_assignment
    static class DoNotReport9 {
        static class X {
            public Object m;

            void foo(Object x) {
                Object o = ((X) x).m;
                if (o == null) { /* */
                }
                ;
            }
        }

    }

    // test0025_parameter
    static class DoNotReport10 {
        static class X {
            void foo(Object o) {
                o.toString();
            }
        }

    }

    // test0027_embedded_comparison
    static class Report16 {
        static class X {
            void foo(Object o) {
                boolean b = o != null;
                if (b) { /* */
                }
                o.toString();
            }
        }

    }

    // test0028_field_as_initializer
    static class DoNotReport11 {
        static class X {
            X f;

            void foo() {
                X x = f;
                if (x == null) { /* */
                }
            }
        }

    }

    // test0029_field_assignment
    static class Report17 {
        static class X {
            public Object m;

            void foo() {
                X x = null;
                x.m = new Object();
            }
        }

    }

    // test0030_conditional_expression
    static class Report18 {
        static class X {
            void foo() {
                Object o = true ? null : null;
                o.toString();
            }
        }

    }

    // test0031_conditional_expression
    static class Report19 {
        static class X {
            void foo() {
                Object o = true ? null : new Object();
                o.toString();
            }
        }

    }

    // test0032_conditional_expression
    static class DoNotReport12 {
        static class X {
            void foo() {
                Object o = false ? null : new Object();
                o.toString();
            }
        }

    }

    // test0033_conditional_expression
    static class Report20 {
        static class X {
            void foo() {
                Object o = (1 == 1) ? null : new Object();
                o.toString();
            }
        }

    }

    // _test0034_conditional_expression
    static class Report21 {
        static class X {
            boolean b;

            void foo() {
                Object o = b ? null : new Object();
                o.toString();
            }
        }

    }

    // test0035_conditional_expression
    static class DoNotReport13 {
        static class X {
            boolean b;

            void foo() {
                Object o = b ? null : new Object();
                if (o == null) { /* */
                }
            }
        }

    }

    // test0036_conditional_expression
    static class Report22 {
        static class X {
            boolean b;

            void foo() {
                Object o = b ? null : null;
                if (o == null) { /* */
                }
            }
        }

    }

    // test0040_autoboxing_compound_assignment
    static class Report23 {
        static class X {
            void foo() {
                Integer i = null;
                i += 1;
            }
        }

    }

    // test0041_autoboxing_increment_operator
    static class Report24 {
        static class X {
            void foo() {
                Integer i = null;
                i++;
                ++i;
            }
        }

    }

    // test0042_autoboxing_literal
    static class Report25 {
        static class X {
            void foo() {
                Integer i = 0;
                if (i == null) {
                }
                ;
            }
        }

    }

    // test0043_autoboxing_literal
    static class Report26 {
        static class X {
            void foo() {
                Integer i = null;
                System.out.println(i + 4);
            }
        }

    }

    // test0044_autoboxing
    static class DoNotReport14 {
        static class X {
            void foo() {
                int i = 0;
                boolean b = i < 10;
            }
        }

    }

    // test0050_array
    static class DoNotReport15 {
        static class X {
            public static void main(String args[]) {
                args = new String[] { "zero" };
                args[0] = null;
                if (args[0] == null) {
                }
                ;
            }
        }

    }

    // test0051_array
    static class Report27 {
        static class X {
            public static void main(String args[]) {
                args = null;
                args[0].toString();
            }
        }

    }

    // test0052_array
    static class DoNotReport16 {
        static class X {
            public void foo(String args[]) {
                String s = args[0];
                if (s == null) {
                }
                ;
            }
        }

    }

    // test0053_array
    static class DoNotReport17 {
        static class X {
            public void foo(String args[]) {
                for (int i = 0; i < args.length; i++) { /* */
                }
            }
        }

    }

    // test0061_method_call_guard
    static class Report28 {
        static class X {
            void foo(Object o) {
                o.toString();
                if (o == null) {
                }
                ;
            }
        }

    }

    // test0062_method_call_isolation
    static class Report29 {
        static class X {
            void foo(Object o) {
                if (bar(o = null)) {
                    if (o == null) {/* empty */
                    }
                }
            }

            boolean bar(Object o) {
                return true;
            }
        }

    }

    // test0063_method_call_isolation
    static class DoNotReport18 {
        static class X {
            void foo(Object o) {
                if (bar(o == null ? new Object() : o)) {
                    if (o == null) {/* empty */
                    }
                }
            }

            boolean bar(Object o) {
                return true;
            }
        }

    }

    // test0064_method_call_isolation
    static class Report30 {
        static class X {
            void foo(Object o) {
                if (bar(o = new Object())) {
                    if (o == null) {/* empty */
                    }
                }
            }

            boolean bar(Object o) {
                return true;
            }
        }

    }

    // test0065_method_call_invocation_target
    static class DoNotReport19 {
        static class X {
            void foo() {
                Object o = null;
                (o = new Object()).toString();
            }
        }

    }

    // test0066_method_call_invocation_target
    static class Report31 {
        static class X {
            void foo() {
                Object o = new Object();
                (o = null).toString();
            }
        }

    }

    // test0067_method_call_invocation_target
    static class Report32 {
        static class X {
            void foo(Object o) {
                (o = new Object()).toString();
                if (o == null) { /* */
                }
            }
        }

    }

    // test0068_method_call_assignment
    static class DoNotReport20 {
        static class X {
            X bar() {
                return null;
            }

            void foo(X x) {
                x = x.bar();
                if (x == null) { /* */
                }
            }
        }

    }

    // test0070_type_reference
    static class Report33 {
        static class X {
            public static void main(String args[]) {
                Class c = java.lang.Object.class;
                if (c == null) {
                }
                ;
            }
        }

    }

    // test0080_shortcut_boolean_expressions
    static class Report34 {
        static class X {
            void foo(Object o1, Object o2) {
                if (o1 != null && (o2 = o1) != null) { /* */
                }
            }
        }

    }

    // test0081_shortcut_boolean_expressions
    static class Report35 {
        static class X {
            void foo(Object o1, Object o2) {
                while (o1 != null && (o2 = o1) != null) { /* */
                }
            }
        }

    }

    // test0082_shortcut_boolean_expression
    static class Report36 {
        static class X {
            void foo(Object o) {
                if (o == null || o == null) {
                    o = new Object();
                }
                if (o == null) { /* */
                }
            }
        }
    }

    // test0083_shortcut_boolean_expression
    static class Report37 {
        static class X {
            void foo(Object o) {
                if (o == null && o == null) {
                    o = new Object();
                }
                if (o == null) { /* */
                }
            }
        }
    }

    // test0084_shortcut_boolean_expression
    static class Report38 {
        static class X {
            boolean foo(Integer i1, Integer i2) {
                return (i1 == null && i2 == null) || (i1.byteValue() == i2.byteValue());
            }
        }
    }

    // test0085_shortcut_boolean_expression
    static class Report39 {
        static class X {
            boolean foo(Integer i1, Integer i2) {
                return (i1 == null & i2 == null) || (i1.byteValue() == i2.byteValue());
            }
        }
    }

    // test0090_instanceof
    static class DoNotReport21 {
        static class X {
            boolean dummy;

            void foo(Object o) {
                if (dummy) {
                    o = null;
                }
                if (o instanceof X) { /* */
                }
            }
        }
    }

    // test0091_instanceof
    static class DoNotReport22 {
        static class X {
            boolean dummy;

            void foo(Object o) {
                if (dummy) {
                    o = null;
                }
                if (o instanceof X) { /* */
                }
                if (o == null) { /* */
                }
            }
        }
    }

    // test0092_instanceof
    static class Report40 {
        static class X {
            boolean dummy;

            void foo() {
                Object o = null;
                if (o instanceof X) { /* */
                }
            }
        }
    }

    // test0093_instanceof
    static class Report41 {
        static class X {
            void foo(Object x) {
                if (x instanceof X) {
                    if (x == null) { /* */
                    }
                }
            }
        }
    }

    // test0094_instanceof
    static class DoNotReport23 {
        static class X {
            void foo(Object x) {
                if (x instanceof X) {
                    return;
                }
                if (x != null) { /* */
                }
            }
        }
    }

    // test0095_instanceof_conditional_or
    static class Report42 {
        static class X {
            void foo(Object x) {
                if (!(x instanceof String) || x == null) {
                    return;
                }
            }
        }
    }

    // test0120_strings_concatenation
    static class DoNotReport24 {
        static class X {
            String foo(String s1, String s2) {
                if (s1 == null) { /* */
                }
                ;
                return s1 + s2;
            }
        }

    }

    // test0121_strings_concatenation
    static class DoNotReport25 {
        static class X {
            String foo(String s1, String s2) {
                if (s1 == null) { /* */
                }
                ;
                s1 += s2;
                return s1;
            }
        }

    }

    // test0122_strings_concatenation
    static class Report43 {
        static class X {
            String foo(String s1) {
                if (s1 == null) { /* */
                }
                ;
                return s1.toString();
            }
        }

    }

    // test0123_strings_concatenation
    static class DoNotReport26 {
        static class X {
            String foo(String s, Object o, Integer i) {
                if (s == null || o == null || i == null) { /* */
                }
                ;
                if (bar()) {
                    return s + i;
                }
                return o + s;
            }

            boolean bar() {
                return false;
            }
        }

    }

    // test0124_strings_concatenation
    static class DoNotReport27 {
        static class X {
            String foo(String s, Object o, Integer i) {
                if (s == null || o == null || i == null) { /* */
                }
                ;
                s += o;
                s += i;
                return s;
            }
        }

    }

    // test0125_strings_concatenation
    static class DoNotReport28 {
        static class X {
            void foo(Object o, Integer i) {
                System.out.println(o + (o == null ? "" : o.toString()));
                System.out.println(i + (i == null ? "" : i.toString()));
            }
        }

    }

    // test0126_strings_concatenation
    static class DoNotReport29 {
        static class X {
            void foo(Object o) {
                System.out.println(o + "");
                if (o != null) { /* */
                }
                ;
            }
        }

    }

    // test0127_strings_concatenation
    static class DoNotReport30 {
        static class X {
            void foo() {
                Object o = null;
                System.out.println(o + "");
            }
        }

    }

    // test0300_if_else
    static class Report44 {
        static class X {
            public void foo() {
                Object o = null;
                if (false) {
                    o = new Object();
                }
                if (true) {
                    //
                } else {
                    o = new Object();
                }
                o.toString();
            }
        }

    }

    // test0301_if_else
    static class Report45 {
        static class X {
            void foo() {
                Object o = new Object();
                if (o != null) {
                }
            }
        }

    }

    // test0302_if_else
    static class Report46 {
        static class X {
            void foo(Object o) throws Exception {
                if (o == null) {
                    throw new Exception();
                }
                if (o != null) {
                }
            }
        }

    }

    // test0303_if_else
    static class Report47 {
        static class X {
            void foo(Object o) {
                if (o == null) {
                    return;
                }
                if (o != null) {
                }
            }
        }

    }

    // test0304_if_else
    static class Report48 {
        static class X {
            void foo(Object o) {
                if (o == null) {
                    o.toString();
                }
            }
        }

    }

    // test0305_if_else
    static class Report49 {
        static class X {
            void foo(Object o) {
                if (o == null) {
                    // do nothing
                }
                o.toString();
            }
        }

    }

    // test0306_if_else
    static class Report50 {
        static class X {
            void foo(Object o) {
                if (o.toString().equals("")) {
                    if (o == null) {
                        // do nothing
                    }
                }
            }
        }

    }

    // test0307_if_else
    static class DoNotReport31 {
        static class X {
            void foo(Object o) {
                if (o == null) {
                    System.exit(0);
                }
                if (o == null) {
                    // do nothing
                }
            }
        }

    }

    // test0308_if_else
    static class Report51 {
        static class X {
            boolean b;

            void foo(Object o) {
                if (b) {
                    o = null;
                }
                o.toString();
            }
        }

    }

    // test0309_if_else
    static class Report52 {
        static class X {
            boolean b1, b2;

            void foo(Object o) {
                if (b1) {
                    o = null;
                }
                if (b2) {
                    o = new Object();
                }
                o.toString();
            }
        }

    }

    // test0310_if_else
    static class Report53 {
        static class X {
            boolean b1, b2;

            void foo(Object o) {
                if (b1) {
                    o = null;
                }
                if (b2) {
                    o.toString();
                    o.toString();
                }
                o.toString();
            }
        }

    }

    // test0311_if_else
    static class DoNotReport32 {
        static class X {
            void foo(Object o) {
                if (o == null)
                    o = new Object();
                o.toString();
            }
        }
    }

    // test0312_if_else
    static class Report54 {
        static class X {

            void foo() {
                Object o = new Object();
                if (o == null) { /* */
                }
                if (o != null) { /* */
                }
                o.toString();
            }
        }

    }

    // test0313_if_else
    static class Report55 {
        static class X {
            void foo(Object o) {
                if (o == null) {
                    o = new Object();
                }
                if (o == null) { /* */
                }
            }
        }
    }

    // test0314_if_else
    static class Report56 {
        static class X {
            void foo(Object o) {
                if (o != null) {
                    o = null;
                }
                if (o == null) { /* */
                }
            }
        }
    }

    // test0315_if_else
    static class Report57 {
        static class X {
            void foo(Object o) {
                if (o != null) {
                    o = null;
                }
                o.toString();
            }
        }
    }

    // test0316_if_else
    static class Report58 {
        static class X {
            @DesireWarning("NP")
            void foo(Object o, boolean b) {
                if (o == null || b) { /* */
                } else { /* */
                }
                o.toString();
            }
        }
    }

    // test0317_if_else_nested
    static class DoNotReport33 {
        static class X {
            void foo(Object o, boolean b) {
                if (o != null) {
                    if (b) {
                        o = null;
                    }
                }
                if (o == null) { /* */
                }
            }
        }
    }

    // test0318_if_else_nested
    static class DoNotReport34 {
        static class X {
            void foo(Object o, boolean b) {
                if (o != null) {
                    if (b) {
                        o = null;
                    }
                    if (o == null) { /* */
                    }
                }
            }
        }
    }

    // test0319_if_else_dead_branch
    static class DoNotReport35 {
        static class X {
            void foo(Object o, boolean b) {
                if (false) {
                    o = null;
                    if (o == null) { /* */
                    }
                }
            }
        }
    }

    // test0320_if_else
    static class Report59 {
        static class X {
            void foo(Object o) {
                o.toString();
                if (o == null) { /* */
                }
            }
        }
    }

    // test0321_if_else
    static class DoNotReport36 {
        static class X {
            void foo(Object o, boolean b) {
                Object other = new Object();
                if (b) {
                    other = o;
                }
                if (o != null) { /* */
                }
            }
        }
    }

    // test0322_if_else
    static class Report60 {
        static class X {
            void foo(Object o, boolean b) {
                o.toString();
                if (b) { /* */
                }
                if (o == null) { /* */
                }
            }
        }
    }

    // test0323_if_else
    static class DoNotReport37 {
        static class X {
            void foo(Object o, boolean b) {
                if (o == null && b) {
                    o = new Object();
                }
                if (o == null) { /* */
                }
            }
        }
    }

    // test0324_if_else_nested
    static class DoNotReport38 {
        static class X {
            void foo(boolean b) {
                String s = null;
                if (b) {
                    if (b) {
                        s = "1";
                    } else {
                        s = "2";
                    }
                } else if (b) {
                    s = "3";
                } else {
                    s = "4";
                }
                s.toString();
            }
        }
    }

    // test0325_if_else_nested
    static class Report61 {
        static class X {
            void foo(boolean b) {
                String s = null;
                if (b) {
                    if (b) {
                        s = "1";
                    } else {
                        s = "2";
                    }
                } else if (b) {
                    if (b) {
                        s = "3";
                    }
                } else {
                    s = "4";
                }
                s.toString();
            }
        }
    }

    // test0326_if_else
    static class Report62 {
        static class X {
            void foo(boolean b) {
                String s1 = null;
                if (b) {
                    s1 = "1";
                }
                s1.toString();
            }
        }
    }

    // test0327_if_else
    static class Report63 {
        static class X {
            void foo(String s1) {
                String s2 = null;
                if (s1 == null) {
                    s1 = "1";
                    s2 = "2";
                }
                s1.toString();
                s2.toString();
            }
        }
    }

    // test0328_if_else
    static class Report64 {
        static class X {
            void foo(Object o, boolean b) {
                if (o != null || b) {
                    if (b) {
                        o = new Object();
                    }
                } else { /* */
                }
                o.toString();
            }
        }
    }

    // test0329_if_else_nested
    static class Report65 {
        static class X {
            void foo(Object o, boolean b) {
                if (b) {
                    if (o != null) { /* */
                    }
                }
                o.toString();
            }
        }
    }

    // test0330_if_else_nested
    static class DoNotReport39 {
        static class X {
            void foo(Object o, boolean b) {
                if (b) {
                    if (o == null) {
                        o = new Object();
                    }
                }
                o.toString();
            }
        }
    }

    // test0331_if_else_nested
    static class DoNotReport40 {
        static class X {
            void foo(Object o1, Object o2) {
                Object o3 = o2;
                if (o1 != null) {
                    o3.toString(); // guards o3
                }
                o1 = o3;
                if (o1 != null) { /* */
                }
            }
        }
    }

    // test0332_if_else
    static class Report66 {
        static class X {
            void foo(Object o, boolean b) {
                o = new Object();
                if (b) {
                    o = new Object();
                }
                if (o != null) { /* */
                }
            }
        }
    }

    // test0333_if_else
    static class Report67 {
        static class X {
            void foo(Object o) {
                o = new Object();
                if (o != null) {
                    o.toString();
                }
                o.toString();
            }
        }
    }

    // test0334_if_else
    static class Report68 {
        static class X {
            void foo(Object o) {
                o = new Object();
                if (o != null) {
                    o.toString();
                } else {
                    o.toString();
                }
                o.toString();
            }
        }
    }

    // _test0335_if_else
    static class Report69 {
        static class X {
            void foo(Object o) {
                if (o != null) {
                    if (o != null) {
                        o.toString();
                    }
                    o.toString();
                }
            }
        }
    }

    // test0336_if_else
    static class Report70 {
        static class X {
            void foo(Object o) {
                if (o != null) {
                    if (o != null) {
                        o.toString();
                    } else {
                        o.toString();
                    }
                    o.toString();
                }
            }
        }
    }

    // _test0337_if_else_nested_correlation
    static class DoNotReport41 {
        static class X {
            public int foo(Object o1, Object o2) {
                int result = 0;
                if (o1 == null && o2 != null) {
                    result = -1;
                } else {
                    if (o1 == null && o2 == null) {
                        result = 0;
                    } else {
                        if (o1 != null && o2 == null) {
                            result = 1;
                        } else {
                            int lhs = ((Y) o1).foo(); // may be null
                            int rhs = ((Y) o2).foo();
                            result = lhs - rhs;
                        }
                    }
                }
                return result;
            }
        }

        abstract class Y {
            abstract int foo();
        }

    }

    // test0338_if_else_nested
    static class DoNotReport42 {
        static class X {
            public int foo(Object o1, Object o2) {
                int result = 0;
                if (o1 == null && o2 == null) {
                    result = 0;
                } else {
                    if (o1 == null) {
                        result = -1;
                    } else {
                        if (o2 == null) {
                            result = 1;
                        } else {
                            int lhs = ((Y) o1).foo();
                            int rhs = ((Y) o2).foo();
                            result = lhs - rhs;
                        }
                    }
                }
                return result;
            }
        }

        abstract class Y {
            abstract int foo();
        }

    }

    // test0339_if_else_nested
    static class Report71 {
        static class X {
            void foo(Object o, boolean b) {
                if (o == null || b) {
                    if (bar() == o) {
                        o.toString();
                    }
                }
            }

            Object bar() {
                return new Object();
            }
        }
    }

    // test0340_if_else_nested
    static class Report72 {
        static class X {
            void foo(Object o) {
                if (o == null) {
                    if (bar() == o) {
                        o.toString();
                    }
                }
            }

            Object bar() {
                return new Object();
            }
        }
    }

    // test0341_if_else_nested
    static class Report73 {
        static class X {
            void foo(Object o1, Object o2, boolean b) {
                if (o1 == null || b) {
                    if (o1 == o2) {
                        o1.toString();
                    }
                }
            }
        }
    }

    // test0342_if_else_nested
    static class Report74 {
        static class X {
            void foo(Object o1, Object o2, boolean b) {
                if (o1 == null || b) {
                    if (o2 == o1) {
                        o1.toString();
                    }
                }
            }
        }
    }

    // test0401_while
    static class Report75 {
        static class X {
            void foo() {
                Object o = null;
                while (o.toString() != null) {/* */
                }
            }
        }

    }

    // test0402_while
    static class Report76 {
        static class X {
            void foo() {
                Object o = null;
                while (o != null) {/* */
                }
            }
        }

    }

    // test0403_while
    static class DoNotReport43 {
        static class X {
            void foo() {
                Object o = null;
                while (o == null) {
                    o = new Object();
                }
            }
        }

    }

    // test0404_while
    static class DoNotReport44 {
        static class X {
            void foo() {
                Object o = null;
                while (o == null) {
                    if (System.currentTimeMillis() > 10L) {
                        o = new Object();
                    }
                }
            }
        }

    }

    // test0405_while
    static class Report77 {
        static class X {
            boolean bar() {
                return true;
            }

            void foo(Object o) {
                while (bar() && o == null) {
                    o.toString();
                    o = new Object();
                }
            }
        }

    }

    // test0406_while
    static class Report78 {
        static class X {
            boolean dummy;

            void foo(Object o) {
                o = null;
                while (dummy || o != null) { /* */
                }
            }
        }

    }

    // test0407_while
    static class Report79 {
        static class X {
            boolean dummy;

            void foo() {
                Object o = null;
                while (dummy) {
                    o.toString();
                    o = new Object();
                }
            }
        }

    }

    // test0408_while
    static class DoNotReport45 {
        static class X {
            void foo() {
                Object o = null, u = new Object(), v = new Object();
                while (o == null) {
                    if (v == null) {
                        o = new Object();
                    }
                    ;
                    if (u == null) {
                        v = null;
                    }
                    ;
                    u = null;
                }
            }
        }

    }

    // test0409_while
    static class Report80 {
        static class X {
            boolean dummy;

            void foo() {
                Object o = null;
                while (dummy || (o = new Object()).equals(o)) {
                    o.toString();
                }
            }
        }

    }

    // test0410_while_nested
    static class DoNotReport46 {
        static class X {
            boolean dummy;

            void foo() {
                Object o = null;
                while (dummy) {
                    while (o != null) {
                        o.toString();
                    }
                    if (System.currentTimeMillis() > 10L) {
                        o = new Object();
                    }
                }
            }
        }

    }

    // test0411_while_nested
    static class ReportAnyway47 {
        static class X {
            void foo() {
                Object o = null, u = new Object(), v = new Object();
                while (o == null) {
                    if (v == null) {
                        o = new Object();
                    }
                    ;
                    while (o == null) { // we never exit this loop; o is always
                                        // null
                        if (u == null) {
                            v = null;
                        }
                        ;
                        u = null;
                    }
                }
            }
        }

    }

    // test0412_while_if_nested
    static class Report81 {
        static class X {
            boolean dummy, other;

            void foo() {
                Object o = null;
                while (dummy) {
                    if (other) {
                        o.toString();
                    }
                    o = new Object();
                }
            }
        }

    }

    // test0413_while_unknown_field
    static class DoNotReport48 {
        static class X {
            Object o;

            void foo(boolean dummy) {
                while (dummy) {
                    o = null;
                }
                o.toString();
            }
        }

    }

    // test0414_while_unknown_parameter
    static class Report82 {
        static class X {
            boolean dummy;

            void foo(Object o) {
                while (dummy) {
                    o = null;
                }
                o.toString();
            }
        }

    }

    // test0415_while_unknown_if_else
    static class DoNotReport49 {
        static class X {
            boolean dummy;

            void foo() {
                Object o = null;
                if (dummy) {
                    o = new Object();
                }
                while (dummy) {
                    if (o == null) {/* */
                    }
                }
            }
        }

    }

    // test0416_while
    static class Report83 {
        static class X {
            boolean dummy;

            void foo() {
                Object o = null;
                while (dummy) {
                    o = new Object();
                }
                o.toString();
            }
        }

    }

    // test0417_while
    static class Report84 {
        static class X {
            boolean dummy;

            void foo() {
                Object o = null;
                while (dummy) { /* */
                }
                o.toString();
            }
        }

    }

    // test0418_while_try
    static class DoNotReport50 {
        static class X {
            boolean bool() {
                return true;
            }

            void foo() {
                Object o = null;
                while (bool()) {
                    try {
                        if (o == null) {
                            o = new Object();
                        }
                    } finally { /* */
                    }
                }
            }
        }
    }

    // test0419_while
    static class Report85 {
        static class X {
            boolean bool;

            void foo(Object o) {
                while (bool) {
                    o.toString();
                    o = null;
                }
            }
        }
    }

    // test0420_while
    static class Report86 {
        static class X {
            boolean bool;

            void foo(Object compare) {
                Object o = new Object();
                while ((o = null) == compare) {
                    if (true) {
                        break;
                    }
                }
                if (o == null) { /* */
                }
            }
        }
    }

    // test0421_while
    static class DoNotReport51 {
        static class X {
            boolean bool;

            void foo(Object compare) {
                Object o = null;
                while (bool) {
                    o = new Object();
                    o.toString();
                }
            }
        }
    }

    // test0422_while
    static class Report87 {
        static class X {
            boolean bool;

            void foo() {
                Object o;
                while (bool) {
                    o = new Object();
                    if (o == null) { /* */
                    }
                    o = null;
                }
            }
        }
    }

    // test0423_while
    static class Report88 {
        static class X {
            boolean bool;

            void foo() {
                Object o = null;
                while (bool) {
                    o = new Object();
                    if (o == null) { /* */
                    }
                    o = null;
                }
            }
        }
    }

    // test0424_while_try
    static class DoNotReport52 {
        static class X {
            void foo(boolean b) {
                Object o = null;
                while (o == null) {
                    try { /* */
                    } finally {
                        if (b) {
                            o = new Object();
                        }
                    }
                }
            }
        }

    }

    // test0425_while
    static class Report89 {
        static class X {
            boolean dummy;

            void foo(Object u) {
                Object o = null;
                while (dummy) {
                    o = u;
                }
                o.toString();
            }
        }

    }

    // test0426_while
    static class Report90 {
        static class X {
            boolean dummy;

            void foo(Object o) {
                o.toString();
                while (dummy) { /* */
                }
                if (o == null) { /* */
                }
            }
        }

    }

    // test0427_while_return
    static class Report91 {
        static class X {
            boolean dummy;

            void foo() {
                Object o = null;
                while (dummy) {
                    if (o == null) {
                        return;
                    }
                }
            }
        }

    }

    // test0428_while
    static class DoNotReport53 {
        static class X {
            X bar() {
                return null;
            }

            void foo(X x) {
                x.bar();
                while (x != null) {
                    x = x.bar();
                }
            }
        }

    }

    // test0429_while_nested
    static class DoNotReport54 {
        static class X {
            boolean dummy;

            void foo(X[] xa) {
                while (dummy) {
                    xa = null;
                    if (dummy) {
                        xa = new X[5];
                    }
                    if (xa != null) {
                        int i = 0;
                        while (dummy) {
                            X x = xa[i++];
                            x.toString();
                        }
                    }
                }
            }
        }
    }

    // test0430_while_for_nested
    static class DoNotReport55 {
        static class X {
            boolean dummy;

            void foo(X[] xa) {
                while (dummy) {
                    xa = null;
                    if (dummy) {
                        xa = new X[5];
                    }
                    if (xa != null) {
                        for (int i = 0; i < xa.length; i++) {
                            X x = xa[i];
                            x.toString();
                        }
                    }
                }
            }
        }
    }

    // test0431_while
    static class DoNotReport56 {
        static class X {
            boolean dummy;

            void foo(X x) {
                x = null;
                while (dummy) {
                    x = bar();
                    x.toString();
                }
            }

            X bar() {
                return null;
            }
        }
    }

    // test0432_while
    static class DoNotReport57 {
        static class X {
            boolean dummy;

            void foo(X x) {
                while (dummy) {
                    x = bar();
                    x.toString();
                }
            }

            X bar() {
                return null;
            }
        }
    }

    // test0433_while
    static class Report92 {
        static class X {
            boolean dummy;

            void foo(X x) {
                x = null;
                while (dummy) {
                    x.toString();
                    x.toString();
                }
            }
        }
    }

    // test0434_while_switch_nested
    static class DoNotReport58 {
        static class X {
            Object bar() {
                return new Object();
            }

            void foo(boolean b, int selector) {
                Object o = null;
                while (b) {
                    switch (selector) {
                    case 0:
                        o = bar();
                        if (o != null) {
                            return;
                        }
                    }
                }
            }
        }
    }

    // test0435_while_init
    static class DoNotReport59 {
        static class X {
            int f1;

            X f2;

            void foo(X x1, boolean b) {
                X x2;
                x2 = x1;
                while (b) {
                    if (x2.toString().equals("")) { /* */
                    }
                    x2 = x2.f2;
                }
            }
        }
    }

    // test0436_while_init
    static class DoNotReport60 {
        static class X {
            int f1;

            X f2;

            void foo(X x1, boolean b) {
                X x2 = x1;
                while (b) {
                    if (x2.f1 > 0) { /* */
                    }
                    x2 = x2.f2;
                }
            }
        }
    }

    // test0437_while_exit
    static class DoNotReport61 {
        static class X {
            void foo(boolean b) {
                Object o = null;
                while (b) {
                    if (b) {
                        o = new Object();
                    }
                    if (o != null) {
                        throw new RuntimeException();
                    }
                }
            }
        }
    }

    // test0438_while
    static class Report93 {
        static class X {
            void foo(Object o) {
                while (o == null) { /* */
                }
                o.toString();
                if (o != null) { /* */
                }
            }
        }
    }

    // test0439_while
    static class Report94 {
        static class X {
            void foo(Object o) {
                while (o == null) {
                    o = new Object();
                }
                o.toString();
            }
        }
    }

    // test0440_while
    static class Report95 {
        static class X {
            void foo(Object o) {
                while (o == null) {
                    o = new Object();
                }
                if (o != null) { /* */
                }
            }
        }
    }

    // test0441_while
    static class Report96 {
        static class X {
            X bar() {
                return new X();
            }

            void foo(Object o) {
                while (o == null) {
                    o = bar();
                }
                if (o != null) { /* */
                }
            }
        }
    }

    // test0442_while
    static class Report97 {
        static class X {
            boolean bar() {
                return true;
            }

            void foo(Object o) {
                while (o == null && bar()) { /* */
                }
                o.toString();
            }
        }
    }

    // test0443_while_nested
    static class DoNotReport62 {
        static class X {
            void foo() {
                Object o = null;
                ext: for (int i = 0; i < 5; i++) {
                    if (o != null) {
                        break;
                    }
                    o = new Object();
                    int j = 0;
                    while (j++ < 2) {
                        continue ext;
                    }
                    return;
                }
            }
        }
    }

    // test0444_while_deeply_nested
    static class DoNotReport63 {
        static class X {
            void foo(boolean b) {
                Object o = null;
                ext: for (int i = 0; i < 5; i++) {
                    if (o != null) {
                        break;
                    }
                    do {
                        o = new Object();
                        int j = 0;
                        while (j++ < 2) {
                            continue ext;
                        }
                    } while (b);
                    return;
                }
            }
        }
    }

    // test0445_while_deeply_nested
    static class Report98 {
        static class X {
            void foo(boolean b) {
                Object o = null;
                ext: for (int i = 0; i < 5; i++) {
                    if (o != null) {
                        break;
                    }
                    do {
                        // o = new Object();
                        int j = 0;
                        while (j++ < 2) {
                            continue ext;
                        }
                    } while (b);
                    return;
                }
            }
        }
    }

    // test0446_while
    static class Report99 {
        static class X {
            void foo(Object o, boolean b) {
                while (o == null || b) {
                    o = new Object();
                }
                if (o != null) { /* */
                }
            }
        }
    }

    // test0447_while
    static class DoNotReport64 {
        static class X {
            void foo(Object o, boolean b) {
                while (o == null & b) {
                    o = new Object();
                }
                if (o != null) { /* */
                }
            }
        }
    }

    // test0448_while
    static class Report100 {
        static class X {
            void foo(boolean b[]) {
                Object o = null;
                ext: for (int i = 0; i < 5; i++) {
                    if (o != null) {
                        break;
                    }
                    while (b[1]) {
                        continue ext;
                    }
                    while (b[2]) {
                        continue ext;
                    }
                    while (b[3]) {
                        continue ext;
                    }
                    while (b[4]) {
                        continue ext;
                    }
                    while (b[5]) {
                        continue ext;
                    }
                    while (b[6]) {
                        continue ext;
                    }
                    return;
                }
            }
        }
    }

    // test0449_while_nested
    static class DoNotReport65 {
        static class X {
            void foo(Object p, boolean b) {
                Object o = new Object();
                while (b) {
                    while (b) {
                        o = p;
                    }
                }
                if (o != null) { /* */
                }
            }
        }
    }

    // test0450_while
    static class Report101 {
        static class X {
            void foo(boolean b) {
                Object o = new Object();
                while (b) {
                    o = new Object();
                }
                if (o != null) { /* */
                }
            }
        }
    }

    // test0451_while_nested
    static class Report102 {
        static class X {
            void foo(boolean b) {
                Object o = new Object();
                while (b) {
                    while (b) {
                        o = new Object();
                    }
                }
                if (o != null) { /* */
                }
            }
        }
    }

    // _test0452_while
    static class Report103 {
        static class X {
            void foo(Object doubt) {
                Object o = null;
                while (true) {
                    if (o == null) {
                        return;
                    }
                    o = doubt;
                }
            }
        }
    }

    // _test0453_while
    static class Report104 {
        static class X {
            void foo(Object doubt, boolean b) {
                Object o1 = null, o2 = null;
                while (true) {
                    if (o1 == null) { /* empty */
                    }
                    if (b) {
                        if (o2 == null) {
                            return;
                        }
                    }
                    o1 = o2 = doubt;
                }
            }
        }
    }

    // test0454_while
    static class DoNotReport66 {
        static class X {
            Object bar() {
                return new Object();
            }

            void foo() {
                Object o = null;
                while (true) {
                    o = bar();
                    if (o != null) {
                        o = new Object();
                    }
                    o = null; // quiet pls
                }
            }
        }
    }

    // test0455_while_nested
    static class Report105 {
        static class X {
            void foo(boolean b) {
                Object o = new Object();
                while (b) {
                    o = new Object();
                }
                if (o != null) { /* */
                }
            }
        }
    }

    // _test0456_while_nested_explicit_label
    static class DoNotReport67 {
        static class X {
            void foo(Object o) {
                while (true) {
                    if (o != null) {
                        o.toString();
                        loop: while (true) {
                            break loop;
                        }
                        o.toString();
                    }
                }
            }
        }
    }

    // test0500_try_finally
    static class DoNotReport68 {
        static class X {
            public Object m;

            void foo() {
                Object o = null;
                try { /* */
                } finally {
                    o = m;
                }
                o.toString();
            }
        }

    }

    // test0501_try_finally
    static class Report106 {
        static class X {
            void foo() {
                Object o = new Object();
                try { /* */
                } finally {
                    o = null;
                }
                o.toString();
            }
        }

    }

    // test0502_try_finally
    static class DoNotReport69 {
        static class X {
            void foo() {
                Object o = null;
                try {
                    System.out.println();
                    o = new Object();
                } finally { /* */
                }
                o.toString();
            }
        }

    }

    // test0503_try_finally
    static class Report107 {
        static class X {
            void foo(X x) {
                x = null;
                try {
                    x = null;
                } finally { /* */
                }
            }
        }

    }

    // test0504_try_finally
    static class Report108 {
        static class X {
            void foo(X x) {
                x = null;
                try {
                } finally {
                    if (x != null) { /* */
                    }
                }
            }
        }

    }

    // test0505_try_finally
    static class DoNotReport70 {
        static class X {
            void foo(X x) {
                x = this;
                try {
                    x = null;
                } finally {
                    if (x == null) {/* */
                    }
                }
            }
        }

    }

    // test0506_try_finally
    static class Report109 {
        static class X {
            void foo(Object o) {
                try { /* */
                } finally {
                    o = new Object();
                }
                if (o == null) { /* */
                }
            }
        }

    }

    // test0507_try_finally
    static class Report110 {
        static class X {
            void foo(Object o, boolean b) {
                try { /* */
                } finally {
                    o.toString();
                }
                if (o == null) {
                    o = new Object();
                }
            }
        }

    }

    // test0508_try_finally
    static class Report111 {
        static class X {
            void foo(Object o) {
                o = null;
                try { /* */
                } finally {
                    o.toString();
                    o.toString();
                }
                o.toString();
            }
        }

    }

    // test0509_try_finally_embedded
    static class Report112 {
        static class X {
            void foo(Object o1) {
                Object o2 = null;
                while (true) {
                    // o2 = o1;
                    try { /* */
                    } finally {
                        o2.toString();
                        o2.toString();
                    }
                    o2.toString();
                }
            }
        }

    }

    // test0510_try_finally
    static class DoNotReport71 {
        static class X {
            void bar() throws Exception {
                // empty
            }

            void foo(Object o, boolean b) throws Exception {
                try {
                    bar();
                    if (b) {
                        o.toString();
                    }
                } finally {
                    if (o != null) {
                        o.toString();
                    }
                }
            }
        }

    }

    // test0511_try_finally
    static class DoNotReport72 {
        static class X {
            void foo(Object o1, boolean b) {
                Object o2 = null;
                if (b) {
                    o2 = new Object();
                }
                try { /* */
                } finally {
                    o2 = o1;
                }
                o2.toString();
            }
        }

    }

    // test0512_try_finally
    static class Report113 {
        static class X {
            void foo(X x) {
                x = null;
                try {
                    x = new X();
                } finally {
                    x.toString();
                }
            }
        }

    }

    // test0513_try_finally
    static class DoNotReport73 {
        static class X {
            X bar() {
                return null;
            }

            Object foo() {
                X x = null;
                try {
                    x = bar();
                    x.toString();
                    return x;
                } finally {
                    if (x != null) {
                        x.toString();
                    }
                }
            }
        }

    }

    // test0514_try_finally
    static class DoNotReport74 {
        static class X {
            X bar() {
                return null;
            }

            Object foo() {
                X x = null;
                try {
                    try {
                        x = bar();
                        x.toString();
                        return x;
                    } finally {
                    }
                } finally {
                    if (x != null) {
                        x.toString();
                    }
                }
            }
        }

    }

    // test0515_try_finally
    static class DoNotReport75 {
        static class X {
            X bar() {
                return null;
            }

            Object foo() {
                X x = null;
                try {
                    try {
                        x = bar();
                        x.toString();
                        return x;
                    } finally {
                        System.out.println();
                    }
                } finally {
                    if (x != null) {
                        x.toString();
                    }
                }
            }
        }

    }

    // test0516_try_finally
    static class DoNotReport76 {
        static class X {
            Object foo() {
                X x = null;
                try {
                    x = new X();
                    return x;
                } finally {
                    if (x != null) {
                        x.toString();
                    }
                }
            }
        }

    }

    // test0517_try_finally
    static class DoNotReport77 {
        static class X {
            Object foo() {
                String s00, s01, s02, s03, s04, s05, s06, s07, s08, s09;
                String s10, s11, s12, s13, s14, s15, s16, s17, s18, s19;
                String s20, s21, s22, s23, s24, s25, s26, s27, s28, s29;
                String s30, s31, s32, s33, s34, s35, s36, s37, s38, s39;
                String s40, s41, s42, s43, s44, s45, s46, s47, s48, s49;
                String s50, s51, s52, s53, s54, s55, s56, s57, s58, s59;
                String s60, s61, s62, s63, s64, s65, s66, s67, s68, s69;
                String s100, s101, s102, s103, s104, s105, s106, s107, s108, s109;
                String s110, s111, s112, s113, s114, s115, s116, s117, s118, s119;
                String s120, s121, s122, s123, s124, s125, s126, s127, s128, s129;
                String s130, s131, s132, s133, s134, s135, s136, s137, s138, s139;
                String s140, s141, s142, s143, s144, s145, s146, s147, s148, s149;
                String s150, s151, s152, s153, s154, s155, s156, s157, s158, s159;
                String s160, s161, s162, s163, s164, s165, s166, s167, s168, s169;
                String s200, s201, s202, s203, s204, s205, s206, s207, s208, s209;
                String s210, s211, s212, s213, s214, s215, s216, s217, s218, s219;
                String s220, s221, s222, s223, s224, s225, s226, s227, s228, s229;
                String s230, s231, s232, s233, s234, s235, s236, s237, s238, s239;
                String s240, s241, s242, s243, s244, s245, s246, s247, s248, s249;
                String s250, s251, s252, s253, s254, s255, s256, s257, s258, s259;
                String s260, s261, s262, s263, s264, s265, s266, s267, s268, s269;
                X x = new X();
                try {
                    return x;
                } finally {
                }
            }
        }

    }

    // test0518_try_finally
    static class Report114 {
        static class X {
            void foo() {
                String s00, s01, s02, s03, s04, s05, s06, s07, s08, s09;
                String s10, s11, s12, s13, s14, s15, s16, s17, s18, s19;
                String s20, s21, s22, s23, s24, s25, s26, s27, s28, s29;
                String s30, s31, s32, s33, s34, s35, s36, s37, s38, s39;
                String s40, s41, s42, s43, s44, s45, s46, s47, s48, s49;
                String s50, s51, s52, s53, s54, s55, s56, s57, s58, s59;
                String s60, s61, s62, s63, s64, s65, s66, s67, s68, s69;
                String s100, s101, s102, s103, s104, s105, s106, s107, s108, s109;
                String s110, s111, s112, s113, s114, s115, s116, s117, s118, s119;
                String s120, s121, s122, s123, s124, s125, s126, s127, s128, s129;
                String s130, s131, s132, s133, s134, s135, s136, s137, s138, s139;
                String s140, s141, s142, s143, s144, s145, s146, s147, s148, s149;
                String s150, s151, s152, s153, s154, s155, s156, s157, s158, s159;
                String s160, s161, s162, s163, s164, s165, s166, s167, s168, s169;
                String s200, s201, s202, s203, s204, s205, s206, s207, s208, s209;
                String s210, s211, s212, s213, s214, s215, s216, s217, s218, s219;
                String s220, s221, s222, s223, s224, s225, s226, s227, s228, s229;
                String s230, s231, s232, s233, s234, s235, s236, s237, s238, s239;
                String s240, s241, s242, s243, s244, s245, s246, s247, s248, s249;
                String s250, s251, s252, s253, s254, s255, s256, s257, s258, s259;
                String s260, s261, s262, s263, s264, s265, s266, s267, s268, s269;
                X x = null;
                try {
                    x = new X();
                } finally {
                    x.toString();
                }
            }
        }

    }

    // test0519_try_finally_constructor_exc
    static class DoNotReport78 {
        static class X {
            public void foo(Y y) throws E {
                try {
                    new Y();
                    y.toString();
                } finally {
                    y = null;
                }
            }
        }

        static class Y {
            Y() throws E {
            }
        }

        static class E extends Exception {
            private static final long serialVersionUID = 1L;
        }

    }

    // test0520_try_finally_constructor_exc
    static class DoNotReport79 {
        static class X {
            public void foo(Y y) throws E {
                try {
                    new Y() {
                        @Override
                        void bar() {
                            // do nothing
                        }
                    };
                    y.toString();
                } finally {
                    y = null;
                }
            }
        }

        abstract static class Y {
            Y() throws E {
            }

            abstract void bar();
        }

        static class E extends Exception {
            private static final long serialVersionUID = 1L;
        }
    }

    // test0550_try_catch
    static class DoNotReport80 {
        static class X {
            void foo() {
                Object o = null;
                try {
                    System.out.println();
                    o = new Object();
                } catch (Throwable t) {
                    return;
                }
                o.toString();
            }
        }

    }

    // test0551_try_catch
    static class Report115 {
        static class X {
            boolean dummy;

            void foo() {
                Object o = new Object();
                try {
                    System.out.println();
                    if (dummy) {
                        o = null;
                        throw new Exception();
                    }
                } catch (Exception e) {
                    o.toString();
                }
            }
        }

    }

    // test0552_try_catch
    static class DoNotReport81 {
        static class X {
            boolean dummy;

            void foo() throws Exception {
                Object o = new Object();
                try {
                    if (dummy) {
                        o = null;
                        throw new Exception();
                    }
                } catch (Exception e) {
                }
                if (o != null) {
                }
            }
        }

    }

    // test0553_try_catch
    static class Report116 {
        static class X {
            boolean dummy, other;

            void foo() {
                Object o = new Object();
                try {
                    if (dummy) {
                        if (other) {
                            throw new LocalException();
                        }
                        o = null;
                        throw new LocalException();
                    }
                } catch (LocalException e) {
                    o.toString();
                }
            }

            class LocalException extends Exception {
                private static final long serialVersionUID = 1L;
            }
        }

    }

    // test0554_try_catch
    static class DoNotReport82 {
        static class X {
            void foo(Object o) throws Exception {
                try {
                    o = null;
                    throwLocalException();
                    throw new Exception();
                } catch (LocalException e) {
                }
                if (o != null) {
                }
            }

            class LocalException extends Exception {
                private static final long serialVersionUID = 1L;
            }

            void throwLocalException() throws LocalException {
                throw new LocalException();
            }
        }

    }

    // test0555_try_catch
    static class Report117 {
        static class X {
            void foo() {
                Object o = new Object();
                try {
                    o = null;
                    throwException();
                } catch (Exception e) {
                    o.toString();
                }
            }

            void throwException() throws Exception {
                throw new Exception();
            }
        }

    }

    // test0556_try_catch
    static class Report118 {
        static class X {
            void foo() {
                Object o = new Object();
                try {
                    o = null;
                    throwException();
                } catch (Throwable t) {
                    o.toString();
                }
            }

            void throwException() throws Exception {
                throw new Exception();
            }
        }

    }

    // test0557_try_catch
    static class Report119 {
        static class X {
            boolean dummy;

            void foo() {
                Object o = new Object();
                try {
                    if (dummy) {
                        o = null;
                        throw new Exception();
                    }
                } catch (Exception e) {
                    o.toString();
                }
            }
        }

    }

    // test0558_try_catch
    static class Report120 {
        static class X {
            boolean dummy;

            void foo() {
                Object o = new Object();
                try {
                    if (dummy) {
                        System.out.print(0);
                        o = null;
                        throw new LocalException();
                    }
                } catch (LocalException e) {
                    o.toString();
                }
            }

            class LocalException extends Exception {
                private static final long serialVersionUID = 1L;
            }
        }

    }

    // test0559_try_catch
    static class Report121 {
        static class X {
            boolean dummy;

            void foo() {
                Object o = new Object();
                try {
                    if (dummy) {
                        o = null;
                        throw new SubException();
                    }
                } catch (LocalException e) {
                    o.toString();
                }
            }

            class LocalException extends Exception {
                private static final long serialVersionUID = 1L;
            }

            class SubException extends LocalException {
                private static final long serialVersionUID = 1L;
            }
        }

    }

    // test0560_try_catch
    static class Report122 {
        static class X {
            Class bar(boolean b) throws ClassNotFoundException {
                if (b) {
                    throw new ClassNotFoundException();
                }
                return null;
            }

            public Class foo(Class c, boolean b) {
                if (c != null)
                    return c;
                if (b) {
                    try {
                        c = bar(b);
                        return c;
                    } catch (ClassNotFoundException e) {
                        // empty
                    }
                }
                if (c == null) { // should complain: c can only be null
                }
                return c;
            }
        }

    }

    // test0561_try_catch_unchecked_exception
    static class Report123 {
        static class X {
            void foo() {
                Object o = null;
                try {
                    o = bar();
                } catch (RuntimeException e) {
                    o.toString();
                }
            }

            private Object bar() {
                return new Object();
            }
        }

    }

    // test0562_try_catch_unchecked_exception
    static class Report124 {
        static class X {
            void foo() {
                LineNumberReader o = null;
                try {
                    o = new LineNumberReader(new FileReader("dummy"));
                } catch (NumberFormatException e) {
                    o.toString();
                } catch (IOException e) {
                }
            }
        }

    }

    // test0563_try_catch
    static class Report125 {
        static class X {
            public void foo(boolean b) {
                Exception ex = null;
                if (b) {
                    try {
                        System.out.println();
                        return;
                    } catch (Exception e) {
                        ex = e;
                    }
                } else {
                    try {
                        System.out.println();
                        return;
                    } catch (Exception e) {
                        ex = e;
                    }
                }
                if (ex == null) {
                }
            }
        }

    }

    // test0601_do_while
    static class Report126 {
        static class X {
            void foo() {
                Object o = null;
                do {/* */
                } while (o.toString() != null);
            }
        }

    }

    // test0602_do_while
    static class Report127 {
        static class X {
            void foo() {
                Object o = null;
                do {/* */
                } while (o != null);
            }
        }

    }

    // test0603_do_while
    static class Report128 {
        static class X {
            void foo() {
                Object o = null;
                do {
                    o = new Object();
                } while (o == null);
            }
        }

    }

    // test0604_do_while
    static class DoNotReport83 {
        static class X {
            void foo() {
                Object o = null;
                do {
                    if (System.currentTimeMillis() > 10L) {
                        o = new Object();
                    }
                } while (o == null);
            }
        }

    }

    // test0605_do_while
    static class Report129 {
        static class X {
            boolean dummy;

            void foo(Object o) {
                o = null;
                do {
                    // do nothing
                } while (dummy || o != null);
            }
        }

    }

    // test0606_do_while
    static class DoNotReport84 {
        static class X {
            void foo() {
                Object o = null, u = new Object(), v = new Object();
                do {
                    if (v == null) {
                        o = new Object();
                    }
                    ;
                    if (u == null) {
                        v = null;
                    }
                    ;
                    u = null;
                } while (o == null);
            }
        }

    }

    // test0607_do_while
    static class Report130 {
        static class X {
            boolean dummy;

            void foo() {
                Object o = null;
                do {
                    o.toString();
                    o = new Object();
                } while (dummy);
            }
        }

    }

    // test0608_do_while
    static class DoNotReport85 {
        static class X {
            boolean dummy;

            void foo() {
                Object o = null;
                do {
                    o = new Object();
                } while (dummy);
                o.toString();
            }
        }

    }

    // test0609_do_while
    static class Report131 {
        static class X {
            boolean dummy;

            void foo() {
                Object o = null;
                do { /* */
                } while (dummy);
                o.toString();
            }
        }

    }

    // test0610_do_while
    static class DoNotReport86 {
        static class X {
            X bar() {
                return null;
            }

            void foo(X x) {
                x.bar();
                do {
                    x = x.bar();
                } while (x != null);
            }
        }

    }

    // test0611_do_while
    static class Report132 {
        static class X {
            X bar() {
                return new X();
            }

            void foo(Object o) {
                do {
                    o = bar();
                } while (o == null);
                if (o != null) { /* */
                }
            }
        }
    }

    // _test0612_do_while
    static class Report133 {
        static class X {
            void foo(Object doubt) {
                Object o = null;
                do {
                    if (o == null) {
                        return;
                    }
                    o = doubt;
                } while (true);
            }
        }
    }

    // _test0613_do_while
    static class DoNotReport87 {
        static class X {
            String f;

            void foo(boolean b) {
                X x = new X();
                do {
                    System.out.println(x.f);
                    if (b) {
                        x = null;
                    }
                } while (x != null);
            }
        }

    }

    // test0701_for
    static class Report134 {
        static class X {
            void foo() {
                Object o = null;
                for (; o.toString() != null;) {/* */
                }
            }
        }

    }

    // test0702_for
    static class Report135 {
        static class X {
            void foo() {
                Object o = null;
                for (; o != null;) {/* */
                }
            }
        }

    }

    // test0703_for
    static class DoNotReport88 {
        static class X {
            void foo() {
                Object o = null;
                for (; o == null;) {
                    o = new Object();
                }
            }
        }

    }

    // test0704_for
    static class DoNotReport89 {
        static class X {
            void foo() {
                Object o = null;
                for (; o == null;) {
                    if (System.currentTimeMillis() > 10L) {
                        o = new Object();
                    }
                }
            }
        }

    }

    // test0705_for
    static class Report136 {
        static class X {
            boolean bar() {
                return true;
            }

            void foo(Object o) {
                for (; bar() && o == null;) {
                    o.toString();
                    o = new Object();
                }
            }
        }

    }

    // test0707_for
    static class DoNotReport90 {
        static class X {
            void foo(Object o) {
                for (; o == null; o.toString()) {
                    o = new Object();
                }
            }
        }

    }

    // test0708_for
    static class Report137 {
        static class X {
            void foo(Object o) {
                for (; o == null; o.toString()) {
                }
            }
        }

    }

    // test0709_for
    static class Report138 {
        static class X {
            void foo(Object o) {
                for (o.toString(); o == null;) { /* */
                }
            }
        }

    }

    // test0710_for
    static class Report139 {
        static class X {
            boolean bar() {
                return true;
            }

            void foo(Object o) {
                o = null;
                for (o.toString(); bar();) {
                }
            }
        }

    }

    // test0711_for
    static class Report140 {
        static class X {
            void foo() {
                Object t[] = null;
                for (Object o : t) {/* */
                }
            }
        }

    }

    // test0712_for
    static class Report141 {
        static class X {
            void foo() {
                Iterable i = null;
                for (Object o : i) {/* */
                }
            }
        }

    }

    // test0713_for
    static class DoNotReport91 {
        static class X {
            void foo() {
                Object t[] = new Object[1];
                for (Object o : t) {/* */
                }
            }
        }

    }

    // test0714_for
    static class DoNotReport92 {
        static class X {
            void foo() {
                Iterable i = new java.util.Vector<Object>();
                for (Object o : i) {/* */
                }
            }
        }

    }

    // test0715_for
    static class Report142 {
        static class X {
            void foo() {
                Iterable i = new java.util.Vector<Object>();
                Object flag = null;
                for (Object o : i) {
                    flag = new Object();
                }
                flag.toString();
            }
        }

    }

    // test0716_for
    static class Report143 {
        static class X {
            void foo() {
                Iterable i = new java.util.Vector<Object>();
                Object flag = null;
                for (Object o : i) { /* */
                }
                flag.toString();
            }
        }

    }

    // test0717_for
    static class Report144 {
        static class X {
            void foo(boolean dummy) {
                Object flag = null;
                for (; dummy;) {
                    flag = new Object();
                }
                flag.toString();
            }
        }

    }

    // test0718_for
    static class Report145 {
        static class X {
            void foo(boolean dummy) {
                Object flag = null;
                for (; dummy;) { /* */
                }
                flag.toString();
            }
        }

    }

    // test0719_for
    static class DoNotReport93 {
        static class X {
            public static final char[] foo(char[] a, char c1, char c2) {
                char[] r = null;
                for (int i = 0, length = a.length; i < length; i++) {
                    char c = a[i];
                    if (c == c1) {
                        if (r == null) {
                            r = new char[length];
                        }
                        r[i] = c2;
                    } else if (r != null) {
                        r[i] = c;
                    }
                }
                if (r == null)
                    return a;
                return r;
            }
        }

    }

    // test0720_for_continue_break
    static class Report146 {
        static class X {
            void foo() {
                Object o = new Object();
                for (int i = 0; i < 10; i++) {
                    if (o == null) {
                        continue;
                    }
                    o = null;
                    break;
                }
            }
        }

    }

    // test0721_for
    static class DoNotReport94 {
        static class X {
            void foo(boolean b) {
                Object o = null;
                for (; b ? (o = new Object()).equals(o) : false;) {
                    /* */
                }
                if (o == null) { /* */
                }
            }
        }

    }

    // test0722_for_return
    static class Report147 {
        static class X {
            void foo(boolean b) {
                Object o = null;
                for (int i = 0; i < 25; i++) {
                    if (b) {
                        if (o == null) {
                            o = new Object();
                        }
                        return;
                    }
                }
            }
        }

    }

    // test0723_for
    static class DoNotReport95 {
        static class X {
            void foo() {
                Object o[] = new Object[1];
                for (int i = 0; i < 1; i++) {
                    if (i < 1) {
                        o[i].toString();
                    }
                }
            }
        }

    }

    // test0724_for_with_initialization
    static class DoNotReport96 {
        static class X {
            X field;

            void foo(X x1) {
                // X x2;
                outer: for (int i = 0; i < 30; i++) {
                    X x2 = x1;
                    do {
                        if (x2.equals(x1)) {
                            continue outer;
                        }
                        x2 = x2.field;
                    } while (x2 != null);
                }
            }
        }

    }

    // test0725_for_with_assignment
    static class DoNotReport97 {
        static class X {
            X field;

            void foo(X x1) {
                X x2;
                outer: for (int i = 0; i < 30; i++) {
                    x2 = x1;
                    do {
                        if (x2.equals(x1)) {
                            continue outer;
                        }
                        x2 = x2.field;
                    } while (x2 != null);
                }
            }
        }

    }

    // test0726_for
    static class DoNotReport98 {
        static class X {
            void foo(X x1) {
                X x2 = null;
                for (int i = 0; i < 5; i++) {
                    if (x2 == null) {
                        x2 = x1;
                    }
                    x2.toString();
                }
            }
        }

    }

    // test0727_for
    static class DoNotReport99 {
        static class X {
            void foo() {
                for (; true;) { /* */
                }
            }
        }

    }

    // test0728_for
    // static class Report148 {
    // static class X {
    // void foo(X x) {
    // for (; true; x.toString()) { /* */ }
    // if (x == null) { /* */ }
    // }
    // }

    // }

    // test0729_for_try_catch_finally
    static class DoNotReport100 {
        static class X {
            X f;

            void bar() throws IOException {
                throw new IOException();
            }

            void foo(boolean b) {
                for (int i = 0; i < 5; i++) {
                    X x = this.f;
                    if (x == null) {
                        continue;
                    }
                    if (b) {
                        try {
                            bar();
                        } catch (IOException e) { /* */
                        } finally {
                            x.toString();
                        }
                    }
                }
            }
        }
    }

    // test0730_for
    static class Report149 {
        static class X {
            void foo(Object o) {
                for (; o == null;) {
                    o = new Object();
                }
                if (o != null) { /* */
                }
            }
        }
    }

    // test0731_for
    static class Report150 {
        static class X {
            X bar() {
                return new X();
            }

            void foo(Object o) {
                for (; o == null;) {
                    o = bar();
                }
                if (o != null) { /* */
                }
            }
        }
    }

    // test0732_for_nested_break
    static class DoNotReport101 {
        static class X {
            void foo(String doubt) {
                for (int i = 0; i < 10; i++) {
                    String s = doubt;
                    if (s != null) {
                        for (int j = 0; j < 1; j++) {
                            break;
                        }
                        s.length();
                    }
                }
            }
        }

    }

    // test0733_for_while_break
    static class DoNotReport102 {
        static class X {
            void foo(String doubt, boolean b) {
                for (int i = 0; i < 10; i++) {
                    String s = doubt;
                    if (s != null) {
                        while (b) {
                            break;
                        }
                        s.length();
                    }
                }
            }
        }

    }

    // test0734_for_while_break
    static class DoNotReport103 {
        static class X {
            void foo(String doubt, boolean b) {
                for (int i = 0; i < 10; i++) {
                    String s = doubt;
                    if (s != null) {
                        do {
                            break;
                        } while (b);
                        s.length();
                    }
                }
            }
        }

    }

    // test0735_for_nested_break
    static class DoNotReport104 {
        static class X {
            void foo(Object[] a, String doubt) {
                for (int i = 0; i < 10; i++) {
                    String s = doubt;
                    if (s != null) {
                        for (Object o : a) {
                            break;
                        }
                        s.length();
                    }
                }
            }
        }

    }

    // test0736_for_embedded_lazy_init
    static class DoNotReport105 {
        static class X {
            public boolean foo() {
                Boolean b = null;
                for (int i = 0; i < 1; i++) {
                    if (b == null) {
                        b = Boolean.TRUE;
                    }
                    if (b.booleanValue()) {
                        return b.booleanValue();
                    }
                }
                return false;
            }
        }
    }

    // test0737_for_unknown_protection
    static class Report151 {
        static class X {
            public boolean foo(Boolean p) {
                Boolean b = null;
                for (int i = 0; i < 1; i++) {
                    if (b == p) {
                        // empty
                    } else {
                        continue;
                    }
                    if (b.booleanValue()) {
                        return b.booleanValue();
                    }
                }
                return false;
            }
        }
    }

    // _test0738_for_unknown_protection
    static class DoNotReport106 {
        static class X {
            public boolean foo(Boolean p) {
                Boolean b = null;
                for (int i = 0; i < 1; i++) {
                    if (b == p) {
                        // empty
                    } else {
                        b = p;
                    }
                    if (b.booleanValue()) {
                        return b.booleanValue();
                    }
                }
                return false;
            }
        }
    }

    // test0800_switch
    static class DoNotReport107 {
        static class X {
            int k;

            void foo() {
                Object o = null;
                switch (k) {
                case 0:
                    o = new Object();
                    break;
                case 2:
                    return;
                }
                if (o == null) { /* */
                }
            }
        }

    }

    // test0801_switch
    static class Report152 {
        static class X {
            int k;

            void foo() {
                Object o = null;
                switch (k) {
                case 0:
                    o = new Object();
                    break;
                default:
                    return;
                }
                if (o == null) { /* */
                }
            }
        }

    }

    // test0802_switch
    static class Report153 {
        static class X {
            int k;

            void foo() {
                Object o = null;
                switch (k) {
                case 0:
                    o.toString();
                    break;
                }
            }
        }

    }

    // test0803_switch
    static class Report154 {
        static class X {
            int k;

            void foo() {
                Object o = null;
                switch (k) {
                case 0:
                    o = new Object();
                case 1:
                    o.toString();
                    break;
                }
            }
        }

    }

    // test0804_switch
    static class Report155 {
        static class X {
            void foo(Object o, int info) {
                o = null;
                switch (info) {
                case 0:
                    o = new Object();
                    break;
                case 1:
                    o = new String();
                    break;
                default:
                    o = new X();
                    break;
                }
                if (o != null) { /* */
                }
            }
        }

    }

    // test0805_switch
    static class DoNotReport108 {
        static class X {
            void foo(X p) {
                X x = this;
                for (int i = 0; i < 5; i++) {
                    switch (i) {
                    case 1:
                        x = p;
                    }
                }
                if (x != null) { /* */
                }
            }
        }

    }

    // _test0900_non_null_protection_tag
    static class ReportAnyway109 {
        static class X {
            void foo(Object o) {
                boolean b = o != null;
                o/* NN */.toString();
                o.toString();
            }
        }

    }

    // _test0901_non_null_protection_tag
    static class ReportAnyway110 {
        static class X {
            void foo(Object o, boolean b) {
                if (b) {
                    o = null;
                }
                o/* NN */.toString();
                if (b) {
                    o = null;
                }
                o/*
                  * NN comment
                  */.toString();
                if (b) {
                    o = null;
                }
                o/*
                  * NN
                  */.toString();
                if (b) {
                    o = null;
                }
                o // NN
                .toString();
            }
        }

    }

    // _test0902_non_null_protection_tag
    static class ReportAnyway111 {
        static class X {
            void foo(Object o, boolean b) {
                if (b) {
                    o = null;
                }
                o/* NON-NULL */.toString();
                if (b) {
                    o = null;
                }
                o/* NON-NULL comment */.toString();
                if (b) {
                    o = null;
                }
                o/*
                  * NON-NULL
                  */.toString();
                if (b) {
                    o = null;
                }
                o // NON-NULL
                .toString();
            }
        }

    }

    // test0903_non_null_protection_tag
    static class Report156 {
        static class X {
            void foo(Object o, boolean b) {
                if (b) {
                    o = null;
                }
                o/* N N */.toString();
                if (b) {
                    o = null;
                }
                o/* NNa */.toString();
                if (b) {
                    o = null;
                }
                o/* aNN */.toString();
                if (b) {
                    o = null;
                }
                o/* NON NULL */.toString();
                if (b) {
                    o = null;
                }
                o/* Non-Null */.toString();
                if (b) {
                    o = null;
                }
                o/* aNON-NULL */.toString();
            }
        }

    }

    // test0905_non_null_protection_tag
    static class Report157 {
        static class X {
            void foo(Object o) {
                boolean b = o != null;
                o.toString();/* NN */
            }
        }

    }

    // test0906_non_null_protection_tag
    static class Report158 {
        static class X {
            void foo(Object o) {
                boolean b = o != null;
                /* NN */o.toString();
            }
        }

    }

    // test0950_assert
    static class DoNotReport112 {
        static class X {
            void foo(Object o) {
                boolean b = o != null;
                assert (o != null);
                o.toString();
            }
        }

    }

    // test0951_assert
    static class Report159 {
        static class X {
            void foo(Object o) {
                assert (o == null);
                o.toString();
            }
        }

    }

    // test0952_assert
    static class Report160 {
        static class X {
            void foo(Object o, boolean b) {
                assert (o != null || b);
                o.toString();
            }
        }

    }

    // test0953_assert_combined
    static class Report161 {
        static class X {
            void foo(Object o1, Object o2) {
                assert (o1 != null && o2 == null);
                if (o1 == null) {
                }
                ;
                if (o2 == null) {
                }
                ;
            }
        }

    }

    // test0954_assert_fake_reachable
    static class Report162 {
        static class X {
            void foo(Object o) {
                assert (false && o != null);
                if (o == null) {
                }
                ;
            }
        }

    }

    // test0955_assert_combined
    static class Report163 {
        static class X {
            void foo(Object o) {
                assert (false || o != null);
                if (o == null) {
                }
                ;
            }
        }

    }

    // test0956_assert_combined
    static class Report164 {
        static class X {
            void foo() {
                Object o = null;
                assert (o != null);
                if (o == null) {
                }
                ;
            }
        }

    }

    // _test0900_notNull_protection_tag
    static class Report165 {
        static class X {
            void foo(/** @notNull */
            Object o) {
                boolean b = o != null;
            }
        }

    }

    // _test0901_notNull_protection_tag
    static class Report166 {
        static class X {
            void foo(Object o) {
                @NonNull
                Object l = o;
            }
        }

    }

    // _test0902_notNull_protection_tag
    static class Report167 {
        static class X {
            void foo(@CheckForNull Object o) {
                @NonNull
                Object l = o;
            }
        }

    }

    // test0903_notNull_protection_tag
    static class DoNotReport113 {
        static class X {
            Object bar() {
                return null;
            }

            void foo() {
                @NonNull
                Object l = bar();
            }
        }

    }

    // _test0904_notNull_protection_tag
    static class Report168 {
        static class X {
            @NonNull
            Object bar() {
                return new Object();
            }

            void foo() {
                Object l = bar();
                if (l == null) { /* empty */
                }
            }
        }

    }

    // _test0905_notNull_protection_tag
    static class Report169 {
        static class X {
            @NonNull
            Object bar() {
                return null;
            }
        }

    }

    // _test0950_nullable_tag
    static class ReportAnyway114 {
        static class X {
            void foo(@CheckForNull Object o) {
                o.toString();
            }
        }

    }

    // _test0951_nullable_tag
    static class ReportAnyway115 {
        static class X {
            void foo(@CheckForNull Object o) {
                Object l = o;
                l.toString();
            }
        }

    }

    // _test0952_nullable_tag
    // static class DoNotReport116 {
    // static class X {
    // void foo(boolean b) {
    // @CheckForNull Object o;
    // if (b) {
    // o = new Object();
    // }
    // o.toString();
    // }
    // }

    // }

    // test1004
    static class Report170 {
        static class X {
            X foo(X x) {
                x.foo(null); // 0
                if (x != null) { // 1
                    if (x == null) { // 2
                        x.foo(null); // 3
                    } else if (x instanceof X) { // 4
                        x.foo(null); // 5
                    } else if (x != null) { // 6
                        x.foo(null); // 7
                    }
                    x.foo(null); // 8
                }
                return this;
            }
        }

    }

    // test1005
    static class DoNotReport117 {
        static class X {
            void foo(Class c) {
                if (c.isArray()) {
                } else if (c == java.lang.String.class) {
                }
            }
        }

    }

    // test1006
    static class DoNotReport118 {
        static class X {
            void foo(X x) {
                if (x == this)
                    return;
                x.foo(this);
            }
        }

    }

    // test1007
    static class DoNotReport119 {
        static class X {
            void foo(X x, X x2) {
                if (x != null)
                    return;
                x = x2;
                if (x == null) {
                }
            }
        }

    }

    // test1008
    static class DoNotReport120 {
        static class X {
            void foo(X x, X x2) {
                if (x != null)
                    return;
                try {
                    x = x2;
                } catch (Exception e) {
                }
                if (x == null) {
                }
            }
        }

    }

    // test1009
    static class Report171 {

        static class X {
            boolean check(String name) {
                return true;
            }

            Class bar(String name) throws ClassNotFoundException {
                return null;
            }

            File baz(String name) {
                return null;
            }

            public Class foo(String name, boolean resolve) throws ClassNotFoundException {

                Class c = bar(name);
                if (c != null)
                    return c;
                if (check(name)) {
                    try {
                        c = bar(name);
                        return c;
                    } catch (ClassNotFoundException e) {
                        // keep searching
                        // only path to here left c unassigned from try block,
                        // means it was assumed to be null
                    }
                }
                if (c == null) {// should complain: c can only be null
                    File file = baz(name);
                    if (file == null)
                        throw new ClassNotFoundException();
                }
                return c;
            }

        }

    }

    // test1010
    static class DoNotReport121 {
        static class X {

            X itself() {
                return this;
            }

            void bar() {
                X itself = this.itself();
                if (this == itself) {
                    System.out.println(itself.toString()); // 1
                } else {
                    System.out.println(itself.toString()); // 2
                }
            }
        }

    }

    // test1011
    static class Report172 {
        static class X {

            X itself() {
                return this;
            }

            void bar() {
                X itself = this.itself();
                if (this == itself) {
                    X other = itself;
                    if (other != null) {
                    }
                    if (other == null) {
                    }
                }
            }
        }

    }

    // test1012
    static class Report173 {
        static class X {

            void foo() {
                Object o = null;
                do {
                    if (o == null) {
                        return;
                    }
                    // o = bar();
                } while (true);
            }

            X bar() {
                return null;
            }
        }
    }

    // test1013
    static class Report174 {
        static class X {
            void foo(X x) {
                if (x == this) {
                    if (x == null) {
                        x.foo(this);
                    }
                }
            }
        }

    }

    // test1014
    static class Report175 {
        static class X {
            void foo(X x) {
                x = null;
                try {
                    x = this;
                } finally {
                    x.foo(null);
                }
            }
        }

    }

    // test1015
    static class DoNotReport122 {
        static class X {
            void foo() {
                Object o = null;
                int i = 1;
                switch (i) {
                case 1:
                    o = new Object();
                    break;
                }
                if (o != null)
                    o.toString();
            }
        }

    }

    // test1016
    static class Report176 {
        static class X {
            void foo(X x) {
                x = null;
                try {
                    x = null;
                } finally {
                    if (x != null) {
                        x.foo(null);
                    }
                }
            }
        }

    }

    // test1017
    static class Report177 {
        static class X {
            void foo(X x) {
                x = this;
                try {
                    x = null;
                } finally {
                    if (x == null) {
                        x.foo(null);
                    }
                }
            }
        }

    }

    // test1018
    static class Report178 {
        static class X {

            void foo() {
                Object o = null;
                do {
                    if (o != null)
                        return;
                    o = null;
                } while (true);
            }

            X bar() {
                return null;
            }
        }
    }

    // test1019
    static class DoNotReport123 {
        static class X {
            public static final char[] replaceOnCopy(char[] array, char toBeReplaced, char replacementChar) {

                char[] result = null;
                for (int i = 0, length = array.length; i < length; i++) {
                    char c = array[i];
                    if (c == toBeReplaced) {
                        if (result == null) {
                            result = new char[length];
                            System.arraycopy(array, 0, result, 0, i);
                        }
                        result[i] = replacementChar;
                    } else if (result != null) {
                        result[i] = c;
                    }
                }
                if (result == null)
                    return array;
                return result;
            }
        }

    }

    // test1021
    static class DoNotReport124 {
        static class X {
            int kind;

            X parent;

            Object[] foo() {
                return null;
            }

            void findTypeParameters(X scope) {
                Object[] typeParameters = null;
                while (scope != null) {
                    typeParameters = null;
                    switch (scope.kind) {
                    case 0:
                        typeParameters = foo();
                        break;
                    case 1:
                        typeParameters = foo();
                        break;
                    case 2:
                        return;
                    }
                    if (typeParameters != null) {
                        foo();
                    }
                    scope = scope.parent;
                }
            }
        }

    }

    // test1022
    static class DoNotReport125 {
        static class X {
            boolean bool() {
                return true;
            }

            void doSomething() {
            }

            void foo() {
                Object progressJob = null;
                while (bool()) {
                    if (bool()) {
                        if (progressJob != null)
                            progressJob = null;
                        doSomething();
                    }
                    try {
                        if (progressJob == null) {
                            progressJob = new Object();
                        }
                    } finally {
                        doSomething();
                    }
                }
            }
        }
    }

    // test1023
    static class Report179 {
        static class X {

            void foo() {
                Object o = new Object();
                while (this != null) {
                    try {
                        o = null;
                        break;
                    } finally {
                        o = new Object();
                    }
                }
                if (o == null)
                    return;
            }
        }

    }

    // test1024
    static class Report180 {
        static class X {

            boolean bool() {
                return true;
            }

            void doSomething() {
            }

            void foo() {
                Object progressJob = null;
                while (bool()) {
                    if (progressJob != null)
                        progressJob = null;
                    doSomething();
                    try {
                        if (progressJob == null) {
                            progressJob = new Object();
                        }
                    } finally {
                        doSomething();
                    }
                }
            }
        }
    }

    // test1025
    static class Report181 {
        static class X {

            void foo() {
                Object o;
                try {
                    o = null;
                } finally {
                    o = new Object();
                }
                if (o == null)
                    return;
            }
        }

    }

    // _test1026
    static class DoNotReport126 {
        static class X {

            public static void main(String[] args) {
                Object o;
                try {
                    o = null;
                } finally {
                    if (args == null)
                        o = new Object();
                }
                if (o == null)
                    System.out.println("SUCCESS");
            }
        }

    }

    // test1027
    static class DoNotReport127 {
        static class X {
            boolean b;

            void foo() {
                Object o = null;
                while (b) {
                    try {
                        o = null;
                    } finally {
                        if (o == null)
                            o = new Object();
                    }
                }
                if (o == null)
                    return;
            }
        }

    }

    // _test1028
    static class DoNotReport128 {
        static class X {
            boolean b;

            void foo() {
                Object o = null;
                while (b) {
                    try {
                        o = null;
                        break;
                    } finally {
                        if (o == null)
                            o = new Object();
                    }
                }
                if (o == null)
                    return;
            }
        }

    }

    // test1029
    static class DoNotReport129 {
        static class X {
            public static void main(String[] args) {
                Object o = null;
                int i = 0;
                while (i++ < 2) {
                    try {
                        if (i == 2)
                            return;
                        o = null;
                    } finally {
                        if (i == 2)
                            System.out.println(o);
                        o = "SUCCESS";
                    }
                }
                if (o == null)
                    return;
            }
        }

    }

    // test1030
    static class Report182 {
        static class X {

            void foo() {
                Object a = null;
                while (true) {
                    a = null;
                    if (a == null) {
                        System.out.println();
                    }
                    a = new Object();
                    break;
                }
            }
        }

    }

    // test1031
    static class Report183 {
        static class X {

            void foo() {
                Object a = null;
                while (true) {
                    a = null;
                    if (a == null) {
                        System.out.println();
                    }
                    a = new Object();
                    break;
                }
                if (a == null) {
                    System.out.println();
                }
            }
        }

    }

    // test1032
    static class Report184 {
        static class X {
            void foo() {
                Object o1 = this;
                Object o3;
                while (o1 != null && (o3 = o1) != null) {
                    o1 = o3;
                }
            }
        }

    }

    // test1033
    static class Report185 {
        static class X {

            void foo() {
                String a, b;
                do {
                    a = "Hello ";
                } while (a != null);
                if (a != null) { /* */
                }
            }
        }

    }

    // test1034
    static class DoNotReport130 {
        public final class X {
            void foo() {
                String rs = null;
                try {
                    rs = "";
                    return;
                } catch (Exception e) {
                } finally {
                    if (rs != null) {
                        try {
                            rs.toString();
                        } catch (Exception e) {
                        }
                    }
                }
                return;
            }
        }

    }

    // test1036
    static class Report186 {
        static class X {

            void foo() {
                Object o = new Object();
                do {
                    o = null;
                } while (o != null);
                if (o == null) {
                    // throw new Exception();
                }
            }
        }

    }

    // test1500
    static class DoNotReport131 {
        static class X {
            void foo(Object o, int i, boolean b, Object u) {
                o.toString();
                switch (i) {
                case 0:
                    if (b) {
                        o = u;
                    } else {
                        o = new Object();
                    }
                    break;
                }
                if (o == null) { /* empty */
                }
            }
        }

    }

    // test1501
    static class DoNotReport132 {
        static class X {
            void foo(Object o, int i, boolean b, Object u) {
                if (b) {
                    o = new Object();
                }
                o.toString();
                switch (i) {
                case 0:
                    if (b) {
                        o = u;
                    } else {
                        o = new Object();
                    }
                    break;
                }
                if (o == null) { /* empty */
                }
            }
        }

    }

    // test1502
    static class DoNotReport133 {
        static class X {
            void foo(Object o, int i, boolean b, Object u) {
                if (b) {
                    o = u;
                }
                o.toString();
                switch (i) {
                case 0:
                    if (b) {
                        o = u;
                    } else {
                        o = new Object();
                    }
                    break;
                }
                if (o == null) { /* empty */
                }
            }
        }

    }

    // test1503
    static class DoNotReport134 {
        static class X {
            void foo(Object o, int i, boolean b, Object u) {
                if (b) {
                    o = u;
                } else {
                    o = new Object();
                }
                o.toString();
                switch (i) {
                case 0:
                    if (b) {
                        o = u;
                    } else {
                        o = new Object();
                    }
                    break;
                }
                if (o == null) { /* empty */
                }
            }
        }

    }

    // test2000_flow_info
    static class Report187 {
        static class X {

            void foo() {
                Object o0 = new Object(), o1 = o0, o2 = o0, o3 = o0, o4 = o0, o5 = o0, o6 = o0, o7 = o0, o8 = o0, o9 = o0, o10 = o0, o11 = o0, o12 = o0, o13 = o0, o14 = o0, o15 = o0, o16 = o0, o17 = o0, o18 = o0, o19 = o0, o20 = o0, o21 = o0, o22 = o0, o23 = o0, o24 = o0, o25 = o0, o26 = o0, o27 = o0, o28 = o0, o29 = o0, o30 = o0, o31 = o0, o32 = o0, o33 = o0, o34 = o0, o35 = o0, o36 = o0, o37 = o0, o38 = o0, o39 = o0, o40 = o0, o41 = o0, o42 = o0, o43 = o0, o44 = o0, o45 = o0, o46 = o0, o47 = o0, o48 = o0, o49 = o0, o50 = o0, o51 = o0, o52 = o0, o53 = o0, o54 = o0, o55 = o0, o56 = o0, o57 = o0, o58 = o0, o59 = o0, o60 = o0, o61 = o0, o62 = o0, o63 = o0, o64 = o0, o65 = o0, o66 = o0, o67 = o0, o68 = o0, o69 = o0;
                if (o65 == null) { /* */
                }
                if (o65 != null) { /* */
                }
            }
        }

    }

    // test2001_flow_info
    static class DoNotReport135 {
        static class X {

            void foo(Object o0, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8,
                    Object o9, Object o10, Object o11, Object o12, Object o13, Object o14, Object o15, Object o16, Object o17,
                    Object o18, Object o19, Object o20, Object o21, Object o22, Object o23, Object o24, Object o25, Object o26,
                    Object o27, Object o28, Object o29, Object o30, Object o31, Object o32, Object o33, Object o34, Object o35,
                    Object o36, Object o37, Object o38, Object o39, Object o40, Object o41, Object o42, Object o43, Object o44,
                    Object o45, Object o46, Object o47, Object o48, Object o49, Object o50, Object o51, Object o52, Object o53,
                    Object o54, Object o55, Object o56, Object o57, Object o58, Object o59, Object o60, Object o61, Object o62,
                    Object o63, Object o64, Object o65, Object o66, Object o67, Object o68, Object o69) {
                if (o65 == null) { /* */
                }
                if (o65 != null) { /* */
                }
            }
        }

    }

    // test2002_flow_info
    static class DoNotReport136 {
        static class X {
            Object m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12, m13, m14, m15, m16, m17, m18, m19, m20, m21, m22, m23,
                    m24, m25, m26, m27, m28, m29, m30, m31, m32, m33, m34, m35, m36, m37, m38, m39, m40, m41, m42, m43, m44, m45,
                    m46, m47, m48, m49, m50, m51, m52, m53, m54, m55, m56, m57, m58, m59, m60, m61, m62, m63;

            void foo(Object o) {
                if (o == null) { /* */
                }
                if (o != null) { /* */
                }
            }
        }

    }

    // test2003_flow_info
    static class DoNotReport137 {
        static class X {
            Object m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12, m13, m14, m15, m16, m17, m18, m19, m20, m21, m22, m23,
                    m24, m25, m26, m27, m28, m29, m30, m31, m32, m33, m34, m35, m36, m37, m38, m39, m40, m41, m42, m43, m44, m45,
                    m46, m47, m48, m49, m50, m51, m52, m53, m54, m55, m56, m57, m58, m59, m60, m61, m62, m63;

            void foo(Object o) {
                o.toString();
            }
        }

    }

    // test2004_flow_info
    // static class Report188 {
    // static class X {
    // Object m0, m1, m2, m3, m4,
    // m5, m6, m7, m8, m9,
    // m10, m11, m12, m13, m14,
    // m15, m16, m17, m18, m19,
    // m20, m21, m22, m23, m24,
    // m25, m26, m27, m28, m29,
    // m30, m31, m32, m33, m34,
    // m35, m36, m37, m38, m39,
    // m40, m41, m42, m43, m44,
    // m45, m46, m47, m48, m49,
    // m50, m51, m52, m53, m54,
    // m55, m56, m57, m58, m59,
    // m60, m61, m62, m63;
    // void foo() {
    // Object o;
    // if (o == null) { /* */ }
    // }
    // }

    // }

    // test2005_flow_info
    static class DoNotReport138 {
        static class X {
            Object m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12, m13, m14, m15, m16, m17, m18, m19, m20, m21, m22, m23,
                    m24, m25, m26, m27, m28, m29, m30, m31, m32, m33, m34, m35, m36, m37, m38, m39, m40, m41, m42, m43, m44, m45,
                    m46, m47, m48, m49, m50, m51, m52, m53, m54, m55, m56, m57, m58, m59, m60, m61, m62, m63;

            void foo(Object o) {
                o = null;
            }
        }

    }

    // test2006_flow_info
    static class DoNotReport139 {
        static class X {
            Object m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12, m13, m14, m15, m16, m17, m18, m19, m20, m21, m22, m23,
                    m24, m25, m26, m27, m28, m29, m30, m31, m32, m33, m34, m35, m36, m37, m38, m39, m40, m41, m42, m43, m44, m45,
                    m46, m47, m48, m49, m50, m51, m52, m53, m54, m55, m56, m57, m58, m59, m60, m61, m62, m63;

            void foo() {
                Object o = null;
            }
        }

    }

    // test2007_flow_info
    static class DoNotReport140 {
        static class X {
            Object m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12, m13, m14, m15, m16, m17, m18, m19, m20, m21, m22, m23,
                    m24, m25, m26, m27, m28, m29, m30, m31, m32, m33, m34, m35, m36, m37, m38, m39, m40, m41, m42, m43, m44, m45,
                    m46, m47, m48, m49, m50, m51, m52, m53, m54, m55, m56, m57, m58, m59, m60, m61, m62, m63;

            void foo() {
                Object o[] = null;
            }
        }

    }

    // test2008_flow_info
    static class DoNotReport141 {
        static class X {
            Object m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12, m13, m14, m15, m16, m17, m18, m19, m20, m21, m22, m23,
                    m24, m25, m26, m27, m28, m29, m30, m31, m32, m33, m34, m35, m36, m37, m38, m39, m40, m41, m42, m43, m44, m45,
                    m46, m47, m48, m49, m50, m51, m52, m53, m54, m55, m56, m57, m58, m59, m60, m61, m62, m63;

            void foo(boolean b) {
                Object o = null;
                while (o == null) {
                    try { /* */
                    } finally {
                        if (b) {
                            o = new Object();
                        }
                    }
                }
            }
        }

    }

    // test2009_flow_info
    static class Report189 {
        static class X {
            Object m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12, m13, m14, m15, m16, m17, m18, m19, m20, m21, m22, m23,
                    m24, m25, m26, m27, m28, m29, m30, m31, m32, m33, m34, m35, m36, m37, m38, m39, m40, m41, m42, m43, m44, m45,
                    m46, m47, m48, m49, m50, m51, m52, m53, m54, m55, m56, m57, m58, m59, m60, m61, m62, m63;

            void foo(Object o) {
                try { /* */
                } finally {
                    o = new Object();
                }
                if (o == null) { /* */
                }
            }
        }

    }

    // test2010_flow_info
    static class Report190 {
        static class X {
            Object m00, m01, m02, m03, m04, m05, m06, m07, m08, m09, m10, m11, m12, m13, m14, m15, m16, m17, m18, m19, m20, m21,
                    m22, m23, m24, m25, m26, m27, m28, m29, m30, m31, m32, m33, m34, m35, m36, m37, m38, m39, m40, m41, m42, m43,
                    m44, m45, m46, m47, m48, m49, m50, m51, m52, m53, m54, m55, m56, m57, m58, m59, m60, m61, m62, m63;

            void foo() {
                Object o;
                try { /* */
                } finally {
                    o = new Object();
                }
                if (o == null) { /* */
                }
            }
        }

    }

    // test2011_flow_info
    static class Report191 {
        static class X {
            Object m000, m001, m002, m003, m004, m005, m006, m007, m008, m009, m010, m011, m012, m013, m014, m015, m016, m017,
                    m018, m019, m020, m021, m022, m023, m024, m025, m026, m027, m028, m029, m030, m031, m032, m033, m034, m035,
                    m036, m037, m038, m039, m040, m041, m042, m043, m044, m045, m046, m047, m048, m049, m050, m051, m052, m053,
                    m054, m055, m056, m057, m058, m059, m060, m061, m062, m063;

            void foo() {
                Object o000, o001, o002, o003, o004, o005, o006, o007, o008, o009, o010, o011, o012, o013, o014, o015, o016, o017, o018, o019, o020, o021, o022, o023, o024, o025, o026, o027, o028, o029, o030, o031, o032, o033, o034, o035, o036, o037, o038, o039, o040, o041, o042, o043, o044, o045, o046, o047, o048, o049, o050, o051, o052, o053, o054, o055, o056, o057, o058, o059, o060, o061, o062, o063;
                Object o;
                try {
                    o000 = new Object();
                } finally {
                    o = new Object();
                }
                if (o == null) { /* */
                }
            }
        }

    }

    // test2012_flow_info
    static class Report192 {
        static class X {
            Object m000, m001, m002, m003, m004, m005, m006, m007, m008, m009, m010, m011, m012, m013, m014, m015, m016, m017,
                    m018, m019, m020, m021, m022, m023, m024, m025, m026, m027, m028, m029, m030, m031, m032, m033, m034, m035,
                    m036, m037, m038, m039, m040, m041, m042, m043, m044, m045, m046, m047, m048, m049, m050, m051, m052, m053,
                    m054, m055, m056, m057, m058, m059, m060, m061, m062, m063;

            void foo() {
                Object o000, o001, o002, o003, o004, o005, o006, o007, o008, o009, o010, o011, o012, o013, o014, o015, o016, o017, o018, o019, o020, o021, o022, o023, o024, o025, o026, o027, o028, o029, o030, o031, o032, o033, o034, o035, o036, o037, o038, o039, o040, o041, o042, o043, o044, o045, o046, o047, o048, o049, o050, o051, o052, o053, o054, o055, o056, o057, o058, o059, o060, o061, o062, o063;
                Object o;
                try {
                    o = new Object();
                } finally {
                    o000 = new Object();
                }
                if (o == null) { /* */
                }
            }
        }

    }

    // test2013_flow_info
    static class Report193 {
        static class X {
            boolean dummy;

            Object m000, m001, m002, m003, m004, m005, m006, m007, m008, m009, m010, m011, m012, m013, m014, m015, m016, m017,
                    m018, m019, m020, m021, m022, m023, m024, m025, m026, m027, m028, m029, m030, m031, m032, m033, m034, m035,
                    m036, m037, m038, m039, m040, m041, m042, m043, m044, m045, m046, m047, m048, m049, m050, m051, m052, m053,
                    m054, m055, m056, m057, m058, m059, m060, m061, m062, m063;

            void foo(Object u) {
                Object o = null;
                while (dummy) {
                    o = u;
                }
                o.toString();
            }
        }

    }

    // test2014_flow_info
    // static class Report194 {
    // static class X {
    // int m000, m001, m002, m003, m004, m005, m006, m007, m008, m009,
    // m010, m011, m012, m013, m014, m015, m016, m017, m018, m019,
    // m020, m021, m022, m023, m024, m025, m026, m027, m028, m029,
    // m030, m031, m032, m033, m034, m035, m036, m037, m038, m039,
    // m040, m041, m042, m043, m044, m045, m046, m047, m048, m049,
    // m050, m051, m052, m053, m054, m055, m056, m057, m058, m059,
    // m060, m061, m062, m063;
    // final int m064;
    // X() {
    // m064 = 10;
    // class Inner extends X {
    // int m100, m101, m102, m103, m104, m105, m106, m107, m108, m109,
    // m110, m111, m112, m113, m114, m115, m116, m117, m118, m119,
    // m120, m121, m122, m123, m124, m125, m126, m127, m128, m129,
    // m130, m131, m132, m133, m134, m135, m136, m137, m138, m139,
    // m140, m141, m142, m143, m144, m145, m146, m147, m148, m149,
    // m150, m151, m152, m153, m154, m155, m156, m157, m158, m159,
    // m160, m161, m162, m163;
    // final int m164;
    // int bar() {
    // return m100 + m101 + m102 + m103 + m104 +
    // m105 + m106 + m107 + m108 + m109 +
    // m110 + m111 + m112 + m113 + m114 +
    // m115 + m116 + m117 + m118 + m119 +
    // m120 + m121 + m122 + m123 + m124 +
    // m125 + m126 + m127 + m128 + m129 +
    // m130 + m131 + m132 + m133 + m134 +
    // m135 + m136 + m137 + m138 + m139 +
    // m140 + m141 + m142 + m143 + m144 +
    // m145 + m146 + m147 + m148 + m149 +
    // m150 + m151 + m152 + m153 + m154 +
    // m155 + m156 + m157 + m158 + m159 +
    // m160 + m161 + m162 + m163 + m164;
    // }
    // };
    // System.out.println((new Inner()).bar());
    // }
    // }

    // }

    // test2015_flow_info
    // static class Report195 {
    // static class X {
    // int m000, m001, m002, m003, m004, m005, m006, m007, m008, m009,
    // m010, m011, m012, m013, m014, m015, m016, m017, m018, m019,
    // m020, m021, m022, m023, m024, m025, m026, m027, m028, m029,
    // m030, m031, m032, m033, m034, m035, m036, m037, m038, m039,
    // m040, m041, m042, m043, m044, m045, m046, m047, m048, m049,
    // m050, m051, m052, m053, m054, m055, m056, m057, m058, m059,
    // m060, m061, m062, m063;
    // final int m200;
    // int m201, m202, m203, m204, m205, m206, m207, m208, m209,
    // m210, m211, m212, m213, m214, m215, m216, m217, m218, m219,
    // m220, m221, m222, m223, m224, m225, m226, m227, m228, m229,
    // m230, m231, m232, m233, m234, m235, m236, m237, m238, m239,
    // m240, m241, m242, m243, m244, m245, m246, m247, m248, m249,
    // m250, m251, m252, m253, m254, m255, m256, m257, m258, m259,
    // m260, m261, m262, m263;
    // int m301, m302, m303, m304, m305, m306, m307, m308, m309,
    // m310, m311, m312, m313, m314, m315, m316, m317, m318, m319,
    // m320, m321, m322, m323, m324, m325, m326, m327, m328, m329,
    // m330, m331, m332, m333, m334, m335, m336, m337, m338, m339,
    // m340, m341, m342, m343, m344, m345, m346, m347, m348, m349,
    // m350, m351, m352, m353, m354, m355, m356, m357, m358, m359,
    // m360, m361, m362, m363;
    // X() {
    // m200 = 10;
    // class Inner extends X {
    // int m100, m101, m102, m103, m104, m105, m106, m107, m108, m109,
    // m110, m111, m112, m113, m114, m115, m116, m117, m118, m119,
    // m120, m121, m122, m123, m124, m125, m126, m127, m128, m129,
    // m130, m131, m132, m133, m134, m135, m136, m137, m138, m139,
    // m140, m141, m142, m143, m144, m145, m146, m147, m148, m149,
    // m150, m151, m152, m153, m154, m155, m156, m157, m158, m159,
    // m160, m161, m162, m163;
    // final int m164;
    // int bar() {
    // return m100 + m101 + m102 + m103 + m104 +
    // m105 + m106 + m107 + m108 + m109 +
    // m110 + m111 + m112 + m113 + m114 +
    // m115 + m116 + m117 + m118 + m119 +
    // m120 + m121 + m122 + m123 + m124 +
    // m125 + m126 + m127 + m128 + m129 +
    // m130 + m131 + m132 + m133 + m134 +
    // m135 + m136 + m137 + m138 + m139 +
    // m140 + m141 + m142 + m143 + m144 +
    // m145 + m146 + m147 + m148 + m149 +
    // m150 + m151 + m152 + m153 + m154 +
    // m155 + m156 + m157 + m158 + m159 +
    // m160 + m161 + m162 + m163 + m164;
    // }
    // };
    // System.out.println((new Inner()).bar());
    // }
    // }

    // }

    // test2016_flow_info
    static class Report196 {
        static class X {
            int m000, m001, m002, m003, m004, m005, m006, m007, m008, m009, m010, m011, m012, m013, m014, m015, m016, m017, m018,
                    m019, m020, m021, m022, m023, m024, m025, m026, m027, m028, m029, m030, m031, m032, m033, m034, m035, m036,
                    m037, m038, m039, m040, m041, m042, m043, m044, m045, m046, m047, m048, m049, m050, m051, m052, m053, m054,
                    m055, m056, m057, m058, m059, m060, m061;

            final int m062;
            {
                int l063, m201 = 0, m202, m203, m204, m205, m206, m207, m208, m209, m210, m211, m212, m213, m214, m215, m216, m217, m218, m219, m220, m221, m222, m223, m224, m225, m226, m227, m228, m229, m230, m231, m232, m233, m234, m235, m236, m237, m238, m239, m240, m241, m242, m243, m244, m245, m246, m247, m248, m249, m250, m251, m252, m253, m254, m255, m256, m257, m258, m259, m260, m261, m262, m263;
                int m301, m302, m303, m304, m305, m306, m307, m308, m309, m310, m311, m312, m313, m314, m315, m316, m317, m318, m319, m320, m321, m322, m323, m324, m325, m326, m327, m328, m329, m330, m331, m332, m333, m334, m335, m336, m337, m338, m339, m340, m341, m342, m343, m344, m345, m346, m347, m348, m349, m350, m351, m352, m353, m354, m355, m356, m357, m358, m359, m360 = 0, m361 = 0, m362 = 0, m363 = 0;
                m062 = m360;
            }

            X() {
                int l0, l1 = 0;
                m000 = l1;
                class Inner extends X {
                    int bar() {
                        return 0;
                    }
                }
                ;
                System.out.println((new Inner()).bar());
            }
        }

    }

    // test2017_flow_info
    static class DoNotReport142 {
        static class X {
            boolean dummy;

            Object m000, m001, m002, m003, m004, m005, m006, m007, m008, m009, m010, m011, m012, m013, m014, m015, m016, m017,
                    m018, m019, m020, m021, m022, m023, m024, m025, m026, m027, m028, m029, m030, m031, m032, m033, m034, m035,
                    m036, m037, m038, m039, m040, m041, m042, m043, m044, m045, m046, m047, m048, m049, m050, m051, m052, m053,
                    m054, m055, m056, m057, m058, m059, m060, m061, m062, m063;

            void foo(Object u) {
                Object o = null;
                while (dummy) {
                    if (dummy) {
                        o = u;
                        continue;
                    }
                }
                if (o != null) { /* */
                }
            }
        }

    }

    // test2018_flow_info
    // static class Report197 {
    // static class X {
    // boolean dummy;
    // Object m000, m001, m002, m003, m004, m005, m006, m007, m008, m009,
    // m010, m011, m012, m013, m014, m015, m016, m017, m018, m019,
    // m020, m021, m022, m023, m024, m025, m026, m027, m028, m029,
    // m030, m031, m032, m033, m034, m035, m036, m037, m038, m039,
    // m040, m041, m042, m043, m044, m045, m046, m047, m048, m049,
    // m050, m051, m052, m053, m054, m055, m056, m057, m058, m059,
    // m060, m061, m062, m063;
    // void foo() {
    // Object o;
    // while (dummy) {
    // if (dummy) {
    // o = null;
    // continue;
    // }
    // }
    // o.toString();
    // }
    // }

    // }

    // test2019_flow_info
    // static class Report198 {
    // static class X {
    // boolean dummy;
    // Object m000, m001, m002, m003, m004, m005, m006, m007, m008, m009,
    // m010, m011, m012, m013, m014, m015, m016, m017, m018, m019,
    // m020, m021, m022, m023, m024, m025, m026, m027, m028, m029,
    // m030, m031, m032, m033, m034, m035, m036, m037, m038, m039,
    // m040, m041, m042, m043, m044, m045, m046, m047, m048, m049,
    // m050, m051, m052, m053, m054, m055, m056, m057, m058, m059,
    // m060, m061, m062, m063;
    // void foo() {
    // Object o;
    // while (dummy) {
    // if (dummy) {
    // continue;
    // }
    // o = null;
    // }
    // o.toString();
    // }
    // }

    // }

    // test2020_flow_info
    // static class Report199 {
    // static class X {
    // boolean dummy;
    // Object m000, m001, m002, m003, m004, m005, m006, m007, m008, m009,
    // m010, m011, m012, m013, m014, m015, m016, m017, m018, m019,
    // m020, m021, m022, m023, m024, m025, m026, m027, m028, m029,
    // m030, m031, m032, m033, m034, m035, m036, m037, m038, m039,
    // m040, m041, m042, m043, m044, m045, m046, m047, m048, m049,
    // m050, m051, m052, m053, m054, m055, m056, m057, m058, m059,
    // m060, m061, m062, m063;
    // int m200, m201, m202, m203, m204, m205, m206, m207, m208, m209,
    // m210, m211, m212, m213, m214, m215, m216, m217, m218, m219,
    // m220, m221, m222, m223, m224, m225, m226, m227, m228, m229,
    // m230, m231, m232, m233, m234, m235, m236, m237, m238, m239,
    // m240, m241, m242, m243, m244, m245, m246, m247, m248, m249,
    // m250, m251, m252, m253, m254, m255, m256, m257, m258, m259,
    // m260, m261;
    // void foo() {
    // Object o0, o1;
    // while (dummy) {
    // o0 = new Object();
    // if (dummy) {
    // o1 = null;
    // continue;
    // }
    // }
    // o1.toString();
    // }
    // }

    // }
}
