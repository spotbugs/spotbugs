package bugIdeas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NoWarning;

public abstract class Ideas_2009_05_20 {
    @NoWarning("NP_")
    public static void falsePositive_ifnonull(List lst) {
        final ArrayList sections = new ArrayList();
        for (Object o : lst) {
            if (sections != null)
                sections.add(o);
        }
        sections.clear();
    }

    @NoWarning("NP_")
    public static void falsePositive_ifnonnull(Object o) {
        final ArrayList sections = new ArrayList();
        if (sections != null)
            sections.add(o);

        sections.clear();
    }

    @NoWarning("NP_")
    public static void falsePositive(List lst) {
        final ArrayList sections = new ArrayList();
        for (Object o : lst) {
            if (null != sections)
                sections.add(o);
        }
        sections.clear();
    }

    @NoWarning("NP_")
    public static void falsePositive(Object o) {
        final ArrayList sections = new ArrayList();
        if (null != sections)
            sections.add(o);

        sections.clear();
    }

    /**
     * From Ant 1.5.2,
     * org.apache.tools.ant.taskdefs.optional.extension.Specification
     * 
     * Reported as a FindBugs false positive in "Accurate Interprocedural
     * Null-Dereference Analysis for Java", Mangala Gowri Nanda and Saurabh
     * Sinha, ICSE 2009
     */
    @NoWarning("NP_")
    static ArrayList<Ideas_2009_05_20> removeDuplicates_FalsePositive(final ArrayList<Ideas_2009_05_20> list) {
        final ArrayList<Ideas_2009_05_20> results = new ArrayList<Ideas_2009_05_20>();
        final ArrayList<String> sections = new ArrayList<String>();
        while (list.size() > 0) {
            final Ideas_2009_05_20 specification = list.remove(0);
            final Iterator<Ideas_2009_05_20> iterator = list.iterator();
            while (iterator.hasNext()) {
                final Ideas_2009_05_20 other = iterator.next();
                if (specification.equals(other)) {
                    final String[] otherSections = other.getSections();
                    if (null != sections) {
                        sections.addAll(Arrays.asList(otherSections));
                    }
                    iterator.remove();
                }
            }

            final Ideas_2009_05_20 merged = mergeInSections(specification, sections);
            results.add(merged);
            // Reset list of sections
            sections.clear();
        }

        return results;
    }

    private static Ideas_2009_05_20 mergeInSections(Ideas_2009_05_20 specification, ArrayList<String> sections) {
        return specification;
    }

    private String[] getSections() {
        return new String[] { "a" };
    }

}
