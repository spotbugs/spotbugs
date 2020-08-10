package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.xml.XMLOutput;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class VersionInsensitiveBugComparatorTest {
    static class MyBugAnnotation implements BugAnnotation {
        private String desc;

        public MyBugAnnotation(String desc) {
            this.desc = desc;
        }

        @Override
        public Object clone() {
            return null;
        }

        @Override
        public void accept(BugAnnotationVisitor visitor) {
            ;
        }

        @Override
        public String format(String key, ClassAnnotation primaryClass) {
            return key + primaryClass.toString();
        }

        @Override
        public String getDescription() {
            return desc;
        }

        @Override
        public void setDescription(String description) {
            desc = description;
        }

        @Override
        public boolean isSignificant() {
            return true;
        }

        @Override
        public String toString(ClassAnnotation primaryClass) {
            return desc;
        }

        @Override
        public void writeXML(XMLOutput xmlOutput, boolean addMessages, boolean isPrimary) throws IOException {

        }

        @Override
        public void writeXML(XMLOutput xmlOutput) throws IOException {

        }

        public String toString() {
            return desc;
        }

        @Override
        public int compareTo(BugAnnotation o) {
            return this.toString().compareTo(o.toString());
        }
    }

    @Test
    public void compare1() {
        BugInstance lbi = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        BugInstance rbi = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);

        List<BugAnnotation> llist = new LinkedList<>();
        llist.add(new StringAnnotation("ABC"));
        llist.add(new SourceLineAnnotation("A", "B", 1, 2, 3, 4));
        llist.add(new MyBugAnnotation("ABC"));
        lbi.addAnnotations(llist);

        List<BugAnnotation> rlist = new LinkedList<>();
        rlist.add(new StringAnnotation("ABC"));
        rlist.add(new SourceLineAnnotation("A", "B", 1, 2, 3, 4));
        rlist.add(new MyBugAnnotation("DEF"));
        rbi.addAnnotations(rlist);

        VersionInsensitiveBugComparator cmp = new VersionInsensitiveBugComparator();
        Assert.assertEquals(cmp.compare(lbi, rbi), -cmp.compare(rbi, lbi));
    }

    @Test
    public void compare2() {
        BugInstance bi1 = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        BugInstance bi2 = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        BugInstance bi3 = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);

        List<BugAnnotation> list1 = new LinkedList<>();
        list1.add(new StringAnnotation("ABC"));
        list1.add(new MyBugAnnotation("ABC"));
        list1.add(new SourceLineAnnotation("A", "B", 1, 2, 3, 4));
        bi1.addAnnotations(list1);

        List<BugAnnotation> list2 = new LinkedList<>();
        list2.add(new MyBugAnnotation("DEF"));
        list2.add(new StringAnnotation("ABC"));
        list2.add(new SourceLineAnnotation("A", "B", 1, 2, 3, 4));
        bi2.addAnnotations(list2);

        List<BugAnnotation> list3 = new LinkedList<>();
        list3.add(new MyBugAnnotation("GHI"));
        bi3.addAnnotations(list3);

        VersionInsensitiveBugComparator cmp = new VersionInsensitiveBugComparator();
        // Because
        Assert.assertTrue(cmp.compare(bi3, bi2) > 0);
        Assert.assertTrue(cmp.compare(bi2, bi1) > 0);

        // So
        Assert.assertTrue(cmp.compare(bi3, bi1) > 0);
    }

    @Test
    public void compare3() {
        BugInstance bi1 = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        BugInstance bi2 = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);
        BugInstance bi3 = new BugInstance("NP_NULL_ON_SOME_PATH", Priorities.NORMAL_PRIORITY);

        List<BugAnnotation> list1 = new LinkedList<>();
        list1.add(new MyBugAnnotation("ABC"));
        list1.add(new StringAnnotation("ABC"));
        list1.add(new SourceLineAnnotation("A", "B", 1, 2, 3, 4));
        bi1.addAnnotations(list1);

        List<BugAnnotation> list2 = new LinkedList<>();
        list2.add(new MyBugAnnotation("ABC"));
        list2.add(new StringAnnotation("ABC"));
        list2.add(new SourceLineAnnotation("A", "B", 1, 2, 3, 4));
        bi2.addAnnotations(list2);

        List<BugAnnotation> list3 = new LinkedList<>();
        list3.add(new MyBugAnnotation("GHI"));
        bi3.addAnnotations(list3);

        VersionInsensitiveBugComparator cmp = new VersionInsensitiveBugComparator();

        // because
        Assert.assertTrue(cmp.compare(bi1, bi2) == 0);
        Assert.assertTrue(cmp.compare(bi1, bi3) < 0);
        // so
        Assert.assertTrue(cmp.compare(bi2, bi3) < 0);
    }
}
