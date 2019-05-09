import java.util.Date;

public class A {
    private Date creationDate;

    public Date getCreationDate() {
        // reports EI_EXPOSE_REP from MALICIOUS_CODE
        // Since 1.3.8 this category is not enabled by default, so bug should be
        // ignored
        return this.creationDate;
    }

    public void setCreationDate(Date creationDate) {
        // reports EI_EXPOSE_REP2 from MALICIOUS_CODE
        // Since 1.3.8 this category is not enabled by default, so bug should be
        // ignored
        this.creationDate = creationDate;
    }
}
