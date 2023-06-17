package publicIdentifiers;

public class ShadowedPublicIdentifiersOverriddenMethods extends ShadowedPublicIdentifiersOverridableMethods {
    @Override
    protected void ArrayList(int param1) {
        System.out.println(param1);
    }
}
