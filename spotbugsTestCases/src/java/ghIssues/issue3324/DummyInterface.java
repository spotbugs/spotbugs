package ghIssues.issue3324;

import javax.annotation.Nonnull;

public interface DummyInterface
{
    String getNameDefaultNonnull();

    @Nonnull
    String getNameExplicitNonnullFromInterface();
}
