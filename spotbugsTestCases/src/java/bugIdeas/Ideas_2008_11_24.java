package bugIdeas;

import org.easymock.EasyMock;

public class Ideas_2008_11_24 {
    interface Foo {
        void setFoo(int x);
    }

    public static void main(String args[]) {
        Foo foo = EasyMock.createMock(Foo.class);
        foo.setFoo(5);
        EasyMock.replay();
        foo.setFoo(4);
        EasyMock.verify();
    }

}
