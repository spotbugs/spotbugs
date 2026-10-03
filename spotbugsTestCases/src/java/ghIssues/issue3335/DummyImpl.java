package ghIssues.issue3335;

import javax.annotation.Nonnull;

public class DummyImpl implements DummyInterface
{

    @Override
    public void setNullable1(Object objRef)
    {
    }

    @Override
    public void setNullable2(@Nonnull Object objRef)
    {
    }
}
