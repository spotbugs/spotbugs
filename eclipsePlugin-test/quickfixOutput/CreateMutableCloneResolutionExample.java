import java.util.Date;

public class CreateMutableCloneResolutionExample {
    Date myDate = new Date();

    public Date getMyDate() {
        return myDate == null ? null : (Date) myDate.clone();
    }
}
