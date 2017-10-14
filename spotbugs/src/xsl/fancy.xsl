<?xml version="1.0" encoding="UTF-8" ?>
<!--
  Copyright (C) 2005, 2006 Etienne Giraudy, InStranet Inc

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this library; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
-->

<xsl:transform version="2.0"
               xmlns:xsl="http://www.w3.org/1999/XSL/Transform" >
   <xsl:output
         method="xml" indent="yes"
         doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
         doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
         encoding="UTF-8"/>

    <!--
        Parameter for specifying HTMLized sources location; if current dir, use "./"
        If not passed, no links to sources are generated.
        because of back-compatibility reasons.
        The source filename should be package.class.java.html
        The source can have line no anchors like #11 -->
    <xsl:param name="htmlsrcpath"></xsl:param>

   <!--xsl:key name="lbc-category-key"    match="/BugCollection/BugInstance" use="@category" /-->
   <xsl:key name="lbc-code-key"        match="/BugCollection/BugInstance" use="concat(@category,@abbrev)" />
   <xsl:key name="lbc-bug-key"         match="/BugCollection/BugInstance" use="concat(@category,@abbrev,@type)" />
   <xsl:key name="lbp-class-b-t"  match="/BugCollection/BugInstance" use="concat(Class/@classname,@type)" />

<xsl:template match="/" >

<html>
   <head>
      <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
      <title>
        SpotBugs (<xsl:value-of select="/BugCollection/@version" />)
         Analysis for
         <xsl:choose>
            <xsl:when test='string-length(/BugCollection/Project/@projectName)>0'><xsl:value-of select="/BugCollection/Project/@projectName" /></xsl:when>
            <xsl:otherwise><xsl:value-of select="/BugCollection/Project/@filename" /></xsl:otherwise>
         </xsl:choose>
      </title>
      <script type="text/javascript">
         function show(foo) {
            document.getElementById(foo).style.display="block";
         }
         function hide(foo) {
            document.getElementById(foo).style.display="none";
         }
         function toggle(foo) {
            if( document.getElementById(foo).style.display == "none") {
               show(foo);
            } else {
               if( document.getElementById(foo).style.display == "block") {
                  hide(foo);
               } else {
                  show(foo);
               }
            }
         }

         function showmenu(foo) {
            if( document.getElementById(foo).style.display == "none") {
               hide("bug-summary");
               document.getElementById("bug-summary-tab").className="menu-tab";
               hide("analysis-data");
               document.getElementById("analysis-data-tab").className="menu-tab";
               //hide("list-by-b-t");
               //document.getElementById("list-by-b-t-tab").className="menu-tab";
               hide("list-by-package");
               document.getElementById("list-by-package-tab").className="menu-tab";
               hide("list-by-category");
               document.getElementById("list-by-category-tab").className="menu-tab";
               document.getElementById(foo+"-tab").className="menu-tab-selected";
               show(foo);

            }
            // else menu already selected!
         }
         function showbug(buguid, list) {
            var bugplaceholder   = document.getElementById(buguid+'-ph-'+list);
            var bug              = document.getElementById(buguid);

            if ( bugplaceholder==null) {
               alert(buguid+'-ph-'+list+' - '+buguid+' - bugplaceholder==null');
               return;
            }
            if ( bug==null) {
               alert(buguid+'-ph-'+list+' - '+buguid+' - bug==null');
               return;
            }

            var oldBug = bugplaceholder.innerHTML;
            var newBug = bug.innerHTML;
            //alert(oldBug);
            //alert(newBug);
            toggle(buguid+'-ph-'+list);
            bugplaceholder.innerHTML = newBug;
         }
      </script>
      <script type='text/javascript'><xsl:text disable-output-escaping='yes'>
     /* <![CDATA[ */
         // Extended Tooltip Javascript
         // copyright 9th August 2002, 3rd July 2005
         // by Stephen Chapman, Felgall Pty Ltd

         // permission is granted to use this javascript provided that the below code is not altered
         var DH = 0;var an = 0;var al = 0;var ai = 0;if (document.getElementById) {ai = 1; DH = 1;}else {if (document.all) {al = 1; DH = 1;} else { browserVersion = parseInt(navigator.appVersion); if (navigator.appName.indexOf('Netscape') != -1) if (browserVersion == 4) {an = 1; DH = 1;}}}
         function fd(oi, wS) {if (ai) return wS ? document.getElementById(oi).style:document.getElementById(oi); if (al) return wS ? document.all[oi].style: document.all[oi]; if (an) return document.layers[oi];}
         function pw() {return window.innerWidth != null? window.innerWidth: document.body.clientWidth != null? document.body.clientWidth:null;}
         function mouseX(evt) {if (evt.pageX) return evt.pageX; else if (evt.clientX)return evt.clientX + (document.documentElement.scrollLeft ?  document.documentElement.scrollLeft : document.body.scrollLeft); else return null;}
         function mouseY(evt) {if (evt.pageY) return evt.pageY; else if (evt.clientY)return evt.clientY + (document.documentElement.scrollTop ? document.documentElement.scrollTop : document.body.scrollTop); else return null;}
         function popUp(evt,oi) {if (DH) {var wp = pw(); ds = fd(oi,1); dm = fd(oi,0); st = ds.visibility; if (dm.offsetWidth) ew = dm.offsetWidth; else if (dm.clip.width) ew = dm.clip.width; if (st == "visible" || st == "show") { ds.visibility = "hidden"; } else {tv = mouseY(evt) + 20; lv = mouseX(evt) - (ew/4); if (lv < 2) lv = 2; else if (lv + ew > wp) lv -= ew/2; if (!an) {lv += 'px';tv += 'px';} ds.left = lv; ds.top = tv; ds.visibility = "visible";}}}
  /* ]]> */
</xsl:text></script>
      <style type='text/css'>
         html, body {
            background-color: #ffffff;
         }
         a, a:link , a:active, a:visited, a:hover {
            text-decoration: none; color: black;
         }
         .b-r a {
            text-decoration: underline; color: blue;
         }
         div, span {
            vertical-align: top;
         }
         p {
            margin: 0px;
         }
         h1 {
            /*font-size: 14pt;*/
            color: red;
         }
         #menu {
            margin-bottom: 10px;
         }
         #menu ul {
            margin-left: 0;
            padding-left: 0;
            display: inline;
         }
         #menu ul li {
            margin-left: 0;
            margin-bottom: 0;
            padding: 2px 15px 5px;
            border: 1px solid #000;
            list-style: none;
            display: inline;
         }
         #menu ul li.here {
            border-bottom: 1px solid #ffc;
            list-style: none;
            display: inline;
         }
         .menu-tab {
            background: white;
         }
         .menu-tab:hover {
            background: grey;
         }
         .menu-tab-selected {
            background: #aaaaaa;
         }
         #analysis-data ul {
            margin-left: 15px;
         }
         #analyzed-files, #used-libraries, #analysis-error {
           margin: 2px;
           border: 1px black solid;
           padding: 2px;
           float: left;
           overflow:auto;
         }
         #analyzed-files {
           width: 25%;
         }
         #used-libraries {
           width: 25%;
         }
         #analysis-error {
           width: 40%;
         }
         div.summary {
            width:100%;
            text-align:left;
         }
         .summary table {
            border:1px solid black;
         }
         .summary th {
            background: #aaaaaa;
            color: white;
         }
         .summary th, .summary td {
            padding: 2px 4px 2px 4px;
         }
         .summary-name {
            background: #eeeeee;
            text-align:left;
         }
         .summary-size {
            background: #eeeeee;
            text-align:center;
         }
         .summary-ratio {
            background: #eeeeee;
            text-align:center;
         }
         .summary-priority-all {
            background: #dddddd;
            text-align:center;
         }
         .summary-priority-1 {
            background: red;
            text-align:center;
         }
         .summary-priority-2 {
            background: orange;
            text-align:center;
         }
         .summary-priority-3 {
            background: green;
            text-align:center;
         }
         .summary-priority-4 {
            background: blue;
            text-align:center;
         }
         .ob {
            border: 1px solid black;
            margin: 10px;
         }
         .ob-t {
            border-bottom: 1px solid #000000; font-size: 12pt; font-weight: bold;
            background: #cccccc; margin: 0; padding: 0 5px 0 5px;
         }
         .t-h {
            font-weight: normal;
         }
         .ib-1, .ib-2 {
            margin: 0 0 0 10px;
         }
         .ib-1-t, .ib-2-t {
            border-bottom: 1px solid #000000; border-left: 1px solid #000000;
            margin: 0; padding: 0 5px 0 5px;
            font-size: 12pt; font-weight: bold; background: #cccccc;
         }
         .bb {
            border-bottom: 1px solid #000000; border-left: 1px solid #000000;
         }
         .b-1 {
            background: red; height: 0.5em; width: 1em;
            margin-right: 0.5em;
         }
         .b-2 {
            background: orange; height: 0.5em; width: 1em;
            margin-right: 0.5em;
         }
         .b-3 {
            background: green; height: 0.5em; width: 1em;
            margin-right: 0.5em;
         }
         .b-4 {
            background: blue; height: 0.5em; width: 1em;
            margin-right: 0.5em;
         }
         .b-t {
         }
         .b-r {
            font-size: 10pt; font-weight: bold; padding: 0 0 0 60px;
         }
         .b-d {
            font-weight: normal; background: #eeeee0;
            padding: 0 5px 0 5px; margin: 0px;
         }
         .bug-placeholder {
            top:140px;
            border:1px solid black;
            display:none;
         }
         .tip {
            border:solid 1px #666666;
            width:600px;
            padding:3px;
            position:absolute;
            z-index:100;
            visibility:hidden;
            color:#333333;
            top:20px;
            left:90px;
            background-color:#ffffcc;
            layer-background-color:#ffffcc;
         }


      </style>
   </head>
   <body>
   <div id='content'>
      <h1>
         SpotBugs (<xsl:value-of select="/BugCollection/@version" />)
         Analysis for
         <xsl:choose>
            <xsl:when test='string-length(/BugCollection/Project/@projectName)>0'><xsl:value-of select="/BugCollection/Project/@projectName" /></xsl:when>
            <xsl:otherwise><xsl:value-of select="/BugCollection/Project/@filename" /></xsl:otherwise>
         </xsl:choose>
      </h1>
      <div id="menu">
         <ul>
            <li id='bug-summary-tab' class='menu-tab-selected'>
               <xsl:attribute name="onclick">showmenu('bug-summary');return false;</xsl:attribute>
               <a href='' onclick='return false;'>Bug Summary</a>
            </li>
            <li id='analysis-data-tab' class='menu-tab'>
               <xsl:attribute name="onclick">showmenu('analysis-data');return false;</xsl:attribute>
               <a href='' onclick='return false;'>Analysis Information</a>
            </li>
            <li id='list-by-category-tab' class='menu-tab'>
               <xsl:attribute name="onclick">showmenu('list-by-category');return false;</xsl:attribute>
               <a href='' onclick='return false;'>List bugs by bug category</a>
            </li>
            <li id='list-by-package-tab' class='menu-tab'>
               <xsl:attribute name="onclick">showmenu('list-by-package');return false;</xsl:attribute>
               <a href='' onclick='return false;'>List bugs by package</a>
            </li>
         </ul>
      </div>
      <xsl:call-template name="generateSummary" />
      <xsl:call-template name="analysis-data" />
      <xsl:call-template name="list-by-category" />
      <xsl:call-template name="list-by-package" />


      <!-- advanced tooltips -->
      <xsl:for-each select="/BugCollection/BugPattern">
         <xsl:variable name="b-t"><xsl:value-of select="@type" /></xsl:variable>
         <div>
            <xsl:attribute name="id">tip-<xsl:value-of select="$b-t" /></xsl:attribute>
            <xsl:attribute name="class">tip</xsl:attribute>
            <b><xsl:value-of select="@abbrev" /> / <xsl:value-of select="@type" /></b><br/>
            <xsl:value-of select="/BugCollection/BugPattern[@type=$b-t]/Details" disable-output-escaping="yes" />
         </div>
      </xsl:for-each>

      <!-- bug descriptions - hidden -->
      <xsl:for-each select="/BugCollection/BugInstance[not(@last)]">
            <div style="display:none;">
               <xsl:attribute name="id">b-uid-<xsl:value-of select="@instanceHash" />-<xsl:value-of select="@instanceOccurrenceNum" /></xsl:attribute>
               <xsl:for-each select="*/Message">
                   <xsl:choose>
                    <xsl:when test="parent::SourceLine and $htmlsrcpath != '' ">
                      <div class="b-r"><a>
                        <xsl:attribute name="href"><xsl:value-of select="$htmlsrcpath"/><xsl:value-of select="../@sourcepath" />.html#<xsl:value-of select="../@start" /></xsl:attribute>
                        <xsl:apply-templates />
                      </a></div>
                    </xsl:when>
                    <xsl:otherwise>
                      <div class="b-r"><xsl:apply-templates /></div>
                    </xsl:otherwise>
                   </xsl:choose>
               </xsl:for-each>
               <div class="b-d">
                  <xsl:value-of select="LongMessage" disable-output-escaping="no" />
               </div>
            </div>
      </xsl:for-each>
   </div>
   <div id='fixedbox'>
      <div id='bug-placeholder'></div>
   </div>
   </body>
</html>
</xsl:template>

<!-- ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
<!-- generate summary report from stats -->
<xsl:template name="generateSummary" >
<div class='summary' id='bug-summary'>
   <h2>SpotBugs Analysis generated at: <xsl:value-of select="/BugCollection/FindBugsSummary/@timestamp" /></h2>
   <table>
      <tr>
         <th>Package</th>
         <th>Code Size</th>
         <th>Bugs</th>
         <th>High Prio Bugs</th>
         <th>Medium Prio Bugs</th>
         <th>Low Prio Bugs</th>
         <th>Exp. Bugs</th>
         <th>Ratio</th>
      </tr>
      <tr>
         <td class='summary-name'>
            Overall
            (<xsl:value-of select="/BugCollection/FindBugsSummary/@num_packages" /> packages),
            (<xsl:value-of select="/BugCollection/FindBugsSummary/@total_classes" /> classes)
         </td>
         <td class='summary-size'><xsl:value-of select="/BugCollection/FindBugsSummary/@total_size" /></td>
         <td class='summary-priority-all'><xsl:value-of select="/BugCollection/FindBugsSummary/@total_bugs" /></td>
         <td class='summary-priority-1'><xsl:value-of select="/BugCollection/FindBugsSummary/@priority_1" /></td>
         <td class='summary-priority-2'><xsl:value-of select="/BugCollection/FindBugsSummary/@priority_2" /></td>
         <td class='summary-priority-3'><xsl:value-of select="/BugCollection/FindBugsSummary/@priority_3" /></td>
         <td class='summary-priority-4'><xsl:value-of select="/BugCollection/FindBugsSummary/@priority_4" /></td>
         <td class='summary-ratio'></td>
      </tr>
      <xsl:for-each select="/BugCollection/FindBugsSummary/PackageStats">
         <xsl:sort select="@package" />
         <xsl:if test="@total_bugs!='0'" >
            <tr>
               <td class='summary-name'><xsl:value-of select="@package" /></td>
               <td class='summary-size'><xsl:value-of select="@total_size" /></td>
               <td class='summary-priority-all'><xsl:value-of select="@total_bugs" /></td>
               <td class='summary-priority-1'><xsl:value-of select="@priority_1" /></td>
               <td class='summary-priority-2'><xsl:value-of select="@priority_2" /></td>
               <td class='summary-priority-3'><xsl:value-of select="@priority_3" /></td>
               <td class='summary-priority-4'><xsl:value-of select="@priority_4" /></td>
               <td class='summary-ratio'></td>
<!--
               <xsl:for-each select="ClassStats">
                  <xsl:if test="@bugs!='0'" >
                  <li>
                     <xsl:value-of select="@class" /> - total: <xsl:value-of select="@bugs" />
                  </li>
                  </xsl:if>
               </xsl:for-each>
-->
            </tr>
         </xsl:if>
      </xsl:for-each>
   </table>
</div>
</xsl:template>

<!-- ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
<!-- display analysis info -->
<xsl:template name="analysis-data">
      <div id='analysis-data' style='display:none;'>
         <div id='analyzed-files'>
            <h3>Analyzed Files:</h3>
            <ul>
               <xsl:for-each select="/BugCollection/Project/Jar">
                  <li><xsl:apply-templates /></li>
               </xsl:for-each>
            </ul>
         </div>
         <div id='used-libraries'>
            <h3>Used Libraries:</h3>
            <ul>
               <xsl:for-each select="/BugCollection/Project/AuxClasspathEntry">
                  <li><xsl:apply-templates /></li>
               </xsl:for-each>
               <xsl:if test="count(/BugCollection/Project/AuxClasspathEntry)=0" >
                  <li>None</li>
               </xsl:if>
            </ul>
         </div>
         <div id='analysis-error'>
            <h3>Analysis Errors:</h3>
            <ul>
               <xsl:variable name="error-count"
                             select="count(/BugCollection/Errors/MissingClass)" />
               <xsl:if test="$error-count=0" >
                  <li>None</li>
               </xsl:if>
               <xsl:if test="$error-count>0" >
                  <li>Missing ref classes for analysis:
                     <ul>
                        <xsl:for-each select="/BugCollection/Errors/MissingClass">
                           <li><xsl:apply-templates /></li>
                        </xsl:for-each>
                     </ul>
                  </li>
               </xsl:if>
            </ul>
         </div>
      </div>
</xsl:template>

<!-- ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
<!-- show priorities helper -->
<xsl:template name="helpPriorities">
   <span>
      <xsl:attribute name="class">b-1</xsl:attribute>
      <xsl:text disable-output-escaping="yes">&amp;nbsp;&amp;nbsp;</xsl:text>
   </span> High Prio
   <span>
      <xsl:attribute name="class">b-2</xsl:attribute>
      <xsl:text disable-output-escaping="yes">&amp;nbsp;&amp;nbsp;</xsl:text>
   </span> Medium Prio
   <span>
      <xsl:attribute name="class">b-3</xsl:attribute>
      <xsl:text disable-output-escaping="yes">&amp;nbsp;&amp;nbsp;</xsl:text>
   </span> Low Prio
   <span>
      <xsl:attribute name="class">b-4</xsl:attribute>
      <xsl:text disable-output-escaping="yes">&amp;nbsp;&amp;nbsp;</xsl:text>
   </span> Exp.
</xsl:template>

<!-- ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
<!-- display the details of a bug -->
<xsl:template name="display-bug" >
   <xsl:param name="b-t"    select="''" />
   <xsl:param name="bug-id"      select="''" />
   <xsl:param name="which-list"  select="''" />
   <div class="bb">
      <a>
         <xsl:attribute name="href"></xsl:attribute>
         <xsl:attribute name="onclick">showbug('b-uid-<xsl:value-of select="@instanceHash" />-<xsl:value-of select="@instanceOccurrenceNum" />','<xsl:value-of select="$which-list" />');return false;</xsl:attribute>
         <span>
            <xsl:attribute name="class">b-<xsl:value-of select="@priority"/></xsl:attribute>
            <xsl:text disable-output-escaping="yes">&amp;nbsp;&amp;nbsp;</xsl:text>
         </span>
         <span class="b-t"><xsl:value-of select="@abbrev" />: </span> <xsl:value-of select="Class/Message" />
      </a>
      <div style="display:none;">
         <xsl:attribute name="id">b-uid-<xsl:value-of select="@instanceHash" />-<xsl:value-of select="@instanceOccurrenceNum" />-ph-<xsl:value-of select="$which-list" /></xsl:attribute>
         loading...
      </div>
   </div>
</xsl:template>

<!-- ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
<!-- main template for the list by category -->
<xsl:template name="list-by-category" >
   <div id='list-by-category' class='data-box' style='display:none;'>
      <xsl:call-template name="helpPriorities" />
      <xsl:variable name="unique-category" select="/BugCollection/BugCategory/@category"/>
      <xsl:for-each select="$unique-category">
         <xsl:sort select="." order="ascending" />
            <xsl:call-template name="categories">
               <xsl:with-param name="category" select="." />
            </xsl:call-template>
      </xsl:for-each>
   </div>
</xsl:template>

<xsl:template name="categories" >
   <xsl:param name="category" select="''" />
   <xsl:variable name="category-count"
                       select="count(/BugCollection/BugInstance[@category=$category and not(@last)])" />
   <xsl:variable name="category-count-p1"
                       select="count(/BugCollection/BugInstance[@category=$category and @priority='1' and not(@last)])" />
   <xsl:variable name="category-count-p2"
                       select="count(/BugCollection/BugInstance[@category=$category and @priority='2' and not(@last)])" />
   <xsl:variable name="category-count-p3"
                       select="count(/BugCollection/BugInstance[@category=$category and @priority='3' and not(@last)])" />
   <xsl:variable name="category-count-p4"
                       select="count(/BugCollection/BugInstance[@category=$category and @priority='4' and not(@last)])" />
   <div class='ob'>
      <div class='ob-t'>
         <a>
            <xsl:attribute name="href"></xsl:attribute>
            <xsl:attribute name="onclick">toggle('category-<xsl:value-of select="$category" />');return false;</xsl:attribute>
            <xsl:value-of select="/BugCollection/BugCategory[@category=$category]/Description" />
            (<xsl:value-of select="$category-count" />:
            <span class='t-h'><xsl:value-of select="$category-count-p1" />/<xsl:value-of select="$category-count-p2" />/<xsl:value-of select="$category-count-p3" />/<xsl:value-of select="$category-count-p4" /></span>)
         </a>
      </div>
      <div style="display:none;">
         <xsl:attribute name="id">category-<xsl:value-of select="$category" /></xsl:attribute>
         <xsl:call-template name="list-by-category-and-code">
            <xsl:with-param name="category" select="$category" />
         </xsl:call-template>
      </div>
   </div>
</xsl:template>

<xsl:template name="list-by-category-and-code" >
   <xsl:param name="category" select="''" />
   <xsl:variable name="unique-code" select="/BugCollection/BugInstance[@category=$category and not(@last) and generate-id()= generate-id(key('lbc-code-key',concat(@category,@abbrev)))]/@abbrev" />
   <xsl:for-each select="$unique-code">
      <xsl:sort select="." order="ascending" />
         <xsl:call-template name="codes">
            <xsl:with-param name="category" select="$category" />
            <xsl:with-param name="code" select="." />
         </xsl:call-template>
   </xsl:for-each>
</xsl:template>

<xsl:template name="codes" >
   <xsl:param name="category" select="''" />
   <xsl:param name="code"     select="''" />
   <xsl:variable name="code-count"
                       select="count(/BugCollection/BugInstance[@category=$category and @abbrev=$code and not(@last)])" />
   <xsl:variable name="code-count-p1"
                       select="count(/BugCollection/BugInstance[@category=$category and @abbrev=$code and @priority='1' and not(@last)])" />
   <xsl:variable name="code-count-p2"
                       select="count(/BugCollection/BugInstance[@category=$category and @abbrev=$code and @priority='2' and not(@last)])" />
   <xsl:variable name="code-count-p3"
                       select="count(/BugCollection/BugInstance[@category=$category and @abbrev=$code and @priority='3' and not(@last)])" />
   <xsl:variable name="code-count-p4"
                       select="count(/BugCollection/BugInstance[@category=$category and @abbrev=$code and @priority='4' and not(@last)])" />
   <div class='ib-1'>
      <div class="ib-1-t">
         <a>
            <xsl:attribute name="href"></xsl:attribute>
            <xsl:attribute name="onclick">toggle('category-<xsl:value-of select="$category" />-and-code-<xsl:value-of select="$code" />');return false;</xsl:attribute>
            <xsl:value-of select="$code" />: <xsl:value-of select="/BugCollection/BugCode[@abbrev=$code]/Description" />
            (<xsl:value-of select="$code-count" />:
            <span class='t-h'><xsl:value-of select="$code-count-p1" />/<xsl:value-of select="$code-count-p2" />/<xsl:value-of select="$code-count-p3" />/<xsl:value-of select="$code-count-p4" /></span>)
         </a>
      </div>
      <div style="display:none;">
         <xsl:attribute name="id">category-<xsl:value-of select="$category" />-and-code-<xsl:value-of select="$code" /></xsl:attribute>
         <xsl:call-template name="list-by-category-and-code-and-bug">
            <xsl:with-param name="category" select="$category" />
            <xsl:with-param name="code" select="$code" />
         </xsl:call-template>
      </div>
   </div>
</xsl:template>

<xsl:template name="list-by-category-and-code-and-bug" >
   <xsl:param name="category" select="''" />
   <xsl:param name="code" select="''" />
   <xsl:variable name="unique-bug" select="/BugCollection/BugInstance[@category=$category and not(@last) and @abbrev=$code and generate-id()= generate-id(key('lbc-bug-key',concat(@category,@abbrev,@type)))]/@type" />
   <xsl:for-each select="$unique-bug">
      <xsl:sort select="." order="ascending" />
         <xsl:call-template name="bugs">
            <xsl:with-param name="category" select="$category" />
            <xsl:with-param name="code" select="$code" />
            <xsl:with-param name="bug" select="." />
         </xsl:call-template>
   </xsl:for-each>
</xsl:template>

<xsl:template name="bugs" >
   <xsl:param name="category" select="''" />
   <xsl:param name="code"     select="''" />
   <xsl:param name="bug"      select="''" />
   <xsl:variable name="bug-count"
                       select="count(/BugCollection/BugInstance[@category=$category and @abbrev=$code and @type=$bug and not(@last)])" />
   <xsl:variable name="bug-count-p1"
                       select="count(/BugCollection/BugInstance[@category=$category and @abbrev=$code and @type=$bug and @priority='1' and not(@last)])" />
   <xsl:variable name="bug-count-p2"
                       select="count(/BugCollection/BugInstance[@category=$category and @abbrev=$code and @type=$bug and @priority='2' and not(@last)])" />
   <xsl:variable name="bug-count-p3"
                       select="count(/BugCollection/BugInstance[@category=$category and @abbrev=$code and @type=$bug and @priority='3' and not(@last)])" />
   <xsl:variable name="bug-count-p4"
                       select="count(/BugCollection/BugInstance[@category=$category and @abbrev=$code and @type=$bug and @priority='4' and not(@last)])" />
   <div class='ib-2'>
      <div class='ib-2-t'>
         <a>
            <xsl:attribute name="href"></xsl:attribute>
            <xsl:attribute name="onclick">toggle('category-<xsl:value-of select="$category" />-and-code-<xsl:value-of select="$code" />-and-bug-<xsl:value-of select="$bug" />');return false;</xsl:attribute>
            <xsl:attribute name="onmouseout">popUp(event,'tip-<xsl:value-of select="$bug" />');</xsl:attribute>
            <xsl:attribute name="onmouseover">popUp(event,'tip-<xsl:value-of select="$bug" />');</xsl:attribute>
            <xsl:value-of select="/BugCollection/BugPattern[@category=$category and @abbrev=$code and @type=$bug]/ShortDescription" /><xsl:text disable-output-escaping="yes">&amp;nbsp;&amp;nbsp;</xsl:text>
            (<xsl:value-of select="$bug-count" />:
            <span class='t-h'><xsl:value-of select="$bug-count-p1" />/<xsl:value-of select="$bug-count-p2" />/<xsl:value-of select="$bug-count-p3" />/<xsl:value-of select="$bug-count-p4" /></span>)
         </a>
      </div>
      <div style="display:none;">
         <xsl:attribute name="id">category-<xsl:value-of select="$category" />-and-code-<xsl:value-of select="$code" />-and-bug-<xsl:value-of select="$bug" /></xsl:attribute>
         <xsl:variable name="cat-code-type">category-<xsl:value-of select="$category" />-and-code-<xsl:value-of select="$code" />-and-bug-<xsl:value-of select="$bug" /></xsl:variable>
         <xsl:variable name="bug-id">b-uid-<xsl:value-of select="@instanceHash" />-<xsl:value-of select="@instanceOccurrenceNum" /></xsl:variable>
         <xsl:for-each select="/BugCollection/BugInstance[@category=$category and @abbrev=$code and @type=$bug and not(@last)]">
            <xsl:call-template name="display-bug">
               <xsl:with-param name="b-t"     select="@type" />
               <xsl:with-param name="bug-id"       select="$bug-id" />
               <xsl:with-param name="which-list"   select="'c'" />
            </xsl:call-template>
         </xsl:for-each>
      </div>
   </div>
</xsl:template>

<!-- ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
<!-- main template for the list by package -->
<xsl:template name="list-by-package" >
   <div id='list-by-package' class='data-box' style='display:none;'>
      <xsl:call-template name="helpPriorities" />
      <xsl:for-each select="/BugCollection/FindBugsSummary/PackageStats[@total_bugs != '0']/@package">
         <xsl:sort select="." order="ascending" />
            <xsl:call-template name="packages">
               <xsl:with-param name="package" select="." />
            </xsl:call-template>
      </xsl:for-each>
   </div>
</xsl:template>

<xsl:template name="packages" >
   <xsl:param name="package" select="''" />
   <xsl:variable name="package-count-p1">
      <xsl:if test="not(/BugCollection/FindBugsSummary/PackageStats[@package=$package]/@priority_1 != '')">0</xsl:if>
      <xsl:if test="/BugCollection/FindBugsSummary/PackageStats[@package=$package]/@priority_1 != ''">
         <xsl:value-of select="/BugCollection/FindBugsSummary/PackageStats[@package=$package]/@priority_1" />
      </xsl:if>
   </xsl:variable>
   <xsl:variable name="package-count-p2">
      <xsl:if test="not(/BugCollection/FindBugsSummary/PackageStats[@package=$package]/@priority_2 != '')">0</xsl:if>
      <xsl:if test="/BugCollection/FindBugsSummary/PackageStats[@package=$package]/@priority_2 != ''">
         <xsl:value-of select="/BugCollection/FindBugsSummary/PackageStats[@package=$package]/@priority_2" />
      </xsl:if>
   </xsl:variable>
   <xsl:variable name="package-count-p3">
      <xsl:if test="not(/BugCollection/FindBugsSummary/PackageStats[@package=$package]/@priority_3 != '')">0</xsl:if>
      <xsl:if test="/BugCollection/FindBugsSummary/PackageStats[@package=$package]/@priority_3 != ''">
         <xsl:value-of select="/BugCollection/FindBugsSummary/PackageStats[@package=$package]/@priority_3" />
      </xsl:if>
   </xsl:variable>
   <xsl:variable name="package-count-p4">
      <xsl:if test="not(/BugCollection/FindBugsSummary/PackageStats[@package=$package]/@priority_4 != '')">0</xsl:if>
      <xsl:if test="/BugCollection/FindBugsSummary/PackageStats[@package=$package]/@priority_4 != ''">
         <xsl:value-of select="/BugCollection/FindBugsSummary/PackageStats[@package=$package]/@priority_4" />
      </xsl:if>
   </xsl:variable>

   <div class='ob'>
      <div class='ob-t'>
         <a>
            <xsl:attribute name="href"></xsl:attribute>
            <xsl:attribute name="onclick">toggle('package-<xsl:value-of select="$package" />');return false;</xsl:attribute>
            <xsl:value-of select="$package" />
            (<xsl:value-of select="/BugCollection/FindBugsSummary/PackageStats[@package=$package]/@total_bugs" />:
            <span class='t-h'><xsl:value-of select="$package-count-p1" />/<xsl:value-of select="$package-count-p2" />/<xsl:value-of select="$package-count-p3" />/<xsl:value-of select="$package-count-p4" /></span>)
         </a>
      </div>
      <div style="display:none;">
         <xsl:attribute name="id">package-<xsl:value-of select="$package" /></xsl:attribute>
         <xsl:call-template name="list-by-package-and-class">
            <xsl:with-param name="package" select="$package" />
         </xsl:call-template>
      </div>
   </div>
</xsl:template>

<xsl:template name="list-by-package-and-class" >
   <xsl:param name="package" select="''" />
   <xsl:for-each select="/BugCollection/FindBugsSummary/PackageStats[@package=$package]/ClassStats[@bugs != '0']/@class">
      <xsl:sort select="." order="ascending" />
         <xsl:call-template name="classes">
            <xsl:with-param name="package" select="$package" />
            <xsl:with-param name="class" select="." />
         </xsl:call-template>
   </xsl:for-each>
</xsl:template>

<xsl:template name="classes" >
   <xsl:param name="package" select="''" />
   <xsl:param name="class"     select="''" />
   <xsl:variable name="class-count"
                       select="/BugCollection/FindBugsSummary/PackageStats[@package=$package]/ClassStats[@class=$class and @bugs != '0']/@bugs" />

   <xsl:variable name="class-count-p1">
      <xsl:if test="not(/BugCollection/FindBugsSummary/PackageStats[@package=$package]/ClassStats[@class=$class and @bugs != '0']/@priority_1 != '')">0</xsl:if>
      <xsl:if test="/BugCollection/FindBugsSummary/PackageStats[@package=$package]/ClassStats[@class=$class and @bugs != '0']/@priority_1 != ''">
         <xsl:value-of select="/BugCollection/FindBugsSummary/PackageStats[@package=$package]/ClassStats[@class=$class and @bugs != '0']/@priority_1" />
      </xsl:if>
   </xsl:variable>
   <xsl:variable name="class-count-p2">
      <xsl:if test="not(/BugCollection/FindBugsSummary/PackageStats[@package=$package]/ClassStats[@class=$class and @bugs != '0']/@priority_2 != '')">0</xsl:if>
      <xsl:if test="/BugCollection/FindBugsSummary/PackageStats[@package=$package]/ClassStats[@class=$class and @bugs != '0']/@priority_2 != ''">
         <xsl:value-of select="/BugCollection/FindBugsSummary/PackageStats[@package=$package]/ClassStats[@class=$class and @bugs != '0']/@priority_2" />
      </xsl:if>
   </xsl:variable>
   <xsl:variable name="class-count-p3">
      <xsl:if test="not(/BugCollection/FindBugsSummary/PackageStats[@package=$package]/ClassStats[@class=$class and @bugs != '0']/@priority_3 != '')">0</xsl:if>
      <xsl:if test="/BugCollection/FindBugsSummary/PackageStats[@package=$package]/ClassStats[@class=$class and @bugs != '0']/@priority_3 != ''">
         <xsl:value-of select="/BugCollection/FindBugsSummary/PackageStats[@package=$package]/ClassStats[@class=$class and @bugs != '0']/@priority_3" />
      </xsl:if>
   </xsl:variable>
   <xsl:variable name="class-count-p4">
      <xsl:if test="not(/BugCollection/FindBugsSummary/PackageStats[@package=$package]/ClassStats[@class=$class and @bugs != '0']/@priority_4 != '')">0</xsl:if>
      <xsl:if test="/BugCollection/FindBugsSummary/PackageStats[@package=$package]/ClassStats[@class=$class and @bugs != '0']/@priority_4 != ''">
         <xsl:value-of select="/BugCollection/FindBugsSummary/PackageStats[@package=$package]/ClassStats[@class=$class and @bugs != '0']/@priority_4" />
      </xsl:if>
   </xsl:variable>

   <div class='ib-1'>
      <div class="ib-1-t">
         <a>
            <xsl:attribute name="href"></xsl:attribute>
            <xsl:attribute name="onclick">toggle('package-<xsl:value-of select="$package" />-and-class-<xsl:value-of select="$class" />');return false;</xsl:attribute>
            <xsl:value-of select="$class" />  (<xsl:value-of select="$class-count" />:
            <span class='t-h'><xsl:value-of select="$class-count-p1" />/<xsl:value-of select="$class-count-p2" />/<xsl:value-of select="$class-count-p3" />/<xsl:value-of select="$class-count-p4" /></span>)
         </a>
      </div>
      <div style="display:none;">
         <xsl:attribute name="id">package-<xsl:value-of select="$package" />-and-class-<xsl:value-of select="$class" /></xsl:attribute>
         <xsl:call-template name="list-by-package-and-class-and-bug">
            <xsl:with-param name="package" select="$package" />
            <xsl:with-param name="class" select="$class" />
         </xsl:call-template>
      </div>
   </div>
</xsl:template>

<xsl:template name="list-by-package-and-class-and-bug" >
   <xsl:param name="package" select="''" />
   <xsl:param name="class" select="''" />
   <xsl:variable name="unique-class-bugs" select="/BugCollection/BugInstance[not(@last) and Class[position()=1 and @classname=$class] and generate-id() = generate-id(key('lbp-class-b-t',concat(Class/@classname,@type)))]/@type" />

   <xsl:for-each select="$unique-class-bugs">
      <xsl:sort select="." order="ascending" />
         <xsl:call-template name="class-bugs">
            <xsl:with-param name="package" select="$package" />
            <xsl:with-param name="class" select="$class" />
            <xsl:with-param name="type" select="." />
         </xsl:call-template>
   </xsl:for-each>
</xsl:template>

<xsl:template name="class-bugs" >
   <xsl:param name="package" select="''" />
   <xsl:param name="class"     select="''" />
   <xsl:param name="type"      select="''" />
   <xsl:variable name="bug-count"
                       select="count(/BugCollection/BugInstance[@type=$type and not(@last) and Class[position()=1 and @classname=$class]])" />
   <div class='ib-2'>
      <div class='ib-2-t'>
         <a>
            <xsl:attribute name="href"></xsl:attribute>
            <xsl:attribute name="onclick">toggle('package-<xsl:value-of select="$package" />-and-class-<xsl:value-of select="$class" />-and-type-<xsl:value-of select="$type" />');return false;</xsl:attribute>
            <xsl:attribute name="onmouseout">popUp(event,'tip-<xsl:value-of select="$type" />')</xsl:attribute>
            <xsl:attribute name="onmouseover">popUp(event,'tip-<xsl:value-of select="$type" />')</xsl:attribute>
            <xsl:value-of select="/BugCollection/BugPattern[@type=$type]/ShortDescription" /><xsl:text disable-output-escaping="yes">&amp;nbsp;&amp;nbsp;</xsl:text>
            (<xsl:value-of select="$bug-count" />)
         </a>
      </div>
      <div style="display:none;">
         <xsl:attribute name="id">package-<xsl:value-of select="$package" />-and-class-<xsl:value-of select="$class" />-and-type-<xsl:value-of select="$type" /></xsl:attribute>
         <xsl:variable name="package-class-type">package-<xsl:value-of select="$package" />-and-class-<xsl:value-of select="$class" />-and-type-<xsl:value-of select="$type" /></xsl:variable>
         <xsl:variable name="bug-id">b-uid-<xsl:value-of select="@instanceHash" />-<xsl:value-of select="@instanceOccurrenceNum" /></xsl:variable>
         <xsl:for-each select="/BugCollection/BugInstance[@type=$type and not(@last) and Class[position()=1 and @classname=$class]]">
            <xsl:call-template name="display-bug">
               <xsl:with-param name="b-t"     select="@type" />
               <xsl:with-param name="bug-id"       select="$bug-id" />
               <xsl:with-param name="which-list"   select="'p'" />
            </xsl:call-template>
         </xsl:for-each>
      </div>
   </div>
</xsl:template>

</xsl:transform>
