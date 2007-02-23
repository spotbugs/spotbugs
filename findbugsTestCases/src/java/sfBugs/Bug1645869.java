package sfBugs;

public class Bug1645869  {
	
	/*
	 * Here's the comment from SourceForge.net
	 * The found bug "Method superfluously delegates to parent class method"
	 * reports a method that only calls the super class version of the same
	 * method. In doing so it makes no distinction between a normal method and a
	 * final method. A final method could be a way to impose the default behavior
	 * inherited from super class. Michele. 
	 * 
	 * The bug found is a low priority and FindBugs makes the distinction now. - Kristin
	 */
	
    boolean booleanMethod() {
        return true;
    }
    byte byteMethod() {
        return 1;
    }
    char charMethod() {
        return 1;
    }
    short shortMethod() {
        return 1;
    }
    int intMethod() {
        return 1;
    }
    long longMethod() {
        return 1;
    }
    double doubleMethod() {
        return 1;
    }
    float floatMethod() {
        return 1;
    }
    Object objectMethod() {
        return null;
    }
    Object[] arrayMethod() {
        return null;
    }
	
	static class Useless extends Bug1645869 {
        boolean booleanMethod() {
            return super.booleanMethod();
        }
        byte byteMethod() {
            return super.byteMethod();
        }
        char charMethod() {
            return  super.charMethod();
        }
        short shortMethod() {
            return super.shortMethod();
        }
        int intMethod() {
            return super.intMethod();
        }
        long longMethod() {
            return super.longMethod();
        }
        double doubleMethod() {
            return super.doubleMethod();
        }
        float floatMethod() {
            return super.floatMethod();
        }
        Object objectMethod() {
            return super.objectMethod();
        }
        Object[] arrayMethod() {
            return super.arrayMethod();
        }
    }
    static class Uselessful extends Bug1645869 {
        final boolean booleanMethod() {
            return super.booleanMethod();
        }
        final byte byteMethod() {
            return super.byteMethod();
        }
        final  char charMethod() {
            return  super.charMethod();
        }
        final short shortMethod() {
            return super.shortMethod();
        }
        final int intMethod() {
            return super.intMethod();
        }
        final long longMethod() {
            return super.longMethod();
        }
        final double doubleMethod() {
            return super.doubleMethod();
        }
        final float floatMethod() {
            return super.floatMethod();
        }
        final Object objectMethod() {
            return super.objectMethod();
        }
        final Object[] arrayMethod() {
            return super.arrayMethod();
        }
    }

}
