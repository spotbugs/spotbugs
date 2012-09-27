package sfBugs;
//
//(c) Copyright Information Builders, Inc. 2009  All rights reserved. 
//
/*
** $Header: /home/cvs/webfocus/src/ibi/search/common/RequiredFields.java,v 1.11 2011/03/12 16:53:51 Peter_Lenahan Exp $:
** $Log: RequiredFields.java,v $
** Revision 1.11  2011/03/12 16:53:51  Peter_Lenahan
** PMD Code suggestions,  make fields final
**
** Revision 1.10  2010/11/06 00:43:57  Peter_Lenahan
** [p111675][>branch7702] Move the decoding of UTF-8 characters down a few lines
** So the "." can be interpreted correctly for requiredfields. the period is the "and" operator.
**
** Revision 1.9  2010/11/05 16:58:12  Peter_Lenahan
** [p111675][>branch7702] The lucene query parser has special characters which are used for query expansion.
** However, when these characters appear in a value on the tree control, they may cause parser errors.
** This fix escapes the parameters  so the parser does not fail.
**
** Revision 1.8  2010/11/05 15:24:41  Peter_Lenahan
** [p111675][>branch7702] When Toshi added the UTF8 encoding/decoding to support  international character, he changed the balance needed for
** the correct number of encodes/decodes in the bread crumbs. This fix adds some additional encodes in some places
** and removes some decodes in other places. So that the international character are supported and the correct number of
** encodes/decodes happen to display the data correctly.  These changes are all in the breadcrumb area.
**
** Revision 1.7  2010/09/09 21:15:09  Peter_Lenahan
** [>branch7702] Added Javadoc correction
**
** Revision 1.6  2010/07/01 14:11:52  Peter_Lenahan
** [p114373] [>branch7612] [>branch7702] If the URL is truncated because of the 2000 character limit, then
** the new required fields parameter may be chopped in half resulting in garbage. This checks to be sure
** there are actually 2 parameters before referencing the second parameter.
** Reported by Vikram when he had a large number of drilldowns.
**
** Revision 1.5  2010/06/03 16:18:01  Toshifumi_Kojima
** [p112960] Mag:WF7.6.11HF:Special char escapes in category tree
**
** Revision 1.4  2010/06/02 12:50:53  Toshifumi_Kojima
** [p112960] Mag:WF7.6.11HF:Special char escapes in category tree
**  Use encodeURIComponent() function at tree.js instead of escape().
**  Use java.net.URLDecoder.deocde() method at decode() of RequiredFields class.
**
** Revision 1.3  2010/04/11 16:48:58  Peter_Lenahan
** [p111674][>branch7701] [>branch7610_hotfix] [>branch7611_hotfix] Periods in Category tree cause tree and bread crumb trail to fail.
** Solution Double encode periods.
**
** Revision 1.2  2010/01/29 17:08:04  Peter_Lenahan
** [>branch77][>branch7611]Diagnostic logging added
**
** Revision 1.1  2010/01/26 20:36:04  Peter_Lenahan
** [>branch77][>branch7611] International Character issue, when appearing in the tree and on the other links of the Search Page.
**
*/



import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
/**
* This class centralizes the parsing of the URL parameters from the search for the
* &requiredfields= and the &newrequiredfields= parameters.
* 
* Because this was being done in 3 different places in the code it was important to centralize it.
* These parameters may contain international characters which need to be consistency decoded.
* These parameters are also used to generate the URL for 
* 1 - the tree, 
* 2 - The breadcrumbs, 
* 3 - The Sort By links.
* 
* @author Peter Lenahan
*
*/
public class Bug3559113 {
 private static final Logger magnifySearchLogger = Logger.getLogger("ibi.search.search");

 private final ArrayList<String> fieldnames=new ArrayList<String>();
 private final ArrayList<String> fieldvalues=new ArrayList<String>();

 @SuppressWarnings("unchecked")
 public ArrayList<String>getFields() {
     return (ArrayList<String>) fieldnames.clone();
 }
 @SuppressWarnings("unchecked")
 public ArrayList<String>getValues() {
     return (ArrayList<String>) fieldvalues.clone();
 }
 /**
  * This constructor parses the URL parameters and builds the tables.
  * It parses the requiredfields and newrequiredfields parameters on the URL.
  * then saves them in 2 tables which are accessed via the methods
  * getFields() and getValues().
  * The data stored in the table has had all the decoding applied.
  * 
  * The data stored in the requiredfields= parameter must be double encoded because
  * it can contain a "." or a "|" which are the google and/or logical operations
  * to apply to the query. We only support the "." and operation.
  * 
  * while the data stored in the newrequiredfields parameter can be singular decoded
  * 
  * @param req the request object of the servlet
  */
 public Bug3559113(final HttpServletRequest req) {
     String reqfieldParameter=req.getParameter("requiredfields");
     if (reqfieldParameter != null) {

         // Decode the data after you split the expression apart
         
         //String reqfields=decode(reqfieldParameter);
         String reqfields=reqfieldParameter;
//       try {
//           reqfields=new String(reqfieldParameter.getBytes(),"utf8");
//       } catch (UnsupportedEncodingException e) {
//           
//           e.printStackTrace();
//       }

         String [] ss_and=null;
         String [] ss_or=null;
         if (reqfields != null) {
             if (reqfields.indexOf('|') > -1 )
                 ss_or=reqfields.split("\\|");
             else
                 ss_and=reqfields.split("\\.");
     
             String [] ss_l=ss_or;
             // This is what we may be parsing
             // The Meta Tags can be separated by either a "." for and "and" operation
             // of a "|" for and "or" operation.
             //
             //   requiredfields=MetaName1:MetaValue1.MetaName2:MetaValue2.MetaName3:MetaValue3 ...
             // or
             //   requiredfields=MetaName1:MetaValue1|MetaName2:MetaValue2|MetaName3:MetaValue3 ...
             //
             for (int Logical_operator=1;Logical_operator <3;Logical_operator++) {
                 // parse both the or case as well as the and case
                 if (ss_l == null) {
                     ss_l=ss_and;
                     continue;
                 }
                 final int len=ss_l.length;
                 for (int l_index=0;l_index<len;l_index++) {
                     final String [] ss=decode(ss_l[l_index]).split(":");
                     if (ss.length > 1) {
                         // First we must separate the multiple keys if they exist. 
                         addField(ss[0], // String fieldname,
                                  ss[1]); // String fieldvalue,
                     }
                 }
                 ss_l=ss_and;
             }
 
         }
     }
     // Because of UTF-8 issues this is double encoded in the browser.
     // So decode it before it can be used
     String newRequiredFields=req.getParameter("newrequiredfields");
     if (newRequiredFields != null) {
         String  [] newrequiredfields=decode(req.getParameter("newrequiredfields")).split(":");
         // If the URL is chopped off because it exceeds the browser limit of 2000 characters, then
         // the ":" may not be there, check to be sure that there is actually 2 items.
         if (newrequiredfields != null && newrequiredfields.length > 1
                 
                 ) {
 
             addField(newrequiredfields[0], // String fieldname,
                      newrequiredfields[1]); // String fieldvalue
         }
     }
 }


 /**
  * This method does not decode the data, it must be called with the
  * data already decoded correctly by the caller
  * 
  * @param fieldname Which is added to the required fieldnames array
  * @param fieldvalue Which is added to the required fieldvalues array
  */
 private void addField(
         String fieldname,
         String fieldvalue) {
     String decodedfieldname=fieldname;
     String decodedfieldvalue=fieldvalue;
     
     if (magnifySearchLogger.isLoggable(Level.FINER)) 
         magnifySearchLogger.finer("RequiredFields():adecodedfieldnameddField: decodedfieldname="+decodedfieldname+", decodedfieldvalue= "+decodedfieldvalue);
     fieldnames.add(fieldname);
     fieldvalues.add(fieldvalue);
             
 }
 /**
  * 
  * @param s
  * @return
  */
 private String decode (final String s) {
     if (s == null)
         return null;
     try {
       return java.net.URLDecoder.decode(s, "UTF8");
     } catch ( UnsupportedEncodingException ex ) {
       if( magnifySearchLogger.isLoggable(Level.FINER) )
         magnifySearchLogger.finer("decode(): Use ibi.util.URLDecoder.decode()");
//       return ibi.util.URLDecoder.decode(s);
       return  s;

     }
 }
 /*
 private String decodeValue (final String s) {
     if (s == null)
         return null;
     return ibi.search.common.Encoders.requiredFieldsDecoder(s);
     
 }
 */  
}
