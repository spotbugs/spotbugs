class NoSuchFieldErrorTest {
    enum MyEnum {
        ARG1, ARG2
    }

    final MyEnum val;

    public NoSuchFieldErrorTest(MyEnum val) {
        this.val = val;
        try {
            switch (this.val) {
            case ARG1:
                System.out.println("arg1");
                break;
            case ARG2:
            default:
                System.out.println("arg2 or default");
                break;
            }
        } catch (NoSuchFieldError e) {
            e.printStackTrace();
        }
    }
}
