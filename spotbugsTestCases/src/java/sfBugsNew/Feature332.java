package sfBugsNew;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.umd.cs.findbugs.annotations.DesireWarning;
import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Feature332 {
    static class Point {
        int x, y;
        
        Point(int... p) {
            x = p[0];
            y = p[1];
        }
        
        Point() {}
        
        void setX(int x) {
            this.x = x;
        }
    }
    
    static class Circle {
        Point center;
        int radius;
    }
    
    static class Point2 extends Point {
        @NoWarning("UC")
        Point2() {
            super(2, 3);
        }
    }

    @ExpectWarning("UC_USELESS_OBJECT")
    public void testUselessValue() {
        Point p = new Point();
        p.x = 10;
        System.out.println("test");
    }
    
    @ExpectWarning("UC_USELESS_OBJECT")
    public void testUselessList() {
        List<String> list = new ArrayList<>();
        list.add("str");
        list.add(list.get(0));
        System.out.println("test");
    }
    
    // Currently unsupported: maybe later
    @DesireWarning("UC_USELESS_OBJECT")
    public void testUselessInteraction() {
        Point p = new Point();
        Point p2 = new Point();
        p.x = 10;
        p2.x = p.x;
        p2.y = 20;
        p.y = p2.y;
        System.out.println("test");
    }
    
    @ExpectWarning("UC_USELESS_OBJECT")
    public void testUselessMultiArray() {
        int[][] x = new int[2][3];
        x[1][1] = 1;
        x[0][2] = 2;
        System.out.println("test");
    }
    
    @ExpectWarning("UC_USELESS_OBJECT")
    public void testUselessMultiArray2(int[] arr) {
        int[][] x = new int[2][];
        x[0] = arr;
        x[1] = x[0];
        System.out.println("test");
    }
    
    @ExpectWarning("UC_USELESS_OBJECT")
    public void testUselessMultiArray3() {
        int[][] x = new int[2][];
        x[0] = new int[10];
        x[0][1] = 1;
        System.out.println("test");
    }
    
    @ExpectWarning("UC_USELESS_OBJECT")
    public void testUselessMultiArray4() {
        int[][] x = new int[][] {{1,2,3},{4,5,6}};
        x[0][1] = 1;
        System.out.println("test");
    }
    
    @NoWarning("UC")
    public void testUsefulMultiArray(int[] arr) {
        int[][] x = new int[2][];
        x[0] = arr;
        x[0][1] = 1;
        System.out.println("test");
    }
    
    @ExpectWarning("UC_USELESS_OBJECT")
    public void testUselessObjectsArray() {
        Point[] pts = new Point[] {new Point(), new Point(1,2), new Point(2,3)};
        pts[0].x = 2;
        System.out.println("test");
    }
    
    @NoWarning("UC")
    public void testUsefulObjectsArray(Point p) {
        Point[] pts = new Point[] {p, new Point(1,2), new Point(2,3)};
        pts[0].x = 2;
        System.out.println("test");
    }
    
    @ExpectWarning("UC_USELESS_OBJECT")
    public void testUselessReuse() {
        Point p = new Point();
        p.x = 10;
        p.y = p.x+1;
        System.out.println("test");
    }
    
    @ExpectWarning("UC_USELESS_OBJECT")
    public void testUselessArray() {
        int[] x = new int[3];
        x[0] = 2;
        x[1] = 3;
        x[2] = x[0]+x[1];
        System.out.println("test");
    }
    
    @SuppressWarnings("unchecked")
    @ExpectWarning("UC_USELESS_OBJECT")
    public void testUselessArrayOfLists() {
        @SuppressWarnings("rawtypes")
        List[] lists = new List[] {new ArrayList<>()};
        lists[0].add("test");
        System.out.println("test");
    }
    
    @SuppressWarnings("unchecked")
    @NoWarning("UC")
    public void testUsefulArrayOfLists(List<?> l) {
        @SuppressWarnings("rawtypes")
        List[] lists = new List[] {l};
        lists[0].add("test");
        System.out.println("test");
    }

    // Currently unsupported: maybe later
    @DesireWarning("UC_USELESS_OBJECT")
    public void testUselessCircle() {
        Circle c = new Circle();
        c.center = new Point();
        c.radius = 5;
        c.center.x = c.radius;
        c.center.y = 2;
        System.out.println("test");
    }
    
    @ExpectWarning("UC_USELESS_OBJECT_STACK")
    public void testNonAssigned() {
        new Point().x = 2;
        System.out.println("test");
    }
    
    @ExpectWarning("UC_USELESS_OBJECT_STACK")
    public void testNonAssignedCall() {
        new Point().setX(2);
        System.out.println("test");
    }
    
    @ExpectWarning("UC_USELESS_OBJECT_STACK")
    public void testNonAssignedCall2() {
        new ArrayList<String>().add("test");
        System.out.println("test");
    }
    
    @ExpectWarning("UC_USELESS_OBJECT_STACK")
    public void testNonAssignedArray() {
        (new double[1])[0] = 1;
        System.out.println("test");
    }
    
    // False-positive appeared before we handle IINC properly
    @NoWarning("UC")
    public void testIncrement() {
        int[] p = new int[1];
        p[0] = 5;
        int i = p[0];
        i++;
        System.out.println(i);
    }
    
    // False-positive appeared before we handle loops properly
    @NoWarning("UC")
    public void sizeUsed() {
        List<String> test = new ArrayList<>();
        if(Math.random() > 0.5) {
            test.add("qq");
        }
        for(int p = test.size(); p > 0; p--) {
            System.out.println("qq");
        }
    }
    
    @NoWarning("UC")
    public String testStringBuilder(Object obj) {
        return obj == null ? null : "test" + obj;
    }
    
    @NoWarning("UC")
    public void testVarArgs(String f) {
        String.format(f, 0);
    }

    // False-positive appeared for unconditional throwers: now handled correctly
    @NoWarning("UC")
    public void testUseThrower() throws Exception {
        raiseException("test", "best", "quest");
    }
    
    public void raiseException(String... message) throws Exception {
        throw new Exception(Arrays.asList(message).toString());
    }

}
