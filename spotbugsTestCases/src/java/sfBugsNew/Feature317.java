package sfBugsNew;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

public class Feature317 {
    // Quick-and-dirty benchmark to measure the speed
    public static void main(String... args) throws Exception {
        if(args.length == 0) {
            System.err.println("Please supply FindBugs XML file as an argument");
            return;
        }
        File file = new File(args[0]);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(file);

        for(int n=0; n<5; n++) {
            long start = System.currentTimeMillis();
            getAvgPrioritySlow(document);
            if(n > 0) {
                System.out.println("Slow: "+(System.currentTimeMillis()-start));
            }
        }
        for(int n=0; n<5; n++) {
            long start = System.currentTimeMillis();
            getAvgPriorityFast(document);
            if(n > 0) {
                System.out.println("Fast: "+(System.currentTimeMillis()-start));
            }
        }
    }

    @NoWarning("IIL_ELEMENTS_GET_LENGTH_IN_LOOP")
    public static double getAvgPriorityFast(Document document) {
        NodeList bugs = document.getElementsByTagName("BugInstance");
        int prioritySum = 0;
        int length = bugs.getLength();
        for(int i=0; i<length; i++) {
            prioritySum += Integer.parseInt(((Element)bugs.item(i)).getAttribute("priority"));
        }
        return ((double)prioritySum)/length;
    }

    @ExpectWarning("IIL_ELEMENTS_GET_LENGTH_IN_LOOP")
    public static double getAvgPrioritySlow(Document document) {
        NodeList bugs = document.getElementsByTagName("BugInstance");
        int prioritySum = 0;
        for(int i=0; i<bugs.getLength(); i++) {
            prioritySum += Integer.parseInt(((Element)bugs.item(i)).getAttribute("priority"));
        }
        return ((double)prioritySum)/bugs.getLength();
    }

    @NoWarning("IIL_ELEMENTS_GET_LENGTH_IN_LOOP")
    public static double getAvgPriorityFastMult(Document[] documents) {
        int prioritySum = 0;
        int bugsCount = 0;
        for(Document document : documents) {
            NodeList bugs = document.getElementsByTagName("BugInstance");
            // getLength is in a loop, but getElementsByTagName was in the same loop: should not trigger
            int length = bugs.getLength();
            for(int i=0; i<length; i++) {
                prioritySum += Integer.parseInt(((Element)bugs.item(i)).getAttribute("priority"));
            }
            bugsCount+=length;
        }
        return ((double)prioritySum)/bugsCount;
    }

    @ExpectWarning("IIL_ELEMENTS_GET_LENGTH_IN_LOOP")
    public static double getAvgPrioritySlowMult(Document[] documents) {
        int prioritySum = 0;
        int bugsCount = 0;
        for(Document document : documents) {
            NodeList bugs = document.getElementsByTagName("BugInstance");
            // should trigger, because getLength is in the nested loop
            for(int i=0; i<bugs.getLength(); i++) {
                prioritySum += Integer.parseInt(((Element)bugs.item(i)).getAttribute("priority"));
            }
            bugsCount+=bugs.getLength();
        }
        return ((double)prioritySum)/bugsCount;
    }
}
