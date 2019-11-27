import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// The use of `assert` or LoggerFactory.getLogger(Foo.class) causes an unreported unwritten field.
public class UnreportedUnwrittenField {
  // This line causes UWF_UNWRITTEN_FIELD to go unreported.
  private static final Logger LOG = LoggerFactory.getLogger(UnreportedUnwrittenField.class);
  // Although, this version does not:
  //private static final Logger LOG = LoggerFactory.getLogger("UnreportedUnwrittenField");
  // This version doesn't either:
  //private final Logger LOG = LoggerFactory.getLogger(getClass());

  private String unwrittenField;
  private String data = "foo";

  public UnreportedUnwrittenField() {
    LOG.debug("log foo");
  }

  public String getUnwrittenField() {
    return unwrittenField;
  }

  public String getData() {
    // This line also causes UWF_UNWRITTEN_FIELD to go unreported.
    assert data != null;
    return data;
  }
}