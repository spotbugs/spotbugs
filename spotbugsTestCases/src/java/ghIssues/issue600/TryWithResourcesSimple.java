package ghIssues.issue600;

public class TryWithResourcesSimple {
    public int nocatchInterface(IMyAutocloseable x) throws Exception {
        try (IMyAutocloseable closeable = x) {
            return closeable.hashCode();
        }
    }

    public void nocatch() throws Exception {
        try (MyAutocloseable closeable = MyAutocloseable.create()) {
            closeable.exampleMethod();
        }
    }

    public void nocatchFormat1() throws Exception {
        try
                (MyAutocloseable closeable = MyAutocloseable.create()) {
            closeable.exampleMethod();
        }
    }

    public void nocatchFormat2() throws Exception {
        try
                (MyAutocloseable closeable = MyAutocloseable.create())
        {
            closeable.exampleMethod();
        }
    }

    public void nocatchFormat3() throws Exception {
        try
                (
                        MyAutocloseable closeable = MyAutocloseable.create())
        {
            closeable.exampleMethod();
        }
    }

    public void nocatchFormat4() throws Exception {
        try
                (
                        MyAutocloseable closeable = MyAutocloseable.create()
                )
        {
            closeable.exampleMethod();
        }
    }

    public void catchAndSuppress() {
        try (MyAutocloseable closeable = MyAutocloseable.create()) {
            closeable.exampleMethod();
        } catch (IllegalArgumentException e) {
            System.out.println(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void catchAndSuppressFormat1() {
        try (
                MyAutocloseable closeable = MyAutocloseable.create()) {
            closeable.exampleMethod();
        } catch (IllegalArgumentException e) {
            System.out.println(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void catchAndSuppressFormat2() {
        try (
                MyAutocloseable closeable = MyAutocloseable.create())
        {
            closeable.exampleMethod();
        } catch (IllegalArgumentException e) {
            System.out.println(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void catchAndSuppressFormat3() {
        try
                (
                MyAutocloseable closeable = MyAutocloseable.create()
                )
        {
            closeable.exampleMethod();
        } catch (IllegalArgumentException e) {
            System.out.println(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void catchAndSuppressFormat4() {
        try
                (
                        MyAutocloseable closeable = MyAutocloseable.create()
                )
        {
            closeable.exampleMethod();
        }
        catch (IllegalArgumentException e)
        {
            System.out.println(e);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void catchAndThrowException() throws Exception {
        try (MyAutocloseable closeable = MyAutocloseable.create()) {
            closeable.exampleMethod();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void catchAndThrowExceptionFormat1() throws Exception {
        try
                (MyAutocloseable closeable = MyAutocloseable.create())
        {
            closeable.exampleMethod();

        } catch (IllegalArgumentException e)
        {
            throw new RuntimeException(e);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }
    }

    public void catchAndThrowThrowable() throws Throwable {
        try (MyAutocloseable closeable = MyAutocloseable.create()) {
            closeable.exampleMethod();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void catchAndThrowThrowableFormat1() throws Throwable {
        try
                (
                        MyAutocloseable closeable
                                =
                                MyAutocloseable.create()
                ) {
            closeable.exampleMethod();
        } catch (
                IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (
                Throwable e) {
            e.printStackTrace();

            throw e;
        }
    }

    public void mixedWithOtherCode() throws Exception {
        try {
            try (MyAutocloseable closeable = MyAutocloseable.create()) {
                closeable.exampleMethod();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}
