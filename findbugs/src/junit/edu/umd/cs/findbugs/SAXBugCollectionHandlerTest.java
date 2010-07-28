package edu.umd.cs.findbugs;

import java.io.StringReader;

import junit.framework.TestCase;

public class SAXBugCollectionHandlerTest extends TestCase {
    public void testBugInstanceXmlPropsNoReviews() throws Exception {
        SortedBugCollection bc = new SortedBugCollection();
        bc.readXML(new StringReader("<BugCollection version='1.3.10-dev-20100728' sequence='0' timestamp='1280333223462' analysisTimestamp='1280333224881' release=''>" +
                "  <Project projectName='Byte code Engineering Library (BCEL)'>" +
                "    <Jar>C:\\Users\\Keith\\Code\\findbugs\\findbugs\\lib\\bcel.jar</Jar>" +
                "    <Cloud id='edu.umd.cs.findbugs.cloud.appengine.findbugs-cloud'></Cloud>" +
                "  </Project>" +
                "  <BugInstance type='MS_MUTABLE_ARRAY' priority='1' abbrev='MS' category='MALICIOUS_CODE' instanceHash='1acc5c5b9b7ab9efacede805afe1e53a' instanceOccurrenceNum='0' instanceOccurrenceMax='0' rank='16' ageInDays='108' firstSeen='4/11/10 11:24 AM'>" +
                "    <ShortMessage>Field is a mutable array</ShortMessage>" +
                "    <LongMessage>org.apache.bcel.Constants.ACCESS_NAMES is a mutable array</LongMessage>" +
                "    <Class classname='org.apache.bcel.Constants' primary='true'>" +
                "      <SourceLine classname='org.apache.bcel.Constants' start='210' end='1443' sourcefile='Constants.java' sourcepath='org/apache/bcel/Constants.java'>" +
                "        <Message>At Constants.java:[lines 210-1443]</Message>" +
                "      </SourceLine>" +
                "      <Message>In class org.apache.bcel.Constants</Message>" +
                "    </Class>" +
                "    <Field classname='org.apache.bcel.Constants' name='ACCESS_NAMES' signature='[Ljava/lang/String;' isStatic='true' primary='true'>" +
                "      <SourceLine classname='org.apache.bcel.Constants' sourcefile='Constants.java' sourcepath='org/apache/bcel/Constants.java'>" +
                "        <Message>In Constants.java</Message>" +
                "      </SourceLine>" +
                "      <Message>Field org.apache.bcel.Constants.ACCESS_NAMES</Message>" +
                "    </Field>" +
                "    <SourceLine classname='org.apache.bcel.Constants' primary='true' start='210' end='210' startBytecode='89' endBytecode='89' sourcefile='Constants.java' sourcepath='org/apache/bcel/Constants.java'>" +
                "      <Message>At Constants.java:[line 210]</Message>" +
                "    </SourceLine>" +
                "  </BugInstance>" +
                "</BugCollection>"));
        assertEquals(1, bc.getCollection().size());
        BugInstance bug = bc.getCollection().iterator().next();
        assertEquals("1acc5c5b9b7ab9efacede805afe1e53a", bug.getInstanceHash());
        assertEquals(108, bug.getXmlProps().getAgeInDays());
        assertEquals(16, bug.getBugRank());
        assertEquals("4/11/10 11:24 AM", BugInstance.FIRST_SEEN_XML_FORMAT.format(bug.getXmlProps().getFirstSeen()));
        assertFalse(bug.getXmlProps().isNotAProblem());
        assertFalse(bug.getXmlProps().isShouldFix());
        assertEquals(0, bug.getXmlProps().getReviewCount());
        assertNull(bug.getXmlProps().getConsensus());
    }

    public void testBugInstanceXmlPropsWithReviews() throws Exception {
        SortedBugCollection bc = new SortedBugCollection();
        bc.readXML(new StringReader("<BugCollection version='1.3.10-dev-20100728' sequence='0' timestamp='1280333223462' analysisTimestamp='1280333224881' release=''>" +
                "  <Project projectName='Byte code Engineering Library (BCEL)'>" +
                "    <Jar>C:\\Users\\Keith\\Code\\findbugs\\findbugs\\lib\\bcel.jar</Jar>" +
                "    <Cloud id='edu.umd.cs.findbugs.cloud.appengine.findbugs-cloud'></Cloud>" +
                "  </Project>" +
                "  <BugInstance type='MS_MUTABLE_ARRAY' priority='1' abbrev='MS' category='MALICIOUS_CODE' instanceHash='1acc5c5b9b7ab9efacede805afe1e53a' instanceOccurrenceNum='0' instanceOccurrenceMax='0' rank='16' ageInDays='108' firstSeen='4/11/10 11:24 AM' reviews='4' consensus='NOT_A_BUG' notAProblem='true'>" +
                "    <ShortMessage>Field is a mutable array</ShortMessage>" +
                "    <LongMessage>org.apache.bcel.Constants.ACCESS_NAMES is a mutable array</LongMessage>" +
                "    <Class classname='org.apache.bcel.Constants' primary='true'>" +
                "      <SourceLine classname='org.apache.bcel.Constants' start='210' end='1443' sourcefile='Constants.java' sourcepath='org/apache/bcel/Constants.java'>" +
                "        <Message>At Constants.java:[lines 210-1443]</Message>" +
                "      </SourceLine>" +
                "      <Message>In class org.apache.bcel.Constants</Message>" +
                "    </Class>" +
                "    <Field classname='org.apache.bcel.Constants' name='ACCESS_NAMES' signature='[Ljava/lang/String;' isStatic='true' primary='true'>" +
                "      <SourceLine classname='org.apache.bcel.Constants' sourcefile='Constants.java' sourcepath='org/apache/bcel/Constants.java'>" +
                "        <Message>In Constants.java</Message>" +
                "      </SourceLine>" +
                "      <Message>Field org.apache.bcel.Constants.ACCESS_NAMES</Message>" +
                "    </Field>" +
                "    <SourceLine classname='org.apache.bcel.Constants' primary='true' start='210' end='210' startBytecode='89' endBytecode='89' sourcefile='Constants.java' sourcepath='org/apache/bcel/Constants.java'>" +
                "      <Message>At Constants.java:[line 210]</Message>" +
                "    </SourceLine>" +
                "  </BugInstance>" +
                "</BugCollection>"));
        assertEquals(1, bc.getCollection().size());
        BugInstance bug = bc.getCollection().iterator().next();
        assertEquals("1acc5c5b9b7ab9efacede805afe1e53a", bug.getInstanceHash());
        assertEquals(108, bug.getXmlProps().getAgeInDays());
        assertEquals(16, bug.getBugRank());
        assertEquals("4/11/10 11:24 AM", BugInstance.FIRST_SEEN_XML_FORMAT.format(bug.getXmlProps().getFirstSeen()));
        assertTrue(bug.getXmlProps().isNotAProblem());
        assertFalse(bug.getXmlProps().isShouldFix());
        assertEquals(4, bug.getXmlProps().getReviewCount());
        assertEquals("NOT_A_BUG", bug.getXmlProps().getConsensus());
    }

    public void testBugInstanceXmlPropsWithReviewsShouldFix() throws Exception {
        SortedBugCollection bc = new SortedBugCollection();
        bc.readXML(new StringReader("<BugCollection version='1.3.10-dev-20100728' sequence='0' timestamp='1280333223462' analysisTimestamp='1280333224881' release=''>" +
                "  <Project projectName='Byte code Engineering Library (BCEL)'>" +
                "    <Jar>C:\\Users\\Keith\\Code\\findbugs\\findbugs\\lib\\bcel.jar</Jar>" +
                "    <Cloud id='edu.umd.cs.findbugs.cloud.appengine.findbugs-cloud'></Cloud>" +
                "  </Project>" +
                "  <BugInstance type='MS_MUTABLE_ARRAY' priority='1' abbrev='MS' category='MALICIOUS_CODE' instanceHash='1acc5c5b9b7ab9efacede805afe1e53a' instanceOccurrenceNum='0' instanceOccurrenceMax='0' rank='16' ageInDays='108' firstSeen='4/11/10 11:24 AM' reviews='4' consensus='SHOULD_FIX' shouldFix='true'>" +
                "    <ShortMessage>Field is a mutable array</ShortMessage>" +
                "    <LongMessage>org.apache.bcel.Constants.ACCESS_NAMES is a mutable array</LongMessage>" +
                "    <Class classname='org.apache.bcel.Constants' primary='true'>" +
                "      <SourceLine classname='org.apache.bcel.Constants' start='210' end='1443' sourcefile='Constants.java' sourcepath='org/apache/bcel/Constants.java'>" +
                "        <Message>At Constants.java:[lines 210-1443]</Message>" +
                "      </SourceLine>" +
                "      <Message>In class org.apache.bcel.Constants</Message>" +
                "    </Class>" +
                "    <Field classname='org.apache.bcel.Constants' name='ACCESS_NAMES' signature='[Ljava/lang/String;' isStatic='true' primary='true'>" +
                "      <SourceLine classname='org.apache.bcel.Constants' sourcefile='Constants.java' sourcepath='org/apache/bcel/Constants.java'>" +
                "        <Message>In Constants.java</Message>" +
                "      </SourceLine>" +
                "      <Message>Field org.apache.bcel.Constants.ACCESS_NAMES</Message>" +
                "    </Field>" +
                "    <SourceLine classname='org.apache.bcel.Constants' primary='true' start='210' end='210' startBytecode='89' endBytecode='89' sourcefile='Constants.java' sourcepath='org/apache/bcel/Constants.java'>" +
                "      <Message>At Constants.java:[line 210]</Message>" +
                "    </SourceLine>" +
                "  </BugInstance>" +
                "</BugCollection>"));
        assertEquals(1, bc.getCollection().size());
        BugInstance bug = bc.getCollection().iterator().next();
        assertEquals("1acc5c5b9b7ab9efacede805afe1e53a", bug.getInstanceHash());
        assertEquals(108, bug.getXmlProps().getAgeInDays());
        assertEquals(16, bug.getBugRank());
        assertEquals("4/11/10 11:24 AM", BugInstance.FIRST_SEEN_XML_FORMAT.format(bug.getXmlProps().getFirstSeen()));
        assertFalse(bug.getXmlProps().isNotAProblem());
        assertTrue(bug.getXmlProps().isShouldFix());
        assertEquals(4, bug.getXmlProps().getReviewCount());
        assertEquals("SHOULD_FIX", bug.getXmlProps().getConsensus());
    }
}
