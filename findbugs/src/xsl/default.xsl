<?xml version="1.0" encoding="UTF-8"?>

<!--
  FindBugs - Find bugs in Java programs
  Copyright (C) 2004, University of Maryland
  
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

<!--
  A simple XSLT stylesheet to transform FindBugs XML results
  annotated with messages into HTML.

  Authors:
  David Hovemeyer
-->

<xsl:stylesheet
	version="1.0"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output
	method="xml"
	omit-xml-declaration="yes"
	standalone="yes"
	doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
	doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
	indent="yes"
	encoding="UTF-8"/>

<xsl:variable name="bugTableHeader">
	<tr class="tableheader">
		<th align="left">Code</th>
		<th align="left">Warning</th>
		<th align="left">Location</th>
	</tr>
</xsl:variable>

<xsl:template match="/">
	<html>
	<head>
		<title>FindBugs Report</title>
		<style type="text/css">
		table.warningtable {
			border-collapse: collapse;
		}

		.tablerow0 {
			background: #EEEEEE;
		}

		.tablerow1 {
			background: white;
		}

		.tableheader {
			background: #b9b9fe;
		}

		.tablerow0:hover, .tablerow1:hover {
			background: #aaffaa;
		}
		</style>
		<script type="text/javascript">
			// <![CDATA[
			function toggleRow(elid) {
				if (document.getElementById) {
					element = document.getElementById(elid);
					if (element.style.display == 'none') {
						element.style.display = 'table-row';
						//window.status = 'Toggle on!';
					} else {
						element.style.display = 'none';
						//window.status = 'Toggle off!';
					}
				}
			}
			// ]]>
		</script>
	</head>
	<body>

	<h1>FindBugs Report</h1>
	<p> Some metadata about the report would be nice. </p>

	<h1>Warnings</h1>

	<p>Click on a warning row to see full context information.</p>

	<h2><a name="Warnings_CORRECTNESS">Correctness</a></h2>
	<p>
	<table class="warningtable" width="100%">
	<xsl:copy-of select="$bugTableHeader"/>
	<xsl:apply-templates select="/BugCollection/BugInstance[@category='CORRECTNESS']">
		<xsl:sort select="@abbrev"/>
		<xsl:sort select="Class/@classname"/>
	</xsl:apply-templates>
	</table>
	</p>

	<h2><a name="Warnings_MT_CORRECTNESS">Multithreaded Correctness</a></h2>
	<p>
	<table class="warningtable" width="100%">
	<xsl:copy-of select="$bugTableHeader"/>
	<xsl:apply-templates select="/BugCollection/BugInstance[@category='MT_CORRECTNESS']">
		<xsl:sort select="@abbrev"/>
		<xsl:sort select="Class/@classname"/>
	</xsl:apply-templates>
	</table>
	</p>

	<h2><a name="Warnings_MALICIOUS_CODE">Malicious Code Vulnerability</a></h2>
	<p>
	<table class="warningtable" width="100%">
	<xsl:copy-of select="$bugTableHeader"/>
	<xsl:apply-templates select="/BugCollection/BugInstance[@category='MALICIOUS_CODE']">
		<xsl:sort select="@abbrev"/>
		<xsl:sort select="Class/@classname"/>
	</xsl:apply-templates>
	</table>
	</p>

	<h2><a name="Warnings_PERFORMANCE">Performance</a></h2>
	<p>
	<table class="warningtable" width="100%">
	<xsl:copy-of select="$bugTableHeader"/>
	<xsl:apply-templates select="/BugCollection/BugInstance[@category='PERFORMANCE']">
		<xsl:sort select="@abbrev"/>
		<xsl:sort select="Class/@classname"/>
	</xsl:apply-templates>
	</table>
	</p>

	<h1>Details</h1>

	<xsl:apply-templates select="/BugCollection/BugPattern">
		<xsl:sort select="@abbrev"/>
		<xsl:sort select="ShortDescription"/>
	</xsl:apply-templates>

	</body>
	</html>
</xsl:template>

<xsl:template match="BugInstance">
	<xsl:variable name="warningClass">tablerow<xsl:value-of select="position() mod 2"/></xsl:variable>
	<xsl:variable name="warningId"><xsl:value-of select="generate-id()"/></xsl:variable>

	<tr class="{$warningClass}" onclick="toggleRow('{$warningId}');">

	<td>
	<xsl:value-of select="@abbrev"/>
	</td>

	<td>
	<a href="#{@type}"><xsl:value-of select="ShortMessage"/></a>
	</td>

	<td>
	<xsl:value-of select="Class/@classname"/>
	<xsl:choose>
		<xsl:when test="SourceLine">
			<br/><xsl:apply-templates select="SourceLine[1]"/>
		</xsl:when>
		<xsl:when test="Method/SourceLine">
			<br/><xsl:apply-templates select="Method/SourceLine"/>
		</xsl:when>
	</xsl:choose>
	</td>

	</tr>

	<!-- Add bug annotation elements: Class, Method, Field, SourceLine, Field -->
	<tr class="{$warningClass}" id="{$warningId}" style="display: none;">
		<td/>
		<td colspan="2">
			<table>
				<xsl:for-each select="./*/Message">
					<tr>
						<td><xsl:value-of select="text()"/></td>
					</tr>
				</xsl:for-each>
			</table>
		</td>
	</tr>
</xsl:template>

<xsl:template match="SourceLine">
	<!-- Only match the first SourceLine of all matched by the template -->
	<xsl:if test="position() = 1">
		at <xsl:value-of select="@sourcefile"/>
		<xsl:choose>
			<xsl:when test="@start &gt; 0">
				<xsl:if test="@end != @start">, lines <xsl:value-of select="@start"/>-<xsl:value-of select="@end"/>
				</xsl:if>
				<xsl:if test="@start = @end">, line <xsl:value-of select="@start"/>
				</xsl:if>
			</xsl:when>
		</xsl:choose>
	</xsl:if>
</xsl:template>

<xsl:template match="BugPattern">
	<h2><a name="{@type}"><xsl:value-of select="@abbrev"/>: <xsl:value-of select="ShortDescription"/></a></h2>
	<xsl:value-of select="Details" disable-output-escaping="yes"/>
</xsl:template>

</xsl:stylesheet>

<!-- vim:set ts=4: -->
