<?xml version="1.0" encoding="UTF-8" ?>
<!--
  Copyright (C) 2005, 2006 Etienne Giraudy, InStranet Inc
  Copyright (C) 2005, 2007 Etienne Giraudy

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

<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" >
   <xsl:output
         method="xml" indent="yes"
         doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
         doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
         encoding="UTF-8"/>

   <xsl:variable name="apos" select="&quot;'&quot;"/>
   <xsl:key name="lbc-code-key"        match="/BugCollection/BugInstance" use="concat(@category,@abbrev)" />
   <xsl:key name="lbc-bug-key"         match="/BugCollection/BugInstance" use="concat(@category,@abbrev,@type)" />
   <xsl:key name="lbp-class-b-t"  match="/BugCollection/BugInstance" use="concat(Class/@classname,@type)" />

<xsl:template match="/" >

<html>
   <head>
      <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
      <title>
         FindBugs (<xsl:value-of select="/BugCollection/@version" />) 
         Analysis for 
         <xsl:choose>
            <xsl:when test='string-length(/BugCollection/Project/@projectName)>0'><xsl:value-of select="/BugCollection/Project/@projectName" /></xsl:when>
            <xsl:otherwise><xsl:value-of select="/BugCollection/Project/@filename" /></xsl:otherwise>
         </xsl:choose>
      </title>
      <style type="text/css">
         html, body, div, form {
            margin:0px;
            padding:0px;
         }
         body {
            padding:3px;
         }
         a, a:link , a:active, a:visited, a:hover {
            text-decoration: none; color: black;
         }
         #navlist {
                 padding: 3px 0;
                 margin-left: 0;
                 border-bottom: 1px solid #778;
                 font: bold 12px Verdana, sans-serif;
         }
         #navlist li {
                 list-style: none;
                 margin: 0;
                 display: inline;
         }
         #navlist li a {
                 padding: 3px 0.5em;
                 margin-left: 3px;
                 border: 1px solid #778;
                 border-bottom: none;
                 background: #DDE;
                 text-decoration: none;
         }
         #navlist li a:link { color: #448; }
         #navlist li a:visited { color: #667; }
         #navlist li a:hover {
                 color: #000;
                 background: #AAE;
                 border-color: #227;
         }
         #navlist li a.current {
                 background: white;
                 border-bottom: 1px solid white;
         }
         #filterWrapper {
            margin-bottom:5px;
         }
         #displayWrapper {
            margin-top:5px;
         }
         .message {
            background:#BBBBBB;
           border: 1px solid #778;
         }
         .displayContainer {
            border:1px solid #555555;
            margin-top:3px;
            padding: 3px;
            display:none;
         }
         #summaryContainer table,
         #historyContainer table {
            border:1px solid black;
         }
         #summaryContainer th,
         #historyContainer th {
            background: #aaaaaa;
            color: white;
         }
         #summaryContainer th, #summaryContainer td,
         #historyContainer th, #historyContainer td {
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

         .bugList-level1 {
            margin-bottom:5px;
         }
         .bugList-level1, .bugList-level2, .bugList-level3, .bugList-level4 {
            background-color: #ffffff;
            margin-left:15px;
            padding-left:10px;
         }
         .bugList-level1-label, .bugList-level2-label, .bugList-level3-label, .bugList-level4-label {
            background-color: #bbbbbb;
            border: 1px solid black;
            padding: 1px 3px 1px 3px;;
         }
         .bugList-level2-label, .bugList-level3-label, .bugList-level4-label {
            border-width: 0px 1px 1px 1px;
         }
         .bugList-level4-label {
            background-color: #ffffff;
            border: 0px 0px 1px 0px;
         }
         .bugList-level4 {
            border: 0px 1px 1px 1px;
         }

         .bugList-level4-inner {
            border-style: solid;
            border-color: black;
            border-width: 0px 1px 1px 1px;
         }
         .b-r {
            font-size: 10pt; font-weight: bold; padding: 0 0 0 60px;
         }
         .b-d {
            font-weight: normal; background: #ccccc0;
            padding: 0 5px 0 5px; margin: 0px;
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

      </style>
      <script type='text/javascript'><xsl:text disable-output-escaping='yes'><![CDATA[
         var menus            = new Array('summary','info','history','listByCategories','listByPackages');
         var selectedMenuId   = "summary";
         var selectedVersion  = -1;
         var selectedPriority = 4;
         var lastVersion      = 0;
         var includeFixedIntroducedBugs;

         var bPackageNamesPopulated = false;

         var filterContainerId              = "filterWrapper";
         var historyControlContainerId      = "historyControlWrapper";
         var messageContainerId             = "messageContainer";
         var summaryContainerId             = "summaryContainer";
         var infoContainerId                = "infoContainer";
         var historyContainerId             = "historyContainer";
         var listByCategoriesContainerId    = "listByCategoriesContainer";
         var listByPackagesContainerId      = "listByPackagesContainer";

         var idxCatKey = 0; var idxCatDescr = 1; var idxBugCat = 1;
         var idxCodeKey = 0; var idxCodeDescr = 1; var idxBugCode = 2;
         var idxPatternKey = 2; var idxPatternDescr = 3; var idxBugPattern = 3;
         var idxBugKey = 0; var idxBugDescr = 6;
         var idxBugClass = 6, idxBugPackage = 7;

         // main init function
         function init() {
            loadFilter();
            selectMenu(selectedMenuId);
            lastVersion = versions.length - 1;
         }

         // menu callback function
         function selectMenu(menuId) {
            document.getElementById(selectedMenuId).className="none";
            document.getElementById(menuId).className="current";
            if (menuId!=selectedMenuId) {
               hideMenu(selectedMenuId);
               selectedMenuId = menuId;
            }
            if (menuId=="summary")           displaySummary();
            if (menuId=="info")              displayInfo();
            if (menuId=="history")           displayHistory();
            if (menuId=="listByCategories")  displayListByCategories();
            if (menuId=="listByPackages")    displayListByPackages();
         }

         // display filter
         function loadFilter() {
            var versionsBox = document.findbugsForm.versions.options;
            versionsBox[0] = new Option(" -- All Versions -- ","-1");
            versionsBox.selectedIndex = 0;
            if (versions.length>=1) {
               for (x=0; versions.length>1 && x<versions.length; x++) {
                  versionsBox[x+1] = new Option(" Bugs at release: "+versions[versions.length-x-1][1], versions[versions.length-x-1][0]);
               }
            }

            var prioritiesBox = document.findbugsForm.priorities.options;
            prioritiesBox[0] = new Option(" -- All priorities -- ", "4");
            prioritiesBox[1] = new Option(" P1 bugs ", "1");
            prioritiesBox[2] = new Option(" P1 and P2 bugs ", "2");
            prioritiesBox[3] = new Option(" P1, P2 and P3 bugs ", "3");
         }

         // display a message
         function displayMessage(msg) {
            var container = document.getElementById(messageContainerId);
            container.innerHTML = "<div class='message'>"+msg+"</div>";
         }

         // reset displayed message
         function resetMessage() {
            var container = document.getElementById(messageContainerId);
            container.innerHTML = "";
         }

         function hideMenu(menuId) {
            var container = menuId+"Container";
            document.getElementById(container).style.display="none";
         }

         // filter callback function
         function filter() {
            var versionsBox = document.findbugsForm.versions.options;
            selectedVersion = versionsBox[versionsBox.selectedIndex].value;

            var prioritiesBox = document.findbugsForm.priorities.options;
            selectedPriority = prioritiesBox[prioritiesBox.selectedIndex].value;

            selectMenu(selectedMenuId);
         }

         // includeFixedBugs callback function
         function includeFixedIntroducedBugsInHistory() {
            includeFixedIntroducedBugs =
              document.findbugsHistoryControlForm.includeFixedIntroducedBugs.checked;

            selectMenu(selectedMenuId);
         }

         // display summary tab
         function displaySummary() {
            resetMessage();
            hide(filterContainerId);
            hide(historyControlContainerId);
            var container = document.getElementById(summaryContainerId);
            container.style.display="block";
         }

         // display info tab
         function displayInfo() {
            resetMessage();
            hide(filterContainerId);
            hide(historyControlContainerId);
            var container = document.getElementById(infoContainerId);
            container.style.display="block";
         }

         // display history tab
         function displayHistory() {
            displayMessage("Loading history...");
            hide(filterContainerId);
            show(historyControlContainerId);
            var container = document.getElementById(historyContainerId);
            var content = "";
            var i=0;
            var p = [0,0,0,0,0];
            var f = [0,0,0,0,0];

            content += "<table><tr><th>Release</th><th>Bugs</th><th>Bugs p1</th><th>Bugs p2</th><th>Bugs p3</th><th>Bugs Exp.</th></tr>";

            var aSpan   = "<span title='Bugs introduced in this release that have not been fixed.'>";
            var fSpan   = "<span title='Bugs fixed in this release.'>";
            var fiSpan  = "<span title='Bugs introduced in this release that were fixed in later releases.'>";
            var afiSpan = "<span title='Total number of bugs introduced in this release.'>";
            var eSpan   = "</span>";

            if(includeFixedIntroducedBugs) {
                for (i=(versions.length-1); i>0; i--) {
                    v = countBugsVersion(i, 4);
                    t = countTotalBugsVersion(i);
                    o = countFixedButActiveBugsVersion(i);
                    f = countFixedBugsInVersion(i);
                    fi = countFixedBugsIntroducedInVersion(i);
                    content += "<tr>";
                    content += "<td class='summary-name'>" + versions[i][1] + "</td>";
                    content += "<td class='summary-priority-all'> " + (t[0] + o[0]) + " (+" + afiSpan + (v[0] + fi[0]) + eSpan +
                      " [" + aSpan + v[0] + eSpan + " / " + fiSpan + fi[0] + eSpan + "] " + eSpan + " / -" + fSpan + f[0] + eSpan + ") </td>";
                    content += "<td class='summary-priority-1'> " + (t[1] + o[1]) + " (+" + afiSpan + (v[1] + fi[1]) + eSpan +
                      " [" + aSpan + v[1] + eSpan + " / " + fiSpan + fi[1] + eSpan + "] " + eSpan + " / -" + fSpan + f[1] + eSpan + ") </td>";
                    content += "<td class='summary-priority-2'> " + (t[2] + o[2]) + " (+" + afiSpan + (v[2] + fi[2]) + eSpan +
                      " [" + aSpan + v[2] + eSpan + " / " + fiSpan + fi[2] + eSpan + "] " + eSpan + " / -" + fSpan + f[2] + eSpan + ") </td>";
                    content += "<td class='summary-priority-3'> " + (t[3] + o[3]) + " (+" + afiSpan + (v[3] + fi[3]) + eSpan +
                      " [" + aSpan + v[3] + eSpan + " / " + fiSpan + fi[3] + eSpan + "] " + eSpan + " / -" + fSpan + f[3] + eSpan + ") </td>";
                    content += "<td class='summary-priority-4'> " + (t[4] + o[4]) + " (+" + afiSpan + (v[4] + fi[4]) + eSpan +
                      " [" + aSpan + v[4] + eSpan + " / " + fiSpan + fi[4] + eSpan + "] " + eSpan + " / -" + fSpan + f[4] + eSpan + ") </td>";
                    content += "</tr>";
                }
            } else {
                for (i=(versions.length-1); i>0; i--) {
                    v = countBugsVersion(i, 4);
                    t = countTotalBugsVersion(i);
                    o = countFixedButActiveBugsVersion(i);
                    f = countFixedBugsInVersion(i);
                    content += "<tr>";
                    content += "<td class='summary-name'>" + versions[i][1] + "</td>";
                    content += "<td class='summary-priority-all'> " + (t[0] + o[0]) + " (+" + aSpan + v[0] + eSpan + " / -" + fSpan + f[0] + eSpan + ") </td>";
                    content += "<td class='summary-priority-1'  > " + (t[1] + o[1]) + " (+" + aSpan + v[1] + eSpan + " / -" + fSpan + f[1] + eSpan + ") </td>";
                    content += "<td class='summary-priority-2'  > " + (t[2] + o[2]) + " (+" + aSpan + v[2] + eSpan + " / -" + fSpan + f[2] + eSpan + ") </td>";
                    content += "<td class='summary-priority-3'  > " + (t[3] + o[3]) + " (+" + aSpan + v[3] + eSpan + " / -" + fSpan + f[3] + eSpan + ") </td>";
                    content += "<td class='summary-priority-4'  > " + (t[4] + o[4]) + " (+" + aSpan + v[4] + eSpan + " / -" + fSpan + f[4] + eSpan + ") </td>";
                    content += "</tr>";
                }
            }

            t = countTotalBugsVersion(0);
            o = countFixedButActiveBugsVersion(0);
            content += "<tr>";
            content += "<td class='summary-name'>" + versions[0][1] + "</td>";
            content += "<td class='summary-priority-all'> " + (t[0] + o[0]) + " </td>";
            content += "<td class='summary-priority-1'  > " + (t[1] + o[1]) + " </td>";
            content += "<td class='summary-priority-2'  > " + (t[2] + o[2]) + " </td>";
            content += "<td class='summary-priority-3'  > " + (t[3] + o[3]) + " </td>";
            content += "<td class='summary-priority-4'  > " + (t[4] + o[4]) + " </td>";
            content += "</tr>";

            content += "</table>";
            container.innerHTML = content;
            container.style.display="block";
            resetMessage();
         }

         // display list by cat tab
         function displayListByCategories() {
            hide(historyControlContainerId);
            show(filterContainerId);
            var container = document.getElementById(listByCategoriesContainerId);
            container.innerHTML = "";
            container.style.display="block";
            displayMessage("Loading stats (categories)...");
            container.innerHTML = displayLevel1("lbc", "Stats by Bug Categories");
            resetMessage();
         }

         // display list by package tab
         function displayListByPackages() {
            hide(historyControlContainerId);
            show(filterContainerId);
            var container = document.getElementById(listByPackagesContainerId);
            container.style.display="block";
            if (!bPackageNamesPopulated) {
               displayMessage("Initializing...");
               populatePackageNames();
            }
            displayMessage("Loading stats (packages)...");
            container.innerHTML = displayLevel1("lbp", "Stats by Bug Package");
            resetMessage();
         }

         // callback function for list item click
         function toggleList(listType, containerId, id1, id2, id3) {
            var container = document.getElementById(containerId);
            if (container.style.display=="block") {
               container.style.display="none";
            } else {
               if (listType=="lbc") {
                  if (id1.length>0 && id2.length==0 && id3.length==0) {
                     displayCategoriesCodes(containerId, id1);
                  } else if (id1.length>0 && id2.length>0 && id3.length==0) {
                     displayCategoriesCodesPatterns(containerId, id1, id2);
                  } else if (id1.length>0 && id2.length>0 && id3.length>0) {
                     displayCategoriesCodesPatternsBugs(containerId, id1, id2, id3);
                  } else {
                     // ???
                  }
               } else if (listType=="lbp") {
                  if (id1.length>0 && id2.length==0 && id3.length==0) {
                     displayPackageCodes(containerId, id1);
                  } else if (id1.length>0 && id2.length>0 && id3.length==0) {
                     displayPackageClassPatterns(containerId, id1, id2);
                  } else if (id1.length>0 && id2.length>0 && id3.length>0) {
                     displayPackageClassPatternsBugs(containerId, id1, id2, id3);
                  } else {
                     // ???
                  }
               } else {
                  // ????
               }
            }
         }

         // list by categories, display bug cat>codes
         function displayCategoriesCodes(containerId, catId) {
            displayMessage("Loading stats (codes)...");
            var container = document.getElementById(containerId);
            container.style.display="block";
            if (container.innerHTML=="Loading..." || container.innerHTML=="") {
               container.innerHTML = displayLevel2("lbc", catId);
            }
            resetMessage();
         }

         // list by categories, display bug package>codes
         function displayPackageCodes(containerId, packageId) {
            displayMessage("Loading stats (codes)...");
            var container = document.getElementById(containerId);
            container.style.display="block";
            if (container.innerHTML=="Loading..." || container.innerHTML=="") {
               container.innerHTML = displayLevel2("lbp", packageId);
            }
            resetMessage();
         }

         // list by categories, display bug cat>codes>patterns
         function displayCategoriesCodesPatterns(containerId, catId, codeId) {
            displayMessage("Loading stats (patterns)...");
            var container = document.getElementById(containerId);
            container.style.display="block";
            if (container.innerHTML=="Loading..." || container.innerHTML=="")
               container.innerHTML = displayLevel3("lbc", catId, codeId);
            resetMessage();
         }

         // list by package, display bug package>class>patterns
         function displayPackageClassPatterns(containerId, packageId, classId) {
            displayMessage("Loading stats (patterns)...");
            var container = document.getElementById(containerId);
            container.style.display="block";
            if (container.innerHTML=="Loading..." || container.innerHTML=="")
               container.innerHTML = displayLevel3("lbp", packageId, classId);
            resetMessage();
         }

         // list by categories, display bug cat>codes>patterns>bugs
         function displayCategoriesCodesPatternsBugs(containerId, catId, codeId, patternId) {
            displayMessage("Loading stats (bugs)...");
            var container = document.getElementById(containerId);
            container.style.display="block";
            if (container.innerHTML=="Loading..." || container.innerHTML=="")
               container.innerHTML = displayLevel4("lbc", catId, codeId, patternId);
            resetMessage();
         }

         // list by package, display bug package>class>patterns>bugs
         function displayPackageClassPatternsBugs(containerId, packageId, classId, patternId) {
            displayMessage("Loading stats (bugs)...");
            var container = document.getElementById(containerId);
            container.style.display="block";
            if (container.innerHTML=="Loading..." || container.innerHTML=="")
               container.innerHTML = displayLevel4("lbp",  packageId, classId, patternId);
            resetMessage();
         }

         // generate level 1 list
         function displayLevel1(list, title) {
            var content = "";
            var content2 = "";

            content += "<h3>"+title+"</h3>";
            content += getPriorityLegend();
            content2 += "<div class='bugList'>";

            var id = "";
            var containerId = "";
            var subContainerId = "";
            var prefixSub = "";
            var prefixId = "";
            var p = [0,0,0,0,0];
            var numberOfBugs = 0;
            var label = "";
            var max = 0;
            if (list=="lbc") {
               max = categories.length;
            } else if (list=="lbp") {
               max = packageStats.length;
            }

            for (var x=0; x<max -1; x++) {
               if (list=="lbp" && packageStats[x][1]=="0") continue;

               if (list=="lbc") {
                  id = categories[x][idxCatKey];
                  label = categories[x][idxCatDescr];
                  containerId = "categories-" + id;
                  subContainerId = "cat-"+id;
                  p = countBugsCat(selectedVersion, selectedPriority, id, idxBugCat);
               }
               if (list=="lbp") {
                  id = packageStats[x][0];
                  label = packageStats[x][0];
                  containerId = "packages-" + id;
                  subContainerId = "package-"+id;
                  p = countBugsPackage(selectedVersion, selectedPriority, id, idxBugPackage);
               }

               subContainerId = prefixSub+id;

               var total = p[1]+p[2]+p[3]+p[4];
               if (total > 0) {
                  content2 += addListItem( 1, containerId, label, total, p, subContainerId,
                                          "toggleList('" + list + "', '" + subContainerId + "', '"+ id + "', '', '')"
                                          );
               }
               numberOfBugs += total;
            }
            content2 += "</div>";
            content += "<h4>Total number of bugs";
            if (selectedVersion!=-1) {
               content += " (introduced in release " + versions[selectedVersion][1] +")";
            }
            content += ": "+numberOfBugs+"</h4>";
            return content+content2;
         }

         // generate level 2 list
        function displayLevel2(list, id1) {
            var content = "";
            var code = "";
            var containerId = "";
            var subContainerId = "";
            var p = [0,0,0,0,0];
            var max = 0;
            var id2 = "";
            if (list=="lbc") {
               max = codes.length;
            } else if (list=="lbp") {
               max = classStats.length;
            }

            for (var x=0; x<max -1; x++) {
               if (list=="lbp" && classStats[x][3]=="0") continue;

               if (list=="lbc") {
                  id2 = codes[x][idxCodeKey];
                  label = codes[x][idxCodeDescr];
                  containerId = "codes-"+id1;
                  subContainerId = "cat-" + id1 + "-code-" + id2;
                  p = countBugsCode(selectedVersion, selectedPriority, id1, idxBugCat, id2, idxBugCode);
               }
               if (list=="lbp") {
                  id2 = classStats[x][0];
                  label = classStats[x][0];
                  containerId = "packages-"+id1;
                  subContainerId = "package-" + id1 + "-class-" + id2;
                  p = countBugsClass(selectedVersion, selectedPriority, id1, idxBugPackage, id2, idxBugClass);
               }

               var total = p[1]+p[2]+p[3]+p[4];
               if (total > 0) {
                  content += addListItem( 2, containerId, label, total, p, subContainerId,
                                          "toggleList('"+ list + "', '" + subContainerId + "', '"+ id1 + "', '"+ id2 + "', '')"
                                          );
               }
            }
            return content;
         }

         // generate level 3 list
        function displayLevel3(list, id1, id2) {
            var content = "";
            var containerId = "";
            var subContainerId = "";
            var p = [0,0,0,0,0];
            var max = 0;
            var label = "";
            var id3 = "";

            if (list=="lbc") {
               max = patterns.length;
            } else if (list=="lbp") {
               max = patterns.length;
            }

            for (var x=0; x<max -1; x++) {
               //if (list=="lbp" && (patterns[x][0]!=id1 || patterns[x][1]!=id2)) continue;
               //if (list=="lbp" && classStats[x][3]=="0") continue;

               if (list=="lbc") {
                  id3 = patterns[x][idxPatternKey];;
                  label = patterns[x][idxPatternDescr];
                  containerId = "patterns-"+id1;
                  subContainerId = "cat-" + id1 + "-code-" + id2 + "-pattern-" + id3;
                  p = countBugsPattern(selectedVersion, selectedPriority, id1, idxBugCat, id2, idxBugCode, id3, idxBugPattern);
               }
               if (list=="lbp") {
                  id3 = patterns[x][idxPatternKey];;
                  label = patterns[x][idxPatternDescr];
                  containerId = "classpatterns-"+id1;
                  subContainerId = "package-" + id1 + "-class-" + id2 + "-pattern-" + id3;
                  p = countBugsClassPattern(selectedVersion, selectedPriority, id2, idxBugClass, id3, idxBugPattern);
               }

               var total = p[1]+p[2]+p[3]+p[4];
               if (total > 0) {
                  content += addListItem( 3, containerId, label, total, p, subContainerId,
                                          "toggleList('" + list + "', '" + subContainerId + "', '"+ id1 + "', '"+ id2 + "', '"+ id3 + "')"
                                          );
               }
            }
            return content;
         }

         // generate level 4 list
        function displayLevel4(list, id1, id2, id3) {
            var content = "";
            var bug = "";
            var bugP = 0;
            var containerId = "";
            var subContainerId = "";
            var bugId = "";
            var label = "";
            var p = [0,0,0,0,0];
            for (var x=0; x<bugs.length -1; x++) {
               bug = bugs[x];
               if (list=="lbc") {
                  if ( bug[1]!=id1 || bug[2]!=id2 || bug[3]!=id3 ) continue;
                  if ( selectedVersion!=-1
                     && selectedVersion!=bug[5]) continue;
                  if ( selectedPriority!=4
                     && selectedPriority<bug[4]) continue;

                  subContainerId = "cat-" + id1 + "-code-" + id2 + "-pattern-" + id3 + "-bug-" + bug[0];
               }
               if (list=="lbp") {
                  if ( bug[7]!=id1 || bug[6]!=id2 || bug[3]!=id3 ) continue;
                  if ( selectedVersion!=-1
                     && selectedVersion!=bug[5]) continue;
                  if ( selectedPriority!=4
                     && selectedPriority<bug[4]) continue;

                  subContainerId = "package-" + id1 + "-class-" + id2 + "-pattern-" + id3 + "-bug-" + bug[0];
               }

               bugId = "b-uid-" + bug[0];
               label = bug[idxBugDescr];
               containerId = "bugs-"+bugId;
               bugP = bug[4];
               p[bugP]++;
               var total = p[1]+p[2]+p[3]+p[4];
               if (total > 0) {
                  content += addBug(   4, containerId, label, bugP, bug[5], subContainerId,
                                       "showbug('" + bugId + "', '" + subContainerId + "', '"+id3+"')");
               }
            }
            return content;
         }


         function addListItem(level, id, label, total, p, subId, onclick) {
            var content = "";

            content += "<div class='bugList-level"+level+"' >";
            content += "<div class='bugList-level"+level+"-label' id='"+id+"' >";
            content += "<a href='' onclick=\"" + onclick + ";return false;\" ";
            content += ">";
            content += "<strong>"+label+"</strong>";
            content += " "+total+" bugs";
            if (selectedPriority>1)
               content += " <em>("+p[1];
            if (selectedPriority>=2)
               content += "/"+p[2];
            if (selectedPriority>=3)
               content += "/"+p[3];
            if (selectedPriority>=4)
               content += "/"+p[4];
            if (selectedPriority>1)
               content += ")</em>";
            content += "</a>";
            content += "</div>";
            content += "<div class='bugList-level"+level+"-inner' id='"+subId+"' style='display:none;'>Loading...</div>";
            content += "</div>";
            return content;
         }

         function addBug( level, id, label, p, version, subId, onclick) {
            var content = "";

            content += "<div class='bugList-level" + level + "' id='" + id + "'>";
            content += "<div class='bugList-level" + level + "-label' id='" + id + "'>";
            content += "<span class='b-" + p + "'>&nbsp;&nbsp;&nbsp;</span>";
            content += "<a href='' onclick=\"" + onclick + ";return false;\">";
            if (version==lastVersion) {
               content += "<span style='color:red;font-weight:bold;'>NEW!</span> ";
            }
            content += "<strong>" + label + "</strong>";
            if (version==0) {
               content += " <em>since release first historized release</em>";
            } else {
               content += " <em>since release " + versions[version][1] + "</em>";
            }
            content += "</a>";
            content += "</div>";
            content += "<div class='bugList-level" + level + "-inner' id='" + subId + "' style='display:none;'>Loading...</div>";
            content += "</div>";
            return content;
         }

         function countBugsVersion(version, priority) {
            return countBugs(version, priority, "", -1, "", -1, "", -1, "", -1, "", -1);
         }

         function countBugsCat(version, priority, cat, idxCat) {
            return countBugs(version, priority, cat, idxCat, "", -1, "", -1, "", -1, "", -1);
         }

         function countBugsPackage(version, priority, packageId, idxPackage) {
            return countBugs(version, priority, "", -1, "", -1, "", -1, packageId, idxPackage, "", -1);
         }

         function countBugsCode(version, priority, cat, idxCat, code, idxCode) {
            return countBugs(version, priority, cat, idxCat, code, idxCode, "", -1, "", -1, "", -1);
         }

         function countBugsPattern(version, priority, cat, idxCat, code, idxCode, packageId, idxPattern) {
            return countBugs(version, priority, cat, idxCat, code, idxCode, packageId, idxPattern, "", -1, "", -1);
         }

         function countBugsClass(version, priority, id1, idxBugPackage, id2, idxBugClass) {
            return countBugs(version, priority, "", -1, "", -1, "", -1, id1, idxBugPackage, id2, idxBugClass);
         }

         function countBugsClassPattern(version, priority, id2, idxBugClass, id3, idxBugPattern) {
            return countBugs(version, priority, "", -1, "", -1, id3, idxBugPattern, "", -1, id2, idxBugClass);
         }

         function countBugs(version, priority, cat, idxCat, code, idxCode, pattern, idxPattern, packageId, idxPackage, classId, idxClass) {
            var count = [0,0,0,0,0];
            var last=1000000;
            for (var x=0; x<bugs.length-1; x++) {
               var bug = bugs[x];

               var bugCat = bug[idxCat];
               var bugP = bug[4];
               var bugCode = bug[idxCode];
               var bugPattern = bug[idxPattern];

               if (     (version==-1    || version==bug[5])
                     && (priority==4    || priority>=bug[4])
                     && (idxCat==-1     || bug[idxCat]==cat)
                     && (idxCode==-1    || bug[idxCode]==code)
                     && (idxPattern==-1 || bug[idxPattern]==pattern)
                     && (idxPackage==-1 || bug[idxPackage]==packageId)
                     && (idxClass==-1   || bug[idxClass]==classId)
                     ) {
                  count[bug[4]]++;
               }
            }
            count[0] = count[1] + count[2] + count[3] + count[4];
            return count;
         }

         function countFixedBugsInVersion(version) {
            var count = [0,0,0,0,0];
            var last=1000000;
            for (var x=0; x<fixedBugs.length-1; x++) {
               var bug = fixedBugs[x];

               var bugP = bug[4];

               if ( version==-1 || version==(bug[6]+1)) {
                  count[bug[4]]++;
               }
            }
            count[0] = count[1] + count[2] + count[3] + count[4];
            return count;
         }

         function countFixedBugsIntroducedInVersion(version) {
            var count = [0,0,0,0,0];
            var last=1000000;
            for (var x=0; x<fixedBugs.length-1; x++) {
               var bug = fixedBugs[x];

               var bugP = bug[4];

               if ( version==-1 || version==(bug[5])) {
                  count[bug[4]]++;
               }
            }
            count[0] = count[1] + count[2] + count[3] + count[4];
            return count;
         }

         function countFixedButActiveBugsVersion(version) {
            var count = [0,0,0,0,0];
            var last=1000000;
            for (var x=0; x<fixedBugs.length-1; x++) {
               var bug = fixedBugs[x];

               var bugP = bug[4];

               if ( version==-1 || (version >=bug[5] && version<=bug[6]) ) {
                  count[bug[4]]++;
               }
            }
            count[0] = count[1] + count[2] + count[3] + count[4];
            return count;
         }

         function countTotalBugsVersion(version) {
            var count = [0,0,0,0,0];
            var last=1000000;
            for (var x=0; x<bugs.length-1; x++) {
               var bug = bugs[x];

               var bugP = bug[4];

               if (version==-1 || version>=bug[5]) {
                  count[bug[4]]++;
               }
            }
            count[0] = count[1] + count[2] + count[3] + count[4];
            return count;
         }

         function getPriorityLegend() {
            var content = "";
            content += "<h5><span class='b-1'>&nbsp;&nbsp;&nbsp;</span> P1 ";
            content += "<span class='b-2'>&nbsp;&nbsp;&nbsp;</span> P2 ";
            content += "<span class='b-3'>&nbsp;&nbsp;&nbsp;</span> P3 ";
            content += "<span class='b-4'>&nbsp;&nbsp;&nbsp;</span> Exp ";
            content += "</h5>";
            return content;
         }

         function populatePackageNames() {
            for (var i=0; i<bugs.length; i++) {
               var classId = bugs[i][6];
               var idx = classId.lastIndexOf('.');
               var packageId = "";

               if (idx>0) {
                  packageId = classId.substring(0, idx);
               }

               bugs[i][7] = packageId;
            }
         }

         function showbug(bugId, containerId, patternId) {
            var bugplaceholder   = document.getElementById(containerId);
            var bug              = document.getElementById(bugId);

            if ( bugplaceholder==null) {
               alert(buguid+'-ph-'+list+' - '+buguid+' - bugplaceholder==null');
               return;
            }
            if ( bug==null) {
               alert(buguid+'-ph-'+list+' - '+buguid+' - bug==null');
               return;
            }

            var newBug = bug.innerHTML;
            var pattern = document.getElementById('tip-'+patternId).innerHTML;
            toggle(containerId);
            bugplaceholder.innerHTML = newBug + pattern;
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
         function show(foo) {
            document.getElementById(foo).style.display="block";
         }
         function hide(foo) {
            document.getElementById(foo).style.display="none";
         }

         window.onload = function(){
            init();
         };
      ]]></xsl:text></script>
      <script type='text/javascript'>
         // versions fields: release id, release label
         var versions = new Array(
            <xsl:for-each select="/BugCollection/History/AppVersion">
               [ "<xsl:value-of select="@sequence" />", "<xsl:value-of select="@release" />" ],
            </xsl:for-each>
               [ "<xsl:value-of select="/BugCollection/@sequence" />", "<xsl:value-of select="/BugCollection/@release" />" ]
            );

         // categories fields: category id, category label
         var categories = new Array(
            <xsl:for-each select="/BugCollection/BugCategory">
               <xsl:sort select="@category" order="ascending" />
               [ "<xsl:value-of select="@category" />", "<xsl:value-of select="Description" />" ],
            </xsl:for-each>
               [ "", "" ]
            );

         // codes fields: code id, code label
         var codes = new Array(
            <xsl:for-each select="/BugCollection/BugCode">
               <xsl:sort select="@abbrev" order="ascending" />
               [ "<xsl:value-of select="@abbrev" />", "<xsl:value-of select="Description" />" ],
            </xsl:for-each>
               [ "", "" ]
            );

         // patterns fields: category id, code id, pattern id, pattern label
         var patterns = new Array(
            <xsl:for-each select="/BugCollection/BugPattern">
               <xsl:sort select="@type" order="ascending" />
               [ "<xsl:value-of select="@category" />", "<xsl:value-of select="@abbrev" />", "<xsl:value-of select="@type" />", "<xsl:value-of select="translate(ShortDescription, '&quot;', $apos)" />" ],

            </xsl:for-each>
               [ "", "", "", "" ]
            );

         // class stats fields: class name, package name, isInterface, total bugs, bugs p1, bugs p2, bugs p3, bugs p4
         var classStats = new Array(
            <xsl:for-each select="/BugCollection/FindBugsSummary/PackageStats/ClassStats">
               <xsl:sort select="@class" order="ascending" />
               [ "<xsl:value-of select="@class" />", "<xsl:value-of select="../@package" />", "<xsl:value-of select="@interface" />", "<xsl:value-of select="@bugs" />", "<xsl:value-of select="@priority_1" />", "<xsl:value-of select="@priority_2" />", "<xsl:value-of select="@priority_3" />", "<xsl:value-of select="@priority_4" />" ],
            </xsl:for-each>
               [ "", "", "", "", "", "", "", "" ]
            );

         // package stats fields: package name, total bugs, bugs p1, bugs p2, bugs p3, bugs p4
         var packageStats = new Array(
            <xsl:for-each select="/BugCollection/FindBugsSummary/PackageStats">
               <xsl:sort select="@package" order="ascending" />
               [ "<xsl:value-of select="@package" />", "<xsl:value-of select="@total_bugs" />", "<xsl:value-of select="@priority_1" />", "<xsl:value-of select="@priority_2" />", "<xsl:value-of select="@priority_3" />", "<xsl:value-of select="@priority_4" />" ],
            </xsl:for-each>
               [ "", "", "", "", "", "" ]
            );


         // bugs fields: bug id, category id, code id, pattern id, priority, release id, class name, packagename (populated by javascript)
         var bugs = new Array(
            <xsl:for-each select="/BugCollection/BugInstance[string-length(@last)=0]">

               [ "<xsl:value-of select="@instanceHash" />-<xsl:value-of select="@instanceOccurrenceNum" />",
                 "<xsl:value-of select="@category" />",
                 "<xsl:value-of select="@abbrev" />",
                 "<xsl:value-of select="@type" />",
                 <xsl:value-of select="@priority" />,
                 <xsl:choose><xsl:when test='string-length(@first)=0'>0</xsl:when><xsl:otherwise><xsl:value-of select="@first" /></xsl:otherwise></xsl:choose>,
                 "<xsl:value-of select="Class/@classname" />",
                 ""],
            </xsl:for-each>
               [ "", "", "", "", 0, 0, "", "" ]
            );

         // bugs fields: bug id, category id, code id, pattern id, priority, first release id, fixed release id, class name
         var fixedBugs = new Array(
            <xsl:for-each select="/BugCollection/BugInstance[string-length(@last)>0]">

               [ "<xsl:value-of select="@instanceHash" />-<xsl:value-of select="@instanceOccurrenceNum" />",
                 "<xsl:value-of select="@category" />",
                 "<xsl:value-of select="@abbrev" />",
                 "<xsl:value-of select="@type" />",
                 <xsl:value-of select="@priority" />,
                 <xsl:choose><xsl:when test='string-length(@first)=0'>0</xsl:when><xsl:otherwise><xsl:value-of select="@first" /></xsl:otherwise></xsl:choose>,
                 <xsl:choose><xsl:when test='string-length(@last)>0'><xsl:value-of select="@last" /></xsl:when><xsl:otherwise>-42</xsl:otherwise></xsl:choose>,
                 "<xsl:value-of select="Class/@classname" />" ],
            </xsl:for-each>
               [ "", "", "", "", 0, 0, 0, "" ]
            );

      </script>
   </head>
   <body>
      <h3>
         <a href="http://findbugs.sourceforge.net">FindBugs</a> (<xsl:value-of select="/BugCollection/@version" />) 
         Analysis for 
         <xsl:choose>
            <xsl:when test='string-length(/BugCollection/Project/@projectName)>0'><xsl:value-of select="/BugCollection/Project/@projectName" /></xsl:when>
            <xsl:otherwise><xsl:value-of select="/BugCollection/Project/@filename" /></xsl:otherwise>
         </xsl:choose>
      </h3>

      <div id='menuWrapper' style=''>
         <div id="navcontainer">
            <ul id="navlist">
               <li><a id='summary'           class="current" href="#" onclick="selectMenu('summary'); return false;"         >Summary</a></li>
               <li><a id='history'           class="none"    href="#" onclick="selectMenu('history'); return false;"         >History</a></li>
               <li><a id='listByCategories'  class="none"    href="#" onclick="selectMenu('listByCategories'); return false;">Browse By Categories</a></li>
               <li><a id='listByPackages'    class="none"    href="#" onclick="selectMenu('listByPackages'); return false;"  >Browse by Packages</a></li>
               <li><a id='info'              class="none"    href="#" onclick="selectMenu('info'); return false;"            >Info</a></li>
            </ul>
         </div>
      </div>

      <div id='displayWrapper'>

      <div style='height:25px;'>
         <div id='messageContainer' style='float:right;'>
            Computing data...
         </div>
         <div id='filterWrapper' style='display:none;'>
            <form name='findbugsForm'>
               <div id='filterContainer' >
                  <select name='versions' onchange='filter()'>
                     <option value="loading">Loading filter...</option>
                  </select>
                  <select name='priorities' onchange='filter()'>
                     <option value="loading">Loading filter...</option>
                  </select>
               </div>
            </form>
         </div>
         <div id='historyControlWrapper' style='display:none;'>
           <form name="findbugsHistoryControlForm">
             <div id='historyControlContainer'>
               <input type='checkbox' name='includeFixedIntroducedBugs'
                      value='checked' alt='Include fixed introduced bugs.'
                      onclick='includeFixedIntroducedBugsInHistory()' />
               Include counts of introduced bugs that were fixed in later releases.
             </div>
           </form>
         </div>
      </div>
         <div id='summaryContainer'          class='displayContainer'>
            <h3>Package Summary</h3>
            <table>
               <tr>
                  <th>Package</th>
                  <th>Code Size</th>
                  <th>Bugs</th>
                  <th>Bugs p1</th>
                  <th>Bugs p2</th>
                  <th>Bugs p3</th>
                  <th>Bugs Exp.</th>
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
               </tr>
               <xsl:for-each select="/BugCollection/FindBugsSummary/PackageStats">
                  <xsl:sort select="@package" order="ascending" />
                  <xsl:if test="@total_bugs!='0'" >
                     <tr>
                        <td class='summary-name'><xsl:value-of select="@package" /></td>
                        <td class='summary-size'><xsl:value-of select="@total_size" /></td>
                        <td class='summary-priority-all'><xsl:value-of select="@total_bugs" /></td>
                        <td class='summary-priority-1'><xsl:value-of select="@priority_1" /></td>
                        <td class='summary-priority-2'><xsl:value-of select="@priority_2" /></td>
                        <td class='summary-priority-3'><xsl:value-of select="@priority_3" /></td>
                        <td class='summary-priority-4'><xsl:value-of select="@priority_4" /></td>
                     </tr>
                  </xsl:if>
               </xsl:for-each>
            </table>
         </div>

         <div id='infoContainer'             class='displayContainer'>
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
         <div id='historyContainer'          class='displayContainer'>Loading...</div>
         <div id='listByCategoriesContainer' class='displayContainer'>Loading...</div>
         <div id='listByPackagesContainer'   class='displayContainer'>Loading...</div>
      </div>

      <div id='bug-collection' style='display:none;'>
      <!-- advanced tooltips -->
      <xsl:for-each select="/BugCollection/BugPattern">
         <xsl:variable name="b-t"><xsl:value-of select="@type" /></xsl:variable>
         <div>
            <xsl:attribute name="id">tip-<xsl:value-of select="$b-t" /></xsl:attribute>
            <xsl:attribute name="class">tip</xsl:attribute>
            <xsl:value-of select="/BugCollection/BugPattern[@type=$b-t]/Details" disable-output-escaping="yes" />
         </div>
      </xsl:for-each>

      <!-- bug descriptions - hidden -->
      <xsl:for-each select="/BugCollection/BugInstance[not(@last)]">
            <div style="display:none;" class='bug'>
               <xsl:attribute name="id">b-uid-<xsl:value-of select="@instanceHash" />-<xsl:value-of select="@instanceOccurrenceNum" /></xsl:attribute>
               <xsl:for-each select="*/Message">
                  <div class="b-r"><xsl:apply-templates /></div>
               </xsl:for-each>
               <div class="b-d">
                  <xsl:value-of select="LongMessage" disable-output-escaping="no" />
               </div>
            </div>
      </xsl:for-each>
      </div>
   </body>
</html>
</xsl:template>


</xsl:transform>

