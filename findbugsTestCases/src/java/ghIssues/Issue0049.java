package ghIssues;

import java.io.Serializable;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Issue0049 {
    public interface TestInterface {}

    @NoWarning("RI_REDUNDANT_INTERFACES")
    public static class SuperClass implements TestInterface {}

    @ExpectWarning("RI_REDUNDANT_INTERFACES")
    public static class SubClassWithDuplicate extends SuperClass implements TestInterface {}

    @NoWarning("RI_REDUNDANT_INTERFACES")
    public static class SubClassWithoutDuplicate extends SuperClass {}

    @ExpectWarning("RI_REDUNDANT_INTERFACES")
    public static class SubSubClassWithDuplicate extends SubClassWithoutDuplicate implements TestInterface {}

    @NoWarning("RI_REDUNDANT_INTERFACES")
    public static class SerializableSuperClass implements Serializable {}

    @NoWarning("RI_REDUNDANT_INTERFACES")
    public static class SerializableSubClassWithDuplicate extends SerializableSuperClass implements Serializable {}

    @NoWarning("RI_REDUNDANT_INTERFACES")
    public static class SerializableSubClassWithoutDuplicate extends SerializableSuperClass {}

    @NoWarning("RI_REDUNDANT_INTERFACES")
    public static class SerializableSubSubClassDuplicate extends SerializableSubClassWithoutDuplicate implements Serializable {}
}
