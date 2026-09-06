package edu.umd.cs.findbugs.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

class ClassFeatureSetTest {

    @Test
    void classNameAndPackageTransformationsRespectJavaPackages() {
        assertEquals("java.util.List", ClassFeatureSet.transformClassName("java.util.List"));
        assertEquals("MyType", ClassFeatureSet.transformClassName("com.example.MyType"));
        assertTrue(ClassFeatureSet.isUnlikelyToBeRenamed("java.lang"));
        assertFalse(ClassFeatureSet.isUnlikelyToBeRenamed("com.example"));
    }

    @Test
    void signatureTransformationsNormalizeReferenceTypes() {
        assertEquals("Ljava/lang/String;", ClassFeatureSet.transformSignature("Ljava/lang/String;"));
        assertEquals("LString;", ClassFeatureSet.transformSignature("Lcom/example/String;"));
        assertEquals("[[LString;", ClassFeatureSet.transformSignature("[[Lcom/example/String;"));
        assertEquals("(LString;I[[LObject;)", ClassFeatureSet.transformMethodSignature("(Lcom/example/String;I[[Lorg/acme/Object;)V"));
    }

    @Test
    void similarityHandlesInterfaceAndFeatureThresholdRules() {
        ClassFeatureSet a = new ClassFeatureSet();
        a.setClassName("com.example.A");
        a.setInterface(false);
        a.addFeature("f1");

        ClassFeatureSet b = new ClassFeatureSet();
        b.setClassName("com.example.A");
        b.setInterface(false);
        b.addFeature("f2");

        assertEquals(ClassFeatureSet.EXACT_CLASS_NAME_MATCH, ClassFeatureSet.similarity(a, b));

        b.setInterface(true);
        assertEquals(0.0, ClassFeatureSet.similarity(a, b));
        b.setInterface(false);
        b.setClassName("com.example.B");
        assertEquals(0.0, ClassFeatureSet.similarity(a, b));
    }

    @Test
    void similarityUsesFeatureOverlapForLargerFeatureSets() {
        ClassFeatureSet a = new ClassFeatureSet();
        a.setClassName("A");
        a.setInterface(false);
        for (int i = 0; i < 6; i++) {
            a.addFeature("f" + i);
        }

        ClassFeatureSet b = new ClassFeatureSet();
        b.setClassName("B");
        b.setInterface(false);
        b.addFeature("f0");
        b.addFeature("f1");
        b.addFeature("f2");
        b.addFeature("x3");
        b.addFeature("x4");
        b.addFeature("x5");

        assertEquals(0.5, ClassFeatureSet.similarity(a, b));
        assertFalse(a.similarTo(b));

        b.addFeature("f3");
        b.addFeature("f4");
        assertTrue(a.similarTo(b));
    }

    @Test
    void writeXmlEmitsClassAndFeatures() throws Exception {
        ClassFeatureSet set = new ClassFeatureSet();
        set.setClassName("com.example.Example");
        set.addFeature("Method:run:()V");
        set.addFeature("Field:value:I");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        XMLOutput xmlOutput = new OutputStreamXMLOutput(output);

        set.writeXML(xmlOutput);
        xmlOutput.finish();

        String xml = output.toString(StandardCharsets.UTF_8);
        assertTrue(xml.contains("<ClassFeatureSet class=\"com.example.Example\">"));
        assertTrue(xml.contains("<Feature value=\"Method:run:()V\"/>"));
        assertTrue(xml.contains("<Feature value=\"Field:value:I\"/>"));
        assertTrue(xml.contains("</ClassFeatureSet>"));
    }

    @Test
    void initializeCapturesClassMethodFieldAndCodeLengthFeatures() throws Exception {
        JavaClass javaClass;
        String resourceName = Child.class.getName().replace('.', '/') + ".class";
        try (InputStream stream = Child.class.getClassLoader().getResourceAsStream(resourceName)) {
            javaClass = new ClassParser(stream, resourceName).parse();
        }
        ClassFeatureSet set = new ClassFeatureSet().initialize(javaClass);

        assertEquals(Child.class.getName(), set.getClassName());
        assertFalse(set.isInterface());
        assertTrue(containsPrefix(set, ClassFeatureSet.CLASS_NAME_KEY + "ClassFeatureSetTest$Child"));
        assertTrue(containsPrefix(set, ClassFeatureSet.METHOD_NAME_KEY + "helper:()"));
        assertTrue(containsPrefix(set, ClassFeatureSet.FIELD_NAME_KEY + "field:I"));
        assertTrue(containsPrefix(set, ClassFeatureSet.CODE_LENGTH_KEY));
        assertFalse(containsPrefix(set, ClassFeatureSet.METHOD_NAME_KEY + "overridden:()"));
    }

    private static boolean containsPrefix(ClassFeatureSet set, String prefix) {
        List<String> features = new ArrayList<>();
        set.featureIterator().forEachRemaining(features::add);
        return features.stream().anyMatch(feature -> feature.startsWith(prefix));
    }

    static class Parent {
        void overridden() {
        }
    }

    interface ChildContract {
        void contract();
    }

    static class Child extends Parent implements ChildContract {
        int field;

        @Override
        void overridden() {
        }

        @Override
        public void contract() {
        }

        static void helper() {
            int total = 0;
            for (int i = 0; i < 4; i++) {
                total += i;
            }
            if (total == 6) {
                total++;
            }
        }
    }
}
