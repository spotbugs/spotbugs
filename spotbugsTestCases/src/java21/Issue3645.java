import java.io.IOException;
import java.util.List;

public class Issue3645 {
	public void completePhaseActions(List<ProgressSource> sourcesToProgress) throws IOException  {
        boolean progressing = true;
        while (progressing) {
            // We reset progressing to false.
            progressing = false;
            final var currentSource = sourcesToProgress.iterator();
            while (currentSource.hasNext()) {
                final var nextSourceCtx = currentSource.next();
                try {
                    final var sourceProgress = nextSourceCtx.tryToCompletePhase();
                    switch (sourceProgress) {
                        case null -> throw new NullPointerException();
                        case FINISHED -> {
                            currentSource.remove();
                            // we were able to make progress in computation
                            progressing = true;
                        }
                        case PROGRESS -> progressing = true;
                        case NO_PROGRESS -> {
                            // Noop
                        }
                    }
                } catch (RuntimeException e) {
                    throw new IOException("Process failed", e);
                }
            }
        }
	}
	
	private interface ProgressSource {
		SourceProgress tryToCompletePhase();
	}
	
	private enum SourceProgress {
		FINISHED,
		PROGRESS,
		NO_PROGRESS
	}
}
