package ghIssues.issue3324;

import javax.annotation.Nullable;

public class DummyImpl extends DummyAbs implements DummyInterface
{
    @Override
    public @Nullable String getNameDefaultNonnull()
    {
        return null;
    }

    @Override
    public @Nullable String getNameExplicitNonnullFromInterface()
    {
        return null;
    }

    @Override
    public @Nullable String getNameDefaultNonnullFromAbs()
    {
        return null;
    }
}
