import java.io.FileInputStream;

class OverbroadCatch {
    public void foo(String f) {
        try {
            FileInputStream in = new FileInputStream(f);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
