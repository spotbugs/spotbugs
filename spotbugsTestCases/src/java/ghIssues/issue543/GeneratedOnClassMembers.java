package ghIssues.issue543;

import java.io.File;
import org.example.Generated;

@SuppressWarnings("all")
public class GeneratedOnClassMembers {
  /** URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD */
  @Generated
  public int a = 1;

  /** NP_TOSTRING_COULD_RETURN_NULL */
  @Generated
  public String toString() {
    return null;
  }

  /** DMI_HARDCODED_ABSOLUTE_FILENAME */
  public boolean test() {
    File file = new File("/home/test/file");
    return file.canExecute();
  }

  /** ES_COMPARING_PARAMETER_STRING_WITH_EQ */
  @Generated
  public boolean test(String s) {
    return s == "test";
  }
}
