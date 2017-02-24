package sfBugsNew;

import org.junit.Assert;


public abstract class Bug1363 {

    static class GitModelObject {}
    static class GitModelObjectContainer extends GitModelObject{};
    
    static class GitModelBlob extends GitModelObject {};
    static class GitModelCommit extends GitModelObjectContainer {};
  
    public abstract GitModelBlob createGitModelBlob();
    
    
    public void shouldBeSymmetric1() throws Exception {
        // given
        GitModelBlob left = new GitModelBlob(); createGitModelBlob();
        GitModelCommit right = new GitModelCommit();

        // when
        boolean actual1 = left.equals(right);
        boolean actual2 = right.equals(left);

        // then
        Assert.assertTrue(!actual1);
        Assert.assertTrue(!actual2);
    }
}
