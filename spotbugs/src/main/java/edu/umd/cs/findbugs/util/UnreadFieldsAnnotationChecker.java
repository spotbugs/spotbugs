package edu.umd.cs.findbugs.util;

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;

/**
 * Utility class checking for presence of certain annotations. If one of the listed annotations is associated with
 * a given field, then the "URF_UNREAD_FIELD" detector should be skipped for the field.
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/574">GitHub issue</a>
 */
public final class UnreadFieldsAnnotationChecker {

    /**
     * Private constructor to prevent instantiation given utility class.
     */
    private UnreadFieldsAnnotationChecker() {
        throw new UnsupportedOperationException();
    }

    /**
     * Array of annotations which, if present, signal that "URF_UNREAD_FIELD" detector should be skipped.
     */
    private static final Class[] IGNORE_RULE_FOR_THESE_ANNOTATIONS = {
        com.google.gson.annotations.SerializedName.class,
        javax.xml.bind.annotation.XmlElement.class,
        javax.xml.bind.annotation.XmlAttribute.class,
        javax.xml.bind.annotation.XmlValue.class,
        org.junit.jupiter.api.extension.RegisterExtension.class
    };

    private static final Set<ClassDescriptor> IGNORE_RULE_FOR_THESE_CLASS_DESCRIPTORS =
            annotationToClassDescriptorBuilder(IGNORE_RULE_FOR_THESE_ANNOTATIONS);

    /**
     * Checks whether the collection of annotations associated with a given element include annotations that indicate
     * the "URF_UNREAD_FIELD" detector should be skipped.
     * @param annotationsToCheck Collections of annotations associated with given element.
     * @return If true, "URF_UNREAD_FIELD" detector should be ignored for given field.
     * @see <a href="https://github.com/spotbugs/spotbugs/issues/574">GitHub issue</a>
     */
    public static boolean containsSpecialAnnotation(Collection<AnnotationValue> annotationsToCheck) {
        for (AnnotationValue annotationValue : annotationsToCheck) {
            if (IGNORE_RULE_FOR_THESE_CLASS_DESCRIPTORS.contains(annotationValue.getAnnotationClass())) {
                return true;
            }
        }
        return false;
    }

    private static Set<ClassDescriptor> annotationToClassDescriptorBuilder(Class[] annotationsToCheck) {
        Set<ClassDescriptor> tempSet = new HashSet<ClassDescriptor>();
        for (Class annotation : annotationsToCheck) {
            tempSet.add(DescriptorFactory.createClassDescriptor(annotation));
        }
        return tempSet;
    }

}
