package edu.umd.cs.findbugs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.impl.ClassFactory;
import edu.umd.cs.findbugs.classfile.impl.ClassPathImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SAXBugCollectionHandlerTest {

    @Before
    public void setUp() {
        IAnalysisCache analysisCache = ClassFactory.instance().createAnalysisCache(new ClassPathImpl(), new PrintingBugReporter());;
        Global.setAnalysisCacheForCurrentThread(analysisCache);
        FindBugs2.registerBuiltInAnalysisEngines(analysisCache);
    }

    @After
    public void teardown() {
        Global.removeAnalysisCacheForCurrentThread();
    }

    @Test
    public void testBugInstanceXmlPropsNoReviews() throws Exception {
        SortedBugCollection bc = new SortedBugCollection();
        bc.readXML(new StringReader(
                "<BugCollection version='1.3.10-dev-20100728' sequence='0' timestamp='1280333223462' analysisTimestamp='1280333224881' release=''>"
                        + "  <BugInstance type='MS_MUTABLE_ARRAY' priority='1' abbrev='MS' category='MALICIOUS_CODE' instanceHash='1acc5c5b9b7ab9efacede805afe1e53a' instanceOccurrenceNum='0' instanceOccurrenceMax='0' rank='16' ageInDays='108' firstSeen='4/11/10 11:24 AM'>"
                        + "    <ShortMessage>Field is a mutable array</ShortMessage>"
                        + "    <LongMessage>org.apache.bcel.Const.ACCESS_NAMES is a mutable array</LongMessage>"
                        + "    <Class classname='org.apache.bcel.Constants' primary='true'>"
                        + "      <SourceLine classname='org.apache.bcel.Constants' start='210' end='1443' sourcefile='Const.java' sourcepath='org/apache/bcel/Const.java'>"
                        + "        <Message>At Const.java:[lines 210-1443]</Message>"
                        + "      </SourceLine>"
                        + "      <Message>In class org.apache.bcel.Constants</Message>"
                        + "    </Class>"
                        + "    <Field classname='org.apache.bcel.Constants' name='ACCESS_NAMES' signature='[Ljava/lang/String;' isStatic='true' primary='true'>"
                        + "      <SourceLine classname='org.apache.bcel.Constants' sourcefile='Const.java' sourcepath='org/apache/bcel/Const.java'>"
                        + "        <Message>In Const.java</Message>"
                        + "      </SourceLine>"
                        + "      <Message>Field org.apache.bcel.Const.ACCESS_NAMES</Message>"
                        + "    </Field>"
                        + "    <SourceLine classname='org.apache.bcel.Constants' primary='true' start='210' end='210' startBytecode='89' endBytecode='89' sourcefile='Const.java' sourcepath='org/apache/bcel/Const.java'>"
                        + "      <Message>At Const.java:[line 210]</Message>"
                        + "    </SourceLine>"
                        + "  </BugInstance>"
                        + "</BugCollection>"));
        assertEquals(1, bc.getCollection().size());
        assertEquals("edu.umd.cs.findbugs.plugins.core", DetectorFactoryCollection.instance().getCorePlugin().getPluginId());
        BugInstance bug = bc.getCollection().iterator().next();
        assertEquals("MS_MUTABLE_ARRAY", bug.getBugPattern().getType());
        assertEquals("1acc5c5b9b7ab9efacede805afe1e53a", bug.getInstanceHash());
        assertEquals(16, bug.getBugRank());
    }

    @Test
    public void testBugInstanceXmlPropsWithReviews() throws Exception {
        SortedBugCollection bc = new SortedBugCollection();
        bc.readXML(new StringReader(
                "<BugCollection version='1.3.10-dev-20100728' sequence='0' timestamp='1280333223462' analysisTimestamp='1280333224881' release=''>"
                        + "  <BugInstance type='MS_MUTABLE_ARRAY' priority='1' abbrev='MS' category='MALICIOUS_CODE' instanceHash='1acc5c5b9b7ab9efacede805afe1e53a' instanceOccurrenceNum='0' instanceOccurrenceMax='0' rank='16' ageInDays='108' firstSeen='4/11/10 11:24 AM' reviews='4' consensus='NOT_A_BUG' notAProblem='true'>"
                        + "    <ShortMessage>Field is a mutable array</ShortMessage>"
                        + "    <LongMessage>org.apache.bcel.Const.ACCESS_NAMES is a mutable array</LongMessage>"
                        + "    <Class classname='org.apache.bcel.Constants' primary='true'>"
                        + "      <SourceLine classname='org.apache.bcel.Constants' start='210' end='1443' sourcefile='Const.java' sourcepath='org/apache/bcel/Const.java'>"
                        + "        <Message>At Const.java:[lines 210-1443]</Message>"
                        + "      </SourceLine>"
                        + "      <Message>In class org.apache.bcel.Constants</Message>"
                        + "    </Class>"
                        + "    <Field classname='org.apache.bcel.Constants' name='ACCESS_NAMES' signature='[Ljava/lang/String;' isStatic='true' primary='true'>"
                        + "      <SourceLine classname='org.apache.bcel.Constants' sourcefile='Const.java' sourcepath='org/apache/bcel/Const.java'>"
                        + "        <Message>In Const.java</Message>"
                        + "      </SourceLine>"
                        + "      <Message>Field org.apache.bcel.Const.ACCESS_NAMES</Message>"
                        + "    </Field>"
                        + "    <SourceLine classname='org.apache.bcel.Constants' primary='true' start='210' end='210' startBytecode='89' endBytecode='89' sourcefile='Const.java' sourcepath='org/apache/bcel/Const.java'>"
                        + "      <Message>At Const.java:[line 210]</Message>"
                        + "    </SourceLine>"
                        + "  </BugInstance>"
                        + "</BugCollection>"));
        assertEquals(1, bc.getCollection().size());
        BugInstance bug = bc.getCollection().iterator().next();
        assertEquals("MS_MUTABLE_ARRAY", bug.getBugPattern().getType());
        assertEquals("1acc5c5b9b7ab9efacede805afe1e53a", bug.getInstanceHash());
        assertEquals(16, bug.getBugRank());
    }

    @Test
    public void testBugInstanceXmlPropsWithReviewsShouldFix() throws Exception {
        SortedBugCollection bc = new SortedBugCollection();
        bc.readXML(new StringReader(
                "<BugCollection version='1.3.10-dev-20100728' sequence='0' timestamp='1280333223462' analysisTimestamp='1280333224881' release=''>"
                        + "  <BugInstance type='MS_MUTABLE_ARRAY' priority='1' abbrev='MS' category='MALICIOUS_CODE' instanceHash='1acc5c5b9b7ab9efacede805afe1e53a' instanceOccurrenceNum='0' instanceOccurrenceMax='0' rank='16' ageInDays='108' firstSeen='4/11/10 11:24 AM' reviews='4' consensus='SHOULD_FIX' shouldFix='true'>"
                        + "    <ShortMessage>Field is a mutable array</ShortMessage>"
                        + "    <LongMessage>org.apache.bcel.Const.ACCESS_NAMES is a mutable array</LongMessage>"
                        + "    <Class classname='org.apache.bcel.Constants' primary='true'>"
                        + "      <SourceLine classname='org.apache.bcel.Constants' start='210' end='1443' sourcefile='Const.java' sourcepath='org/apache/bcel/Const.java'>"
                        + "        <Message>At Const.java:[lines 210-1443]</Message>"
                        + "      </SourceLine>"
                        + "      <Message>In class org.apache.bcel.Constants</Message>"
                        + "    </Class>"
                        + "    <Field classname='org.apache.bcel.Constants' name='ACCESS_NAMES' signature='[Ljava/lang/String;' isStatic='true' primary='true'>"
                        + "      <SourceLine classname='org.apache.bcel.Constants' sourcefile='Const.java' sourcepath='org/apache/bcel/Const.java'>"
                        + "        <Message>In Const.java</Message>"
                        + "      </SourceLine>"
                        + "      <Message>Field org.apache.bcel.Const.ACCESS_NAMES</Message>"
                        + "    </Field>"
                        + "    <SourceLine classname='org.apache.bcel.Constants' primary='true' start='210' end='210' startBytecode='89' endBytecode='89' sourcefile='Const.java' sourcepath='org/apache/bcel/Const.java'>"
                        + "      <Message>At Const.java:[line 210]</Message>"
                        + "    </SourceLine>"
                        + "  </BugInstance>"
                        + "</BugCollection>"));
        assertEquals(1, bc.getCollection().size());
        BugInstance bug = bc.getCollection().iterator().next();
        assertEquals("MS_MUTABLE_ARRAY", bug.getBugPattern().getType());
        assertEquals("1acc5c5b9b7ab9efacede805afe1e53a", bug.getInstanceHash());
        assertEquals(16, bug.getBugRank());
    }

    @Test
    public void testReadAndThenStoreXmlProps() throws Exception {
        SortedBugCollection origBC = new SortedBugCollection();
        // read it in
        origBC.readXML(new StringReader(
                "<BugCollection version='1.3.10-dev-20100728' sequence='0' timestamp='1280333223462' analysisTimestamp='1280333224881' release=''>"
                        + "  <BugInstance type='MS_MUTABLE_ARRAY' priority='1' abbrev='MS' category='MALICIOUS_CODE' instanceHash='1acc5c5b9b7ab9efacede805afe1e53a' instanceOccurrenceNum='0' instanceOccurrenceMax='0' rank='16' ageInDays='108' firstSeen='4/11/10 11:24 AM' reviews='4' consensus='SHOULD_FIX' shouldFix='true'>"
                        + "    <ShortMessage>Field is a mutable array</ShortMessage>"
                        + "    <LongMessage>org.apache.bcel.Const.ACCESS_NAMES is a mutable array</LongMessage>"
                        + "    <Class classname='org.apache.bcel.Constants' primary='true'>"
                        + "      <SourceLine classname='org.apache.bcel.Constants' start='210' end='1443' sourcefile='Const.java' sourcepath='org/apache/bcel/Const.java'>"
                        + "        <Message>At Const.java:[lines 210-1443]</Message>"
                        + "      </SourceLine>"
                        + "      <Message>In class org.apache.bcel.Constants</Message>"
                        + "    </Class>"
                        + "    <Field classname='org.apache.bcel.Constants' name='ACCESS_NAMES' signature='[Ljava/lang/String;' isStatic='true' primary='true'>"
                        + "      <SourceLine classname='org.apache.bcel.Constants' sourcefile='Const.java' sourcepath='org/apache/bcel/Const.java'>"
                        + "        <Message>In Const.java</Message>"
                        + "      </SourceLine>"
                        + "      <Message>Field org.apache.bcel.Const.ACCESS_NAMES</Message>"
                        + "    </Field>"
                        + "    <SourceLine classname='org.apache.bcel.Constants' primary='true' start='210' end='210' startBytecode='89' endBytecode='89' sourcefile='Const.java' sourcepath='org/apache/bcel/Const.java'>"
                        + "      <Message>At Const.java:[line 210]</Message>"
                        + "    </SourceLine>"
                        + "  </BugInstance>"
                        + "</BugCollection>"));
        // write it out
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        origBC.writeXML(outBytes);

        // read it back in
        SortedBugCollection bc = new SortedBugCollection();
        bc.readXML(new StringReader(new String(outBytes.toByteArray(), StandardCharsets.UTF_8)));

        // check it
        assertEquals(1, bc.getCollection().size());
        BugInstance bug = bc.getCollection().iterator().next();
        assertEquals("MS_MUTABLE_ARRAY", bug.getBugPattern().getType());
        assertEquals("1acc5c5b9b7ab9efacede805afe1e53a", bug.getInstanceHash());
        assertEquals(16, bug.getBugRank());
    }

    @Test
    public void testReadAndThenStoreJasAttribute() throws Exception {
        SortedBugCollection origBC = new SortedBugCollection();
        // read it in
        origBC.readXML(new StringReader(
                "<BugCollection version='1.3.10-dev-20100728' sequence='0' timestamp='1280333223462' analysisTimestamp='1280333224881' release=''>"
                        + "<BugInstance type=\"UUF_UNUSED_FIELD\" priority=\"1\" rank=\"16\" abbrev=\"UuF\" category=\"PERFORMANCE\">\n"
                        + "  <Class classname=\"ghIssues.issue543.ImmutableFoobarValue.class\" classAnnotationNames=\"org.immutables.value.Generated\">\n"
                        + "    <SourceLine classname=\"ghIssues.issue543.ImmutableFoobarValue.class\"/>\n"
                        + "  </Class>\n"
                        + "  <Method classname=\"ghIssues.issue543.ImmutableFoobarValue.class\" name=\"foo\" signature=\"int\" isStatic=\"false\" classAnnotationNames=\"org.immutables.value.Generated\"/>\n"
                        + "  <Field classname=\"ghIssues.issue543.ImmutableFoobarValue.class\" name=\"foo\" signature=\"int\" isStatic=\"false\" classAnnotationNames=\"org.immutables.value.Generated\">\n"
                        + "    <SourceLine classname=\"ghIssues.issue543.ImmutableFoobarValue.class\"/>\n"
                        + "  </Field>\n"
                        + "</BugInstance>\n"
                        + "</BugCollection>"));
        // write it out
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        origBC.writeXML(outBytes);

        // read it back in
        SortedBugCollection bc = new SortedBugCollection();
        bc.readXML(new StringReader(new String(outBytes.toByteArray(), StandardCharsets.UTF_8)));

        // check it
        assertEquals(1, bc.getCollection().size());
        BugInstance bug = bc.getCollection().iterator().next();
        assertEquals("UUF_UNUSED_FIELD", bug.getBugPattern().getType());
        assertEquals(3, bug.getAnnotations().size());
        for (BugAnnotation annotation : bug.getAnnotations()) {
            assertTrue(annotation instanceof PackageMemberAnnotation);
            List<String> javaAnnotationNames = ((PackageMemberAnnotation) annotation).getJavaAnnotationNames();
            assertEquals(1, javaAnnotationNames.size());
            assertEquals("org.immutables.value.Generated", javaAnnotationNames.get(0));
        }
    }
}
