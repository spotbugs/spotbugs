import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractCreateSuperCallResolutionExample {
    @AfterAll
    protected void finalize() throws Throwable {
        this.toString();
    }

    @BeforeEach
    protected void setUp() throws Exception {
        this.toString();
    }

    @AfterEach
    protected void tearDown() throws Exception {
        this.toString();
    }
}
