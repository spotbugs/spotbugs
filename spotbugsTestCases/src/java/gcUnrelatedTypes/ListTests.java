package gcUnrelatedTypes;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ListTests {

    public void test1NoBugs(List<String> list) {
        list.contains("Hello");
        list.remove("Hello");
        list.indexOf("Hello");
        list.lastIndexOf("Hello");
    }

    public void test1Bugs(List<String> list) {
        list.contains(new StringBuffer("Key"));
        list.remove(new StringBuffer("Key"));
        list.indexOf(new StringBuffer("Key"));
        list.lastIndexOf(new StringBuffer("Key"));
    }

    public void test2NoBugs(LinkedList<CharSequence> list) {
        list.indexOf(new StringBuffer("Key"));
    }

    public void test2Bugs(LinkedList<CharSequence> list) {
        list.indexOf(Integer.valueOf(3));
    }

    public void test3NoBugs(ArrayList<? extends CharSequence> list) {
        list.lastIndexOf(new StringBuffer("Key"));
    }

    public void test3Bugs(ArrayList<? extends CharSequence> list) {
        list.lastIndexOf(Integer.valueOf(3));
    }

    public void test4NoBugs(LinkedList<? super CharSequence> list) {
        list.lastIndexOf(new StringBuffer("Key"));
    }

    public void test4Bugs(LinkedList<? super CharSequence> list) {
        list.lastIndexOf(Integer.valueOf(3));
    }

}
