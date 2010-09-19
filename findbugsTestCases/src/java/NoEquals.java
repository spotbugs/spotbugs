public final class NoEquals {
    public void test(NoEquals ne) {
        if (ne.equals(this))
            System.out.println("Yup it's equal");
    }
}
