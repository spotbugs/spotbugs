<?xml version="1.0" encoding="UTF-8" ?>
<!--
  Copyright (C) 2005, 2006 Etienne Giraudy, InStranet Inc
  Copyright (C) 2005, 2007 Etienne Giraudy
  Copyright (C) 2019 Björn Kautler

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
      doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
      doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
      encoding="UTF-8"/>

<xsl:variable name="apos" select="&quot;'&quot;"/>

<xsl:template match="/">

<xsl:variable name="titlePart">
   <xsl:text> (</xsl:text>
   <xsl:value-of select="/BugCollection/@version"/>
   <xsl:text>)</xsl:text>
   <xsl:variable name="project" select="(/BugCollection/Project/@projectName, /BugCollection/Project/@filename)[1]"/>
   <xsl:text> analysis</xsl:text>
   <xsl:if test="$project">
      <xsl:text> for </xsl:text>
      <xsl:value-of select="$project"/>
   </xsl:if>
   <xsl:variable name="release" select="/BugCollection/@release"/>
   <xsl:if test="$release">
      <xsl:text> of release </xsl:text>
      <xsl:value-of select="$release"/>
   </xsl:if>
</xsl:variable>

<html>
   <head>
      <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
      <title>
         <xsl:text>SpotBugs</xsl:text>
         <xsl:value-of select="$titlePart"/>
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
            background: #ef2929;
            text-align:center;
         }
         .summary-priority-2 {
            background: #fcaf3e;
            text-align:center;
         }
         .summary-priority-3 {
            background: #8ae234;
            text-align:center;
         }
         .summary-priority-4 {
            background: #729fcf;
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
            padding: 1px 3px 1px 3px;
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
            background: #ef2929; height: 0.5em; width: 1em;
            margin-right: 0.5em;
         }
         .b-2 {
            background: #fcaf3e; height: 0.5em; width: 1em;
            margin-right: 0.5em;
         }
         .b-3 {
            background: #8ae234; height: 0.5em; width: 1em;
            margin-right: 0.5em;
         }
         .b-4 {
            background: #729fcf; height: 0.5em; width: 1em;
            margin-right: 0.5em;
         }
      </style>
      <script type='text/javascript'>
         <xsl:text disable-output-escaping='yes'><![CDATA[
         var menus = new Array('summary', 'info', 'history', 'listByCategories', 'listByPackages');

         var state = new Proxy({
            tab: "summary",
            includeFixed: true,
            release: -1,
            priority: 4
         }, {
            set: function (target, name, value) {
               target[name] = value;
               history.replaceState(null, null, "#" + JSON.stringify(target));
            }
         });

         var filterContainerId              = "filterWrapper";
         var historyControlContainerId      = "historyControlWrapper";
         var messageContainerId             = "messageContainer";
         var summaryContainerId             = "summaryContainer";
         var infoContainerId                = "infoContainer";
         var historyContainerId             = "historyContainer";
         var listByCategoriesContainerId    = "listByCategoriesContainer";
         var listByPackagesContainerId      = "listByPackagesContainer";

         // main init function
         function init() {
            if (versions.size <= 1) {
               hide("historyMenu")
            }
            loadFilter();
            onHashChange()
            includeFixedIntroducedBugsInHistory();
            window.addEventListener('hashchange', onHashChange);
         }

         function onHashChange() {
            try {
               var hash = location.hash.substring(1);
               if (hash.charAt(1) != "\"") {
                  hash = decodeURIComponent(hash);
               }
               var parsedState = JSON.parse(hash);
               selectMenu(parsedState.tab);
               var element = document.findbugsHistoryControlForm.includeFixedIntroducedBugs;
               element.checked = parsedState.includeFixed;
               element.onchange();
               element = document.findbugsForm.versions;
               element.value = (element.namedItem(parsedState.release) == null) ? state.release : parsedState.release;
               element.onchange();
               element = document.findbugsForm.priorities;
               element.value = (element.namedItem(parsedState.priority) == null) ? state.priority : parsedState.priority;
               element.onchange();
            } catch (e) {
               history.replaceState(null, null, "#" + JSON.stringify(state));
            }
         }

         // menu callback function
         function selectMenu(menuId) {
            document.getElementById(state.tab).className="none";
            document.getElementById(menuId).className="current";
            if (menuId!=state.tab) {
               hideMenu(state.tab);
               state.tab = menuId;
            }
            if (menuId=="summary")           displaySummary();
            if (menuId=="info")              displayInfo();
            if (menuId=="history")           displayHistory();
            if (menuId=="listByCategories")  displayListByCategories();
            if (menuId=="listByPackages")    displayListByPackages();
         }

         // display filter
         function loadFilter() {
            var versionsBox = document.findbugsForm.versions;
            versionsBox.options[0] = new Option(" -- All Releases -- ", "-1");
            versionsBox.options[0].id = "-1";
            versionsBox.options.selectedIndex = 0;
            for (version of versions) {
               var option = new Option(version[1], version[0]);
               option.id = version[0];
               versionsBox.options.add(option);
            }
            if (versions.size <= 1) {
               versionsBox.style.display = "none";
            }
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
            hide(menuId + "Container");
         }

         // filter callback function
         function filter() {
            state.release = document.findbugsForm.versions.value;
            state.priority = document.findbugsForm.priorities.value;
            selectMenu(state.tab);
         }

         // includeFixedBugs callback function
         function includeFixedIntroducedBugsInHistory() {
            state.includeFixed = document.findbugsHistoryControlForm.includeFixedIntroducedBugs.checked;
            selectMenu(state.tab);
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

            content += "<table><tr><th>Release</th><th>Total Bugs</th><th>Bugs P1</th><th>Bugs P2</th><th>Bugs P3</th><th>Bugs Experimental</th></tr>";

            var aSpan   = "<span title='Bugs introduced in this release that have not been fixed.'>";
            var fSpan   = "<span title='Bugs fixed in this release.'>";
            var fiSpan  = "<span title='Bugs introduced in this release that were fixed in later releases.'>";
            var afiSpan = "<span title='Total number of bugs introduced in this release.'>";
            var eSpan   = "</span>";

            var versionIds = Array.from(versions.keys())
            if(state.includeFixed) {
               for (i=(versionIds.length-1); i>0; i--) {
                  var versionId = versionIds[i];
                  v = countBugsVersion(versionId, 4);
                  t = countTotalBugsVersion(versionId);
                  o = countFixedButActiveBugsVersion(versionId);
                  f = countFixedBugsInVersion(versionId);
                  fi = countFixedBugsIntroducedInVersion(versionId);
                  content += "<tr>";
                  content += "<td class='summary-name'>" + versions.get(versionId) + "</td>";
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
               for (i=(versionIds.length-1); i>0; i--) {
                  var versionId = versionIds[i];
                  v = countBugsVersion(versionId, 4);
                  t = countTotalBugsVersion(versionId);
                  o = countFixedButActiveBugsVersion(versionId);
                  f = countFixedBugsInVersion(versionId);
                  content += "<tr>";
                  content += "<td class='summary-name'>" + versions.get(versionId) + "</td>";
                  content += "<td class='summary-priority-all'> " + (t[0] + o[0]) + " (+" + aSpan + v[0] + eSpan + " / -" + fSpan + f[0] + eSpan + ") </td>";
                  content += "<td class='summary-priority-1'  > " + (t[1] + o[1]) + " (+" + aSpan + v[1] + eSpan + " / -" + fSpan + f[1] + eSpan + ") </td>";
                  content += "<td class='summary-priority-2'  > " + (t[2] + o[2]) + " (+" + aSpan + v[2] + eSpan + " / -" + fSpan + f[2] + eSpan + ") </td>";
                  content += "<td class='summary-priority-3'  > " + (t[3] + o[3]) + " (+" + aSpan + v[3] + eSpan + " / -" + fSpan + f[3] + eSpan + ") </td>";
                  content += "<td class='summary-priority-4'  > " + (t[4] + o[4]) + " (+" + aSpan + v[4] + eSpan + " / -" + fSpan + f[4] + eSpan + ") </td>";
                  content += "</tr>";
               }
            }

            t = countTotalBugsVersion(versionIds[0]);
            o = countFixedButActiveBugsVersion(versionIds[0]);
            content += "<tr>";
            content += "<td class='summary-name'>" + versions.get(versionIds[0]) + "</td>";
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
         function displayCategoriesCodes(containerId, category) {
            displayMessage("Loading stats (codes)...");
            var container = document.getElementById(containerId);
            container.style.display="block";
            if (container.innerHTML=="Loading..." || container.innerHTML=="") {
               container.innerHTML = displayLevel2("lbc", category);
            }
            resetMessage();
         }

         // list by categories, display bug package>codes
         function displayPackageCodes(containerId, packageName) {
            displayMessage("Loading stats (codes)...");
            var container = document.getElementById(containerId);
            container.style.display="block";
            if (container.innerHTML=="Loading..." || container.innerHTML=="") {
               container.innerHTML = displayLevel2("lbp", packageName);
            }
            resetMessage();
         }

         // list by categories, display bug cat>codes>patterns
         function displayCategoriesCodesPatterns(containerId, category, code) {
            displayMessage("Loading stats (patterns)...");
            var container = document.getElementById(containerId);
            container.style.display="block";
            if (container.innerHTML=="Loading..." || container.innerHTML=="") {
               container.innerHTML = displayLevel3("lbc", category, code);
            }
            resetMessage();
         }

         // list by package, display bug package>class>patterns
         function displayPackageClassPatterns(containerId, packageName, className) {
            displayMessage("Loading stats (patterns)...");
            var container = document.getElementById(containerId);
            container.style.display="block";
            if (container.innerHTML=="Loading..." || container.innerHTML=="") {
               container.innerHTML = displayLevel3("lbp", packageName, className);
            }
            resetMessage();
         }

         // list by categories, display bug cat>codes>patterns>bugs
         function displayCategoriesCodesPatternsBugs(containerId, category, code, pattern) {
            displayMessage("Loading stats (bugs)...");
            var container = document.getElementById(containerId);
            container.style.display="block";
            if (container.innerHTML=="Loading..." || container.innerHTML=="") {
               container.innerHTML = displayLevel4("lbc", category, code, pattern);
            }
            resetMessage();
         }

         // list by package, display bug package>class>patterns>bugs
         function displayPackageClassPatternsBugs(containerId, packageName, className, pattern) {
            displayMessage("Loading stats (bugs)...");
            var container = document.getElementById(containerId);
            container.style.display="block";
            if (container.innerHTML=="Loading..." || container.innerHTML=="") {
               container.innerHTML = displayLevel4("lbp", packageName, className, pattern);
            }
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

            for (var x=0; x<max; x++) {
               if (list=="lbp" && packageStats[x].totalBugs=="0") continue;

               if (list=="lbc") {
                  id = categories[x].id;
                  label = categories[x].label;
                  containerId = "categories-" + id;
                  subContainerId = "cat-"+id;
                  p = countBugsCat(state.release, state.priority, id);
               }
               if (list=="lbp") {
                  id = packageStats[x].packageName;
                  label = packageStats[x].packageName;
                  containerId = "packages-" + id;
                  subContainerId = "package-"+id;
                  p = countBugsPackage(state.release, state.priority, id);
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
            if (state.release!=-1) {
               content += " (introduced in release " + versions.get(state.release) +")";
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

            for (var x=0; x<max; x++) {
               if (list=="lbp" && classStats[x].totalBugs=="0") continue;

               if (list=="lbc") {
                  id2 = codes[x].id;
                  label = codes[x].label;
                  containerId = "codes-"+id1;
                  subContainerId = "cat-" + id1 + "-code-" + id2;
                  p = countBugsCode(state.release, state.priority, id1, id2);
               }
               if (list=="lbp") {
                  id2 = classStats[x].className;
                  label = classStats[x].className;
                  containerId = "packages-"+id1;
                  subContainerId = "package-" + id1 + "-class-" + id2;
                  p = countBugsClass(state.release, state.priority, id1, id2);
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

            if ((list=="lbc") || (list=="lbp")) {
               max = patterns.length;
            }

            for (var x=0; x<max; x++) {
               if (list=="lbc") {
                  id3 = patterns[x].id;
                  label = patterns[x].label + " ( " + id3 + " )";
                  containerId = "patterns-"+id1;
                  subContainerId = "cat-" + id1 + "-code-" + id2 + "-pattern-" + id3;
                  p = countBugsPattern(state.release, state.priority, id1, id2, id3);
               }
               if (list=="lbp") {
                  id3 = patterns[x].id;
                  label = patterns[x].label + " ( " + id3 + " )";
                  containerId = "classpatterns-"+id1;
                  subContainerId = "package-" + id1 + "-class-" + id2 + "-pattern-" + id3;
                  p = countBugsClassPattern(state.release, state.priority, id2, id3);
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
            for (bug of bugs) {
               if (list=="lbc") {
                  if ( bug.category!=id1 || bug.code!=id2 || bug.pattern!=id3 ) continue;
                  if ( state.release!=-1
                     && state.release!=bug.firstVersion) continue;
                  if ( state.priority!=4
                     && state.priority<bug.priority) continue;

                  subContainerId = "cat-" + id1 + "-code-" + id2 + "-pattern-" + id3 + "-bug-" + bug.id;
               }
               if (list=="lbp") {
                  if ( bug.packageName!=id1 || bug.className!=id2 || bug.pattern!=id3 ) continue;
                  if ( state.release!=-1
                     && state.release!=bug.firstVersion) continue;
                  if ( state.priority!=4
                     && state.priority<bug.priority) continue;

                  subContainerId = "package-" + id1 + "-class-" + id2 + "-pattern-" + id3 + "-bug-" + bug.id;
               }

               bugId = "b-uid-" + bug.id;
               label = bug.className;
               containerId = "bugs-"+bugId;
               bugP = bug.priority;
               p[bugP]++;
               var total = p[1]+p[2]+p[3]+p[4];
               if (total > 0) {
                  content += addBug(   4, containerId, label, bugP, bug.firstVersion, subContainerId,
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
            if (state.priority>1)
               content += " <em>("+p[1];
            if (state.priority>=2)
               content += "/"+p[2];
            if (state.priority>=3)
               content += "/"+p[3];
            if (state.priority>=4)
               content += "/"+p[4];
            if (state.priority>1)
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
            if ((versions.size>1) && (version==lastVersion)) {
               content += "<span style='color:#ef2929;font-weight:bold;'>NEW!</span> ";
            }
            content += "<strong>" + label + "</strong>";
            if (version==0) {
               content += " <em>since first historized release " + versions.get(version.toString()) + "</em>";
            } else {
               content += " <em>since release " + versions.get(version.toString()) + "</em>";
            }
            content += "</a>";
            content += "</div>";
            content += "<div class='bugList-level" + level + "-inner' id='" + subId + "' style='display:none;'>Loading...</div>";
            content += "</div>";
            return content;
         }

         function countBugsVersion(version, priority) {
            return countBugs(version, priority, null, null, null, null, null);
         }

         function countBugsCat(version, priority, category) {
            return countBugs(version, priority, category, null, null, null, null);
         }

         function countBugsPackage(version, priority, packageName) {
            return countBugs(version, priority, null, null, null, packageName, null);
         }

         function countBugsCode(version, priority, category, code) {
            return countBugs(version, priority, category, code, null, null, null);
         }

         function countBugsPattern(version, priority, category, code, pattern) {
            return countBugs(version, priority, category, code, pattern, null, null);
         }

         function countBugsClass(version, priority, packageName, className) {
            return countBugs(version, priority, null, null, null, packageName, className);
         }

         function countBugsClassPattern(version, priority, className, pattern) {
            return countBugs(version, priority, null, null, pattern, null, className);
         }

         function countBugs(version, priority, category, code, pattern, packageName, className) {
            var count = [0,0,0,0,0];
            for (bug of bugs) {
               if (     (version==-1  || version==bug.firstVersion)
                     && (priority==4  || priority>=bug.priority)
                     && (!category    || bug.category==category)
                     && (!code        || bug.code==code)
                     && (!pattern     || bug.pattern==pattern)
                     && (!packageName || bug.packageName==packageName)
                     && (!className   || bug.className==className)
                     ) {
                  count[bug.priority]++;
               }
            }
            count[0] = count[1] + count[2] + count[3] + count[4];
            return count;
         }

         function countFixedBugsInVersion(version) {
            var count = [0,0,0,0,0];
            var last=1000000;
            for (bug of fixedBugs) {
               if (version==-1 || version==(bug.lastVersion+1)) {
                  count[bug.priority]++;
               }
            }
            count[0] = count[1] + count[2] + count[3] + count[4];
            return count;
         }

         function countFixedBugsIntroducedInVersion(version) {
            var count = [0,0,0,0,0];
            var last=1000000;
            for (bug of fixedBugs) {
               if (version==-1 || version==(bug.firstVersion)) {
                  count[bug.priority]++;
               }
            }
            count[0] = count[1] + count[2] + count[3] + count[4];
            return count;
         }

         function countFixedButActiveBugsVersion(version) {
            var count = [0,0,0,0,0];
            var last=1000000;
            for (bug of fixedBugs) {
               if ( version==-1 || (version >=bug.firstVersion && version<=bug.lastVersion) ) {
                  count[bug.priority]++;
               }
            }
            count[0] = count[1] + count[2] + count[3] + count[4];
            return count;
         }

         function countTotalBugsVersion(version) {
            var count = [0,0,0,0,0];
            var last=1000000;
            for (bug of bugs) {
               if (version==-1 || version>=bug.firstVersion) {
                  count[bug.priority]++;
               }
            }
            count[0] = count[1] + count[2] + count[3] + count[4];
            return count;
         }

         function getPriorityLegend() {
            var content = "";
            content += "<h5><span class='b-1'>&nbsp;&nbsp;&nbsp;</span>P1&nbsp;&nbsp;&nbsp;";
            content += "<span class='b-2'>&nbsp;&nbsp;&nbsp;</span>P2&nbsp;&nbsp;&nbsp;";
            content += "<span class='b-3'>&nbsp;&nbsp;&nbsp;</span>P3&nbsp;&nbsp;&nbsp;";
            content += "<span class='b-4'>&nbsp;&nbsp;&nbsp;</span>Experimental";
            content += "</h5>";
            return content;
         }

         function showbug(bugId, containerId, pattern) {
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
            var pattern = document.getElementById('tip-'+pattern).innerHTML;
            toggle(containerId);
            bugplaceholder.innerHTML = newBug + pattern;
         }
         function toggle(containerId) {
            if( document.getElementById(containerId).style.display == "none") {
               show(containerId);
            } else {
               if( document.getElementById(containerId).style.display == "block") {
                  hide(containerId);
               } else {
                  show(containerId);
               }
            }
         }
         function show(containerId) {
            document.getElementById(containerId).style.display="block";
         }
         function hide(containerId) {
            document.getElementById(containerId).style.display="none";
         }

         window.onload = function(){
            init();
         };

         var lastVersion = "]]></xsl:text>
         <xsl:value-of select="/BugCollection/@sequence"/>
         <xsl:text>";

         // versions fields: release id, release label
         var versions = new Map([</xsl:text>
            <xsl:for-each select="/BugCollection/History/AppVersion">
               <xsl:sort select="@sequence" data-type="number"/>
               <xsl:text>&#x0A;               ["</xsl:text>
               <xsl:value-of select="@sequence"/>
               <xsl:text>", "</xsl:text>
               <xsl:value-of select="@release"/>
               <xsl:text>"],</xsl:text>
            </xsl:for-each>
            <xsl:text>&#x0A;               ["</xsl:text>
            <xsl:value-of select="/BugCollection/@sequence"/>
            <xsl:text>", "</xsl:text>
            <xsl:value-of select="/BugCollection/@release"/>
            <xsl:text>"]
         ]);

         var categories = new Array(</xsl:text>
            <xsl:for-each select="/BugCollection/BugCategory">
               <xsl:sort select="Description"/>
               <xsl:text>&#x0A;               {id: "</xsl:text>
               <xsl:value-of select="@category"/>
               <xsl:text>", label: "</xsl:text>
               <xsl:value-of select="Description"/>
               <xsl:text>"}</xsl:text>
               <xsl:if test="position() != last()">
                  <xsl:text>,</xsl:text>
               </xsl:if>
            </xsl:for-each>
         <xsl:text>
         );

         var codes = new Array(</xsl:text>
            <xsl:for-each select="/BugCollection/BugCode">
               <xsl:sort select="Description"/>
               <xsl:text>&#x0A;               {id: "</xsl:text>
               <xsl:value-of select="@abbrev"/>
               <xsl:text>", label: "</xsl:text>
               <xsl:value-of select="Description"/>
               <xsl:text>"}</xsl:text>
               <xsl:if test="position() != last()">
                  <xsl:text>,</xsl:text>
               </xsl:if>
            </xsl:for-each>
         <xsl:text>
         );

         var patterns = new Array(</xsl:text>
            <xsl:for-each select="/BugCollection/BugPattern">
               <xsl:sort select="ShortDescription"/>
               <xsl:text>&#x0A;               {category: "</xsl:text>
               <xsl:value-of select="@category"/>
               <xsl:text>", code: "</xsl:text>
               <xsl:value-of select="@abbrev"/>
               <xsl:text>", id: "</xsl:text>
               <xsl:value-of select="@type"/>
               <xsl:text>", label: "</xsl:text>
               <xsl:value-of select="translate(ShortDescription, '&quot;', $apos)"/>
               <xsl:text>"}</xsl:text>
               <xsl:if test="position() != last()">
                  <xsl:text>,</xsl:text>
               </xsl:if>
            </xsl:for-each>
         <xsl:text>
         );

         var classStats = new Array(</xsl:text>
            <xsl:for-each select="/BugCollection/FindBugsSummary/PackageStats/ClassStats">
               <xsl:sort select="@class"/>
               <xsl:text>&#x0A;               {className: "</xsl:text>
               <xsl:value-of select="@class"/>
               <xsl:text>", packageName: "</xsl:text>
               <xsl:value-of select="../@package"/>
               <xsl:text>", isInterface: "</xsl:text>
               <xsl:value-of select="@interface"/>
               <xsl:text>", totalBugs: "</xsl:text>
               <xsl:value-of select="@bugs"/>
               <xsl:text>", bugsP1: "</xsl:text>
               <xsl:value-of select="(@priority_1, 0)[1]"/>
               <xsl:text>", bugsP2: "</xsl:text>
               <xsl:value-of select="(@priority_2, 0)[1]"/>
               <xsl:text>", bugsP3: "</xsl:text>
               <xsl:value-of select="(@priority_3, 0)[1]"/>
               <xsl:text>", bugsP4: "</xsl:text>
               <xsl:value-of select="(@priority_4, 0)[1]"/>
               <xsl:text>"}</xsl:text>
               <xsl:if test="position() != last()">
                  <xsl:text>,</xsl:text>
               </xsl:if>
            </xsl:for-each>
         <xsl:text>
         );

         var packageStats = new Array(</xsl:text>
            <xsl:for-each select="/BugCollection/FindBugsSummary/PackageStats">
               <xsl:sort select="@package"/>
               <xsl:text>&#x0A;               {packageName: "</xsl:text>
               <xsl:value-of select="@package"/>
               <xsl:text>", totalBugs: "</xsl:text>
               <xsl:value-of select="@total_bugs"/>
               <xsl:text>", bugsP1: "</xsl:text>
               <xsl:value-of select="(@priority_1, 0)[1]"/>
               <xsl:text>", bugsP2: "</xsl:text>
               <xsl:value-of select="(@priority_2, 0)[1]"/>
               <xsl:text>", bugsP3: "</xsl:text>
               <xsl:value-of select="(@priority_3, 0)[1]"/>
               <xsl:text>", bugsP4: "</xsl:text>
               <xsl:value-of select="(@priority_4, 0)[1]"/>
               <xsl:text>"}</xsl:text>
               <xsl:if test="position() != last()">
                  <xsl:text>,</xsl:text>
               </xsl:if>
            </xsl:for-each>
         <xsl:text>
         );

         var bugs = new Array(</xsl:text>
            <xsl:for-each select="/BugCollection/BugInstance[string-length(@last) = 0]">
               {
                  id: "<xsl:value-of select="@instanceHash" />-<xsl:value-of select="@instanceOccurrenceNum" />",
                  category: "<xsl:value-of select="@category" />",
                  code: "<xsl:value-of select="@abbrev" />",
                  pattern: "<xsl:value-of select="@type" />",
                  priority: <xsl:value-of select="@priority" />,
                  firstVersion: <xsl:value-of select="(@first, 0)[1]"/>,
                  className: "<xsl:value-of select="(Class[@primary='true'], Class)[1]/@classname"/>",
                  packageName: "<xsl:value-of select="replace((Class[@primary='true'], Class)[1]/@classname, '\.[^.]+$', '')"/>
                  <xsl:text>"
               }</xsl:text>
               <xsl:if test="position() != last()">
                  <xsl:text>,</xsl:text>
               </xsl:if>
            </xsl:for-each>
         <xsl:text>
         );

         var fixedBugs = new Array(</xsl:text>
            <xsl:for-each select="/BugCollection/BugInstance[string-length(@last) > 0]">
               {
                  id: "<xsl:value-of select="@instanceHash" />-<xsl:value-of select="@instanceOccurrenceNum" />",
                  category: "<xsl:value-of select="@category" />",
                  code: "<xsl:value-of select="@abbrev" />",
                  pattern: "<xsl:value-of select="@type" />",
                  priority: <xsl:value-of select="@priority" />,
                  firstVersion: <xsl:value-of select="@first"/>,
                  lastVersion: <xsl:value-of select="@last"/>,
                  className: "<xsl:value-of select="(Class[@primary='true'], Class)[1]/@classname"/>
                  <xsl:text>"
               }</xsl:text>
               <xsl:if test="position() != last()">
                  <xsl:text>,</xsl:text>
               </xsl:if>
            </xsl:for-each>
         );
      </script>
   </head>
   <body>
      <h3>
         <a href="https://spotbugs.github.io">SpotBugs</a>
         <xsl:value-of select="$titlePart"/>
      </h3>

      <div id='menuWrapper'>
         <div id="navcontainer">
            <ul id="navlist">
               <li                 ><a id='summary'           class="current" href="#summaryTab"          onclick="selectMenu('summary'); return false;"         >Summary</a></li>
               <li id="historyMenu"><a id='history'           class="none"    href="#historyTab"          onclick="selectMenu('history'); return false;"         >History</a></li>
               <li                 ><a id='listByCategories'  class="none"    href="#listByCategoriesTab" onclick="selectMenu('listByCategories'); return false;">Browse By Categories</a></li>
               <li                 ><a id='listByPackages'    class="none"    href="#listByPackagesTab"   onclick="selectMenu('listByPackages'); return false;"  >Browse by Packages</a></li>
               <li                 ><a id='info'              class="none"    href="#infoTab"             onclick="selectMenu('info'); return false;"            >Info</a></li>
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
                     <option id="4" value="4"> -- All priorities -- </option>
                     <option id="1" value="1"> P1 bugs </option>
                     <option id="2" value="2"> P1 and P2 bugs </option>
                     <option id="3" value="3"> P1, P2 and P3 bugs </option>
                  </select>
               </div>
            </form>
         </div>
         <div id='historyControlWrapper' style='display:none;'>
            <form name="findbugsHistoryControlForm">
               <div id='historyControlContainer'>
                  <input type='checkbox' name='includeFixedIntroducedBugs'
                         value='checked' alt='Include fixed introduced bugs.'
                         onchange='includeFixedIntroducedBugsInHistory()' />
                  Include counts of introduced bugs that were fixed in later releases.
               </div>
            </form>
         </div>
      </div>
         <div id='summaryContainer' class='displayContainer'>
            <h3>Package Summary</h3>
            <table>
               <tr>
                  <th>Package</th>
                  <th>Code Size</th>
                  <th>Total Bugs</th>
                  <th>Bugs P1</th>
                  <th>Bugs P2</th>
                  <th>Bugs P3</th>
                  <th>Bugs Experimental</th>
               </tr>
               <tr>
                  <td class='summary-name'>
                     <xsl:text>Overall (</xsl:text>
                     <xsl:value-of select="/BugCollection/FindBugsSummary/@num_packages"/>
                     <xsl:text> packages, </xsl:text>
                     <xsl:value-of select="/BugCollection/FindBugsSummary/@total_classes"/>
                     <xsl:text> classes)</xsl:text>
                  </td>
                  <td class='summary-size'><xsl:value-of select="/BugCollection/FindBugsSummary/@total_size"/></td>
                  <td class='summary-priority-all'><xsl:value-of select="/BugCollection/FindBugsSummary/@total_bugs"/></td>
                  <td class='summary-priority-1'><xsl:value-of select="(/BugCollection/FindBugsSummary/@priority_1, 0)[1]"/></td>
                  <td class='summary-priority-2'><xsl:value-of select="(/BugCollection/FindBugsSummary/@priority_2, 0)[1]"/></td>
                  <td class='summary-priority-3'><xsl:value-of select="(/BugCollection/FindBugsSummary/@priority_3, 0)[1]"/></td>
                  <td class='summary-priority-4'><xsl:value-of select="(/BugCollection/FindBugsSummary/@priority_4, 0)[1]"/></td>
               </tr>
               <xsl:for-each select="/BugCollection/FindBugsSummary/PackageStats">
                  <xsl:sort select="@package"/>
                  <xsl:if test="@total_bugs != '0'">
                     <tr>
                        <td class='summary-name'><xsl:value-of select="@package" /></td>
                        <td class='summary-size'><xsl:value-of select="@total_size" /></td>
                        <td class='summary-priority-all'><xsl:value-of select="@total_bugs" /></td>
                        <td class='summary-priority-1'><xsl:value-of select="(@priority_1, 0)[1]" /></td>
                        <td class='summary-priority-2'><xsl:value-of select="(@priority_2, 0)[1]" /></td>
                        <td class='summary-priority-3'><xsl:value-of select="(@priority_3, 0)[1]" /></td>
                        <td class='summary-priority-4'><xsl:value-of select="(@priority_4, 0)[1]" /></td>
                     </tr>
                  </xsl:if>
               </xsl:for-each>
            </table>
         </div>

         <div id='infoContainer' class='displayContainer'>
            <div id='analyzed-files'>
               <h3>Analyzed Files:</h3>
               <ul>
                  <xsl:for-each select="/BugCollection/Project/Jar">
                     <xsl:sort select="."/>
                     <li><xsl:value-of select="."/></li>
                  </xsl:for-each>
               </ul>
            </div>
            <div id='used-libraries'>
               <h3>Used Libraries:</h3>
               <ul>
                  <xsl:choose>
                     <xsl:when test="/BugCollection/Project/AuxClasspathEntry">
                        <xsl:for-each select="/BugCollection/Project/AuxClasspathEntry">
                           <xsl:sort select="."/>
                           <li><xsl:value-of select="."/></li>
                        </xsl:for-each>
                     </xsl:when>
                     <xsl:otherwise>
                        <li>None</li>
                     </xsl:otherwise>
                  </xsl:choose>
               </ul>
            </div>
            <div id='source-files'>
               <h3>Source Files:</h3>
               <ul>
                  <xsl:choose>
                     <xsl:when test="/BugCollection/Project/SrcDir">
                        <xsl:for-each select="/BugCollection/Project/SrcDir">
                           <xsl:sort select="."/>
                           <li><xsl:value-of select="."/></li>
                        </xsl:for-each>
                     </xsl:when>
                     <xsl:otherwise>
                        <li>None</li>
                     </xsl:otherwise>
                  </xsl:choose>
               </ul>
            </div>
            <div id='plugins'>
               <h3>Plugins:</h3>
               <ul>
                  <xsl:choose>
                     <xsl:when test="/BugCollection/Project/Plugin">
                        <xsl:for-each select="/BugCollection/Project/Plugin">
                           <xsl:sort select="@id"/>
                           <li><xsl:value-of select="@id"/> (enabled: <xsl:value-of select="@enabled"/>)</li>
                        </xsl:for-each>
                     </xsl:when>
                     <xsl:otherwise>
                        <li>None</li>
                     </xsl:otherwise>
                  </xsl:choose>
               </ul>
            </div>
            <div id='analysis-error'>
               <h3>Analysis Errors:</h3>
               <ul>
                  <xsl:choose>
                     <xsl:when test="/BugCollection/Errors/MissingClass">
                        <li>
                           <xsl:text>Missing ref classes for analysis:&#x0A;                     </xsl:text>
                           <ul>
                              <xsl:for-each select="/BugCollection/Errors/MissingClass">
                                 <xsl:sort select="."/>
                                 <li><xsl:value-of select="."/></li>
                              </xsl:for-each>
                           </ul>
                        </li>
                     </xsl:when>
                     <xsl:otherwise>
                        <li>None</li>
                     </xsl:otherwise>
                  </xsl:choose>
               </ul>
            </div>
         </div>
         <div id='historyContainer' class='displayContainer'>Loading...</div>
         <div id='listByCategoriesContainer' class='displayContainer'>Loading...</div>
         <div id='listByPackagesContainer' class='displayContainer'>Loading...</div>
      </div>

      <div id='bug-collection' style='display:none;'>
         <!-- bug descriptions -->
         <xsl:for-each select="/BugCollection/BugInstance[not(@last)]">
            <div id="{concat('b-uid-', @instanceHash, '-', @instanceOccurrenceNum)}" class='bug'>
               <xsl:for-each select="*/Message">
                  <div class="b-r"><xsl:value-of select="."/></div>
               </xsl:for-each>
               <div class="b-d"><xsl:value-of select="LongMessage"/></div>
            </div>
         </xsl:for-each>

         <!-- advanced tips -->
         <xsl:for-each select="/BugCollection/BugPattern">
            <xsl:sort select="@type"/>
            <div id="{concat('tip-', @type)}" class="tip">
               <xsl:value-of select="concat('&#x0A;            ', replace(replace(replace(Details, '^\s+', ''), '(\r?\n|\r)\s*', '$1            '), '\s+$', '&#x0A;         '))" disable-output-escaping="yes"/>
            </div>
         </xsl:for-each>
      </div>
   </body>
</html>
</xsl:template>
</xsl:transform>
