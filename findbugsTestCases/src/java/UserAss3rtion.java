public class UserAss3rtion {
    public static void makeSureItIsOK(boolean cond) {
        if (!cond)
            throw new IllegalStateException();
    }

    public void f(Object o) {
        UserAss3rtion.makeSureItIsOK(o != null);
        System.out.println(o.hashCode());
    }
}
