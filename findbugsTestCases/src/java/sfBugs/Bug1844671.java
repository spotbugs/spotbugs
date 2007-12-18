package sfBugs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Bug1844671 {
	 public void falsePositive1(){
	       FileWriter fw = null;
	       try {
	         fw = new FileWriter(new File(""));
	       } catch(IOException e) {
	    	   System.out.println(e);
	       } finally {
	           try {
	               if( fw != null) { // no false positive
	                   fw.close();
	               }
	           } catch(IOException ioe) { }
	       }
	   }
	 
	 public void falsePositive2(){
	       FileWriter fw = null;
	       try {
	         fw = new FileWriter(new File(""));
	       } catch(IOException e) {
	    	   System.out.println(e);
	       } finally {
	           try {
	               if( null != fw) { // false positive
	                   fw.close();
	               }
	           } catch(IOException ioe) { }
	       }
	   }

	 public void falsePositive3(){
	       FileWriter tmp = null;
	       FileWriter fw = null;
	       try {
	    	 tmp = new FileWriter(new File("")); // true positive
	         fw = new FileWriter(new File(""));  // true positive
	       } catch(IOException e) {
	    	   System.out.println(e);
	       } finally {
	           try {
	               if( tmp != fw) {
	                   if(fw != null) fw.close();
	                   tmp.close();
	               }
	           } catch(IOException ioe) { }
	       }
	   }
}
