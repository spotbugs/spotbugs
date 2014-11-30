package sfBugsNew;

import java.awt.Point;
import edu.umd.cs.findbugs.annotations.DesireNoWarning;

public class Bug1333 {
  @DesireNoWarning("SA_FIELD_DOUBLE_ASSIGNMENT")
  public void resetPointsX(Point[] p) {
    assert p.length == 2;
    p[0].x = 0;
    p[1].x = 0;
  }
}