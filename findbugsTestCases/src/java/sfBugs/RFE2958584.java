package sfBugs;

import java.io.Serializable;
import java.util.List;

public class RFE2958584
    implements Serializable
{
  private static final long serialVersionUID = 1L;
  private List<NotSerializable> list; // Not findbugs warning here.
  private NotSerializable ns;// Findbugs gives a proper warning here.

  static class NotSerializable
  {

  }
}

