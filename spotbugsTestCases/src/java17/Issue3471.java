import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class Issue3471 {
	
	public record Issue3471Suppressed(String name, @SuppressFBWarnings({"EI_EXPOSE_REP"}) List<String> values) {
	}

	public record Issue3471ExposeRep(String name, List<String> values) {
	}
}
