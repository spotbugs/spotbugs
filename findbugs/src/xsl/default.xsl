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
	omit-xml-declaration="no"
	standalone="yes"
	doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
	doctype-system= "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
	indent="yes"
	encoding="UTF-8"/>

<xsl:variable name="bugTableHeader">
	<tr>
		<th align="left">Warning</th>
		<th align="left">Location</th>
	</tr>
</xsl:variable>

<xsl:template match="/">
	<html>
	<head>
		<title>FindBugs Report</title>
	</head>
	<body>

	<h1>FindBugs Report</h1>
	<p> Some metadata about the report would be nice. </p>

	<h1>Warnings</h1>

	<h2><a name="Warnings_CORRECTNESS">Correctness</a></h2>
	<p>
	<table>
	<xsl:copy-of select="$bugTableHeader"/>
	<xsl:apply-templates select="/BugCollection/BugInstance[@category='CORRECTNESS']"/>
	</table>
	</p>

	<h2><a name="Warnings_MT_CORRECTNESS">Multithreaded Correctness</a></h2>
	<p>
	<table>
	<xsl:copy-of select="$bugTableHeader"/>
	<xsl:apply-templates select="/BugCollection/BugInstance[@category='MT_CORRECTNESS']"/>
	</table>
	</p>

	<h2><a name="Warnings_MALICIOUS_CODE">Malicious Code Vulnerability</a></h2>
	<p>
	<table>
	<xsl:copy-of select="$bugTableHeader"/>
	<xsl:apply-templates select="/BugCollection/BugInstance[@category='MALICIOUS_CODE']"/>
	</table>
	</p>

	<h2><a name="Warnings_PERFORMANCE">Performance</a></h2>
	<p>
	<table>
	<xsl:copy-of select="$bugTableHeader"/>
	<xsl:apply-templates select="/BugCollection/BugInstance[@category='PERFORMANCE']"/>
	</table>
	</p>

	</body>
	</html>
</xsl:template>

<xsl:template match="BugInstance">
	<tr>

	<td>
	<xsl:value-of select="LongMessage"/>
	</td>

	<td>
	<xsl:choose>
		<xsl:when test="SourceLine">
			<xsl:apply-templates select="SourceLine[1]"/>
		</xsl:when>
		<xsl:when test="Method[1]/SourceLine">
			<xsl:apply-templates select="Method[1]/SourceLine"/>
<!--
			<xsl:variable name="firstMethodWithSourceLine" select="Method/SourceLine/.."/>
			<xsl:apply-templates select="$firstMethodWithSourceLine"/>
-->
		</xsl:when>
	</xsl:choose>
	</td>

	</tr>
</xsl:template>

<xsl:template match="SourceLine">
	<xsl:value-of select="@sourcefile"/>
	<xsl:choose>
		<xsl:when test="@start &gt; 0">
			<xsl:if test="@end != @start">, lines <xsl:value-of select="@start"/>-<xsl:value-of select="@end"/>
			</xsl:if>
			<xsl:if test="@start = @end">, line <xsl:value-of select="@start"/>
			</xsl:if>
		</xsl:when>
	</xsl:choose>
</xsl:template>

</xsl:stylesheet>

<!-- vim:set ts=4: -->
