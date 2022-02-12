package ghIssues;

import java.util.*;

import edu.umd.cs.findbugs.detect.*;

/**
 * @see {@link ForgotToUpdateHashCodeEqualsToStringDetector}
 * @author lifeinwild1@gmail.com https://github.com/lifeinwild
 */
class Issue1415 {
    /**
     * should be error even if no instance variables because the standard implementation of equals() usually compares concrete class.
     */
    private static class ForgotToUpdateHashcodeEqualsToStringChildNoInstanceVar
            extends ForgotToUpdateHashcodeEqualsToStringParent {
    }

    /**
     * should be error because of no HET methods unlike parent class.
     */
    private static class ForgotToUpdateHashcodeEqualsToStringChildNoMethod
            extends ForgotToUpdateHashcodeEqualsToStringParent {
        private int i2 = 2;
    }

    /**
     * no problem because it supports getter in HET methods.
     */
    private static class ForgotToUpdateHashcodeEqualsToStringGetter {
        /**
         * i1 appears in HET methods by its getter. 
         */
        private int i1 = 1;

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ForgotToUpdateHashcodeEqualsToStringGetter other = (ForgotToUpdateHashcodeEqualsToStringGetter) obj;
            if (getI1() != other.getI1())
                return false;
            return true;
        }

        public int getI1() {
            return i1;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getI1();
            return result;
        }

        @Override
        public String toString() {
            return "ForgotToUpdateHashcodeEqualsToStringGetter [i1=" + getI1() + "]";
        }

    }

    /**
     * should be error because of no HET methods with interface has HET methods.
     */
    private static class ForgotToUpdateHashcodeEqualsToStringImplementation
            implements ForgotToUpdateHashcodeEqualsToStringInterface {
    }

    /**
     * implementation classes should implement HET methods because this interface has HET method signatures.
     */
    private static interface ForgotToUpdateHashcodeEqualsToStringInterface {
        @Override
        boolean equals(Object obj);

        @Override
        int hashCode();

        @Override
        String toString();
    }

    /**
     * no problem because of no parent or outer class or interface that urge HET methods.
     */
    private static class ForgotToUpdateHashcodeEqualsToStringNoMethod {
        private int i2 = 2;
    }

    /**
     * no problem because of proper implementation of HET methods.
     */
    private static class ForgotToUpdateHashcodeEqualsToStringNormal {
        private int i1 = 1;

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ForgotToUpdateHashcodeEqualsToStringNormal other = (ForgotToUpdateHashcodeEqualsToStringNormal) obj;
            if (i1 != other.i1)
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + i1;
            return result;
        }

        @Override
        public String toString() {
            return "ForgotToUpdateHashcodeEqualsToStringNormal [i1=" + i1 + "]";
        }

    }

    /**
     * should be error because of incomplete HET methods.
     */
    private static class ForgotToUpdateHashcodeEqualsToStringNotAppear {
        /**
         * i2 doesn't appear in HET methods. 
         * it might means that programmer forgot to update HET methods when added i2.
         * 
         * {@link ForgotToUpdateHashCodeEqualsToStringDetector#HE_MEMBER_DOESNT_APPEAR_IN_EQUALS}
         * {@link ForgotToUpdateHashCodeEqualsToStringDetector#HE_MEMBER_DOESNT_APPEAR_IN_HASHCODE}
         * {@link ForgotToUpdateHashCodeEqualsToStringDetector#USELESS_STRING_MEMBER_DOESNT_APPEAR_IN_TOSTRING}
         */
        private int i2 = 2;

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ForgotToUpdateHashcodeEqualsToStringNotAppear other = (ForgotToUpdateHashcodeEqualsToStringNotAppear) obj;
            // if (i2 != other.i2)
            // return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result;// + i2;
            return result;
        }

        @Override
        public String toString() {
            return "ForgotToUpdateHashcodeEqualsToStringNotAppear []";
        }

    }

    private static class ForgotToUpdateHashcodeEqualsToStringOuter {
        private int outerMember;

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ForgotToUpdateHashcodeEqualsToStringOuter other = (ForgotToUpdateHashcodeEqualsToStringOuter) obj;
            if (outerMember != other.outerMember)
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + outerMember;
            return result;
        }

        @Override
        public String toString() {
            return "ForgotToUpdateHashcodeEqualsToStringOuter [outerMember=" + outerMember + "]";
        }

        /**
         * should be error because of no HET methods with outer class that has HET methods.
         */
        private class ForgotToUpdateHashcodeEqualsToStringInner {
            private int innerMember = 0;
        }

        /**
         * should be error because of incomplete HET methods.
         */
        private class ForgotToUpdateHashcodeEqualsToStringInner2 {
            private int innerMember = 0;

            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                ForgotToUpdateHashcodeEqualsToStringInner2 other = (ForgotToUpdateHashcodeEqualsToStringInner2) obj;
                if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
                    return false;
                // if (innerMember != other.innerMember)
                // return false;
                return true;
            }

            private ForgotToUpdateHashcodeEqualsToStringOuter getEnclosingInstance() {
                return ForgotToUpdateHashcodeEqualsToStringOuter.this;
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + getEnclosingInstance().hashCode();
                // result = prime * result + innerMember;
                return result;
            }

            @Override
            public String toString() {
                return "ForgotToUpdateHashcodeEqualsToStringInner2 []";
            }

        }
    }

    private static class ForgotToUpdateHashcodeEqualsToStringOuter2 {
        private int outerMember;

        /**
         * no problem because of no HET methods in outer class.
         */
        private class ForgotToUpdateHashcodeEqualsToStringInner3 {
            private int innerMember = 0;
        }
    }

    /**
     * child classes should implement HET methods because this parent class has HET methods.
     */
    private static abstract class ForgotToUpdateHashcodeEqualsToStringParent {
        private int parentMember = 0;

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ForgotToUpdateHashcodeEqualsToStringParent that = (ForgotToUpdateHashcodeEqualsToStringParent) o;
            return parentMember == that.parentMember;
        }

        @Override
        public int hashCode() {
            return Objects.hash(parentMember);
        }

        @Override
        public String toString() {
            return "ForgotToUpdateHashcodeEqualsToStringParent{" + "parentMember=" + parentMember + '}';
        }
    }

    /**
     * no problem because of proper implementations of HET methods.
     */
    private static class ForgotToUpdateHashcodeEqualsToStringProperChild
            extends ForgotToUpdateHashcodeEqualsToStringParent {
        private int childMember = 0;

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj))
                return false;
            if (getClass() != obj.getClass())
                return false;
            ForgotToUpdateHashcodeEqualsToStringProperChild other = (ForgotToUpdateHashcodeEqualsToStringProperChild) obj;
            if (childMember != other.childMember)
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + childMember;
            return result;
        }

        @Override
        public String toString() {
            return "ForgotToUpdateHashcodeEqualsToStringProperChild [childMember=" + childMember + "]";
        }
    }

    /**
     * static member should be no problem.
     */
    private static class ForgotToUpdateHashcodeEqualsToStringStatic {
        /**
         * static member should be no problem in these rules.
         */
        private static int i4 = 4;

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        @Override
        public String toString() {
            return super.toString();
        }

    }

    /**
     * transient member should be no problem.
     */
    private static class ForgotToUpdateHashcodeEqualsToStringTransient {
        private transient int i3 = 3;

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }

    /**
     * no problem because of dummy method.
     */
    private static class HETMethodsAsDummy {
        private int i;

        @Override
        public boolean equals(Object obj) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            throw new UnsupportedOperationException();
        }
    }
}
