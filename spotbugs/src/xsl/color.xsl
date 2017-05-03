<?xml version="1.0" encoding="UTF-8"?>
<!--
  FindBugs - Find bugs in Java programs
  Copyright (C) 2004,2005 University of Maryland
  Copyright (C) 2005, Chris Nappin
  Copyright (C) 2015, Brahim Djoudi (modifications)

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
<xsl:stylesheet version="1.0"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output
	method="xml"
	omit-xml-declaration="yes"
    doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
    doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
	indent="yes"
	encoding="UTF-8"/>

<xsl:variable name="bugTableHeader">
	<tr class="tableheader">
		<th align="left">Warning</th>
		<th align="center">Priority</th>
		<th align="left">Details</th>
	</tr>
</xsl:variable>

<xsl:template match="/">
	<html>
	<head>
		<title>FindBugs Report</title>
		<style type="text/css">
		.tablerow0 {
			background: #EEEEEE;
		}

		.tablerow1 {
			background: white;
		}

		.detailrow0 {
			background: #EEEEEE;
		}

		.detailrow1 {
			background: white;
		}
		.long_message {
			color:#220001;
			background: -moz-linear-gradient(left, rgba(255,255,255,0) 0%, rgba(255,255,255,0.96) 24%, rgba(255,255,255,1) 25%, rgba(255,255,255,0) 100%); /* FF3.6+ */
			background: -webkit-gradient(linear, left top, right top, color-stop(0%,rgba(255,255,255,0)), color-stop(24%,rgba(255,255,255,0.96)), color-stop(25%,rgba(255,255,255,1)), color-stop(100%,rgba(255,255,255,0))); /* Chrome,Safari4+ */
			background: -webkit-linear-gradient(left, rgba(255,255,255,0) 0%,rgba(255,255,255,0.96) 24%,rgba(255,255,255,1) 25%,rgba(255,255,255,0) 100%); /* Chrome10+,Safari5.1+ */
			background: -o-linear-gradient(left, rgba(255,255,255,0) 0%,rgba(255,255,255,0.96) 24%,rgba(255,255,255,1) 25%,rgba(255,255,255,0) 100%); /* Opera 11.10+ */
			background: -ms-linear-gradient(left, rgba(255,255,255,0) 0%,rgba(255,255,255,0.96) 24%,rgba(255,255,255,1) 25%,rgba(255,255,255,0) 100%); /* IE10+ */
			background: linear-gradient(to right, rgba(255,255,255,0) 0%,rgba(255,255,255,0.96) 24%,rgba(255,255,255,1) 25%,rgba(255,255,255,0) 100%); /* W3C */
			filter: progid:DXImageTransform.Microsoft.gradient( startColorstr='#00ffffff', endColorstr='#00ffffff',GradientType=1 ); /* IE6-9 */
		}

		.tableheader {
			font-size: larger;
			background: -moz-linear-gradient(left,  rgba(135,224,253,1) 0%, rgba(83,203,241,0.9) 40%, rgba(59,4,153,0.75) 100%); /* FF3.6+ */
			background: -webkit-gradient(linear, left top, right top, color-stop(0%,rgba(135,224,253,1)), color-stop(40%,rgba(83,203,241,0.9)), color-stop(100%,rgba(59,4,153,0.75))); /* Chrome,Safari4+ */
			background: -webkit-linear-gradient(left,  rgba(135,224,253,1) 0%,rgba(83,203,241,0.9) 40%,rgba(59,4,153,0.75) 100%); /* Chrome10+,Safari5.1+ */
			background: -o-linear-gradient(left,  rgba(135,224,253,1) 0%,rgba(83,203,241,0.9) 40%,rgba(59,4,153,0.75) 100%); /* Opera 11.10+ */
			background: -ms-linear-gradient(left,  rgba(135,224,253,1) 0%,rgba(83,203,241,0.9) 40%,rgba(59,4,153,0.75) 100%); /* IE10+ */
			background: linear-gradient(to right,  rgba(135,224,253,1) 0%,rgba(83,203,241,0.9) 40%,rgba(59,4,153,0.75) 100%); /* W3C */
			filter: progid:DXImageTransform.Microsoft.gradient( startColorstr='#87e0fd', endColorstr='#bf3b0499',GradientType=1 ); /* IE6-9 */

		}
		.high {
			background: -moz-linear-gradient(top, rgba(239,187,110,0.93) 0%, rgba(255,26,0,0.97) 52%, rgba(239,187,110,1) 100%); /* FF3.6+ */
			background: -webkit-gradient(linear, left top, left bottom, color-stop(0%,rgba(239,187,110,0.93)), color-stop(52%,rgba(255,26,0,0.97)), color-stop(100%,rgba(239,187,110,1))); /* Chrome,Safari4+ */
			background: -webkit-linear-gradient(top, rgba(239,187,110,0.93) 0%,rgba(255,26,0,0.97) 52%,rgba(239,187,110,1) 100%); /* Chrome10+,Safari5.1+ */
			background: -o-linear-gradient(top, rgba(239,187,110,0.93) 0%,rgba(255,26,0,0.97) 52%,rgba(239,187,110,1) 100%); /* Opera 11.10+ */
			background: -ms-linear-gradient(top, rgba(239,187,110,0.93) 0%,rgba(255,26,0,0.97) 52%,rgba(239,187,110,1) 100%); /* IE10+ */
			background: linear-gradient(to bottom, rgba(239,187,110,0.93) 0%,rgba(255,26,0,0.97) 52%,rgba(239,187,110,1) 100%); /* W3C */
			filter: progid:DXImageTransform.Microsoft.gradient( startColorstr='#edefbb6e', endColorstr='#efbb6e',GradientType=0 ); /* IE6-9 */
		}
		.medium {
			background: -moz-linear-gradient(top, rgba(244,232,117,1) 0%, rgba(239,187,110,0.96) 52%, rgba(244,232,117,0.93) 100%); /* FF3.6+ */
			background: -webkit-gradient(linear, left top, left bottom, color-stop(0%,rgba(244,232,117,1)), color-stop(52%,rgba(239,187,110,0.96)), color-stop(100%,rgba(244,232,117,0.93))); /* Chrome,Safari4+ */
			background: -webkit-linear-gradient(top, rgba(244,232,117,1) 0%,rgba(239,187,110,0.96) 52%,rgba(244,232,117,0.93) 100%); /* Chrome10+,Safari5.1+ */
			background: -o-linear-gradient(top, rgba(244,232,117,1) 0%,rgba(239,187,110,0.96) 52%,rgba(244,232,117,0.93) 100%); /* Opera 11.10+ */
			background: -ms-linear-gradient(top, rgba(244,232,117,1) 0%,rgba(239,187,110,0.96) 52%,rgba(244,232,117,0.93) 100%); /* IE10+ */
			background: linear-gradient(to bottom, rgba(244,232,117,1) 0%,rgba(239,187,110,0.96) 52%,rgba(244,232,117,0.93) 100%); /* W3C */
			filter: progid:DXImageTransform.Microsoft.gradient( startColorstr='#f4e875', endColorstr='#edf4e875',GradientType=0 ); /* IE6-9 */
		}
		.low {
			background: -moz-linear-gradient(top, rgba(244,232,117,0.93) 0%, rgba(140,186,124,0.97) 53%, rgba(244,232,117,1) 100%); /* FF3.6+ */
			background: -webkit-gradient(linear, left top, left bottom, color-stop(0%,rgba(244,232,117,0.93)), color-stop(53%,rgba(140,186,124,0.97)), color-stop(100%,rgba(244,232,117,1))); /* Chrome,Safari4+ */
			background: -webkit-linear-gradient(top, rgba(244,232,117,0.93) 0%,rgba(140,186,124,0.97) 53%,rgba(244,232,117,1) 100%); /* Chrome10+,Safari5.1+ */
			background: -o-linear-gradient(top, rgba(244,232,117,0.93) 0%,rgba(140,186,124,0.97) 53%,rgba(244,232,117,1) 100%); /* Opera 11.10+ */
			background: -ms-linear-gradient(top, rgba(244,232,117,0.93) 0%,rgba(140,186,124,0.97) 53%,rgba(244,232,117,1) 100%); /* IE10+ */
			background: linear-gradient(to bottom, rgba(244,232,117,0.93) 0%,rgba(140,186,124,0.97) 53%,rgba(244,232,117,1) 100%); /* W3C */
			filter: progid:DXImageTransform.Microsoft.gradient( startColorstr='#edf4e875', endColorstr='#f4e875',GradientType=0 ); /* IE6-9 */
		}
		pre {
			font-family: "Bitstream Vera Sans Mono", Consolas, Inconsolata, "Lucida Console", "Courier New", Monospace !important;
			box-shadow: 0 0;
			color: black;
			border-width: 1px 1px 1px 6px;
			border-style: solid;
			padding: 2ex;
			margin: 2ex 2ex 2ex 2ex;
			overflow: auto;
			-moz-border-radius: 0px;
			-webkit-border-radius: 0px;
			-khtml-border-radius: 0px;
			border-radius: 0px;
			border-color: #996666;

			background: rgb(232,239,244); /* Old browsers */
			background: -moz-linear-gradient(left, rgba(232,239,244,1) 1%, rgba(244,249,249,1) 23%, rgba(249,250,246,1) 87%, rgba(241,242,236,1) 98%); /* FF3.6+ */
			background: -webkit-gradient(linear, left top, right top, color-stop(1%,rgba(232,239,244,1)), color-stop(23%,rgba(244,249,249,1)), color-stop(87%,rgba(249,250,246,1)), color-stop(98%,rgba(241,242,236,1))); /* Chrome,Safari4+ */
			background: -webkit-linear-gradient(left, rgba(232,239,244,1) 1%,rgba(244,249,249,1) 23%,rgba(249,250,246,1) 87%,rgba(241,242,236,1) 98%); /* Chrome10+,Safari5.1+ */
			background: -o-linear-gradient(left, rgba(232,239,244,1) 1%,rgba(244,249,249,1) 23%,rgba(249,250,246,1) 87%,rgba(241,242,236,1) 98%); /* Opera 11.10+ */
			background: -ms-linear-gradient(left, rgba(232,239,244,1) 1%,rgba(244,249,249,1) 23%,rgba(249,250,246,1) 87%,rgba(241,242,236,1) 98%); /* IE10+ */
			background: linear-gradient(to right, rgba(232,239,244,1) 1%,rgba(244,249,249,1) 23%,rgba(249,250,246,1) 87%,rgba(241,242,236,1) 98%); /* W3C */
			filter: progid:DXImageTransform.Microsoft.gradient( startColorstr='#e8eff4', endColorstr='#f1f2ec',GradientType=1 ); /* IE6-9 */
		}
		</style>
	</head>

	<xsl:variable name="unique-catkey" select="/BugCollection/BugCategory/@category"/>
	<!--xsl:variable name="unique-catkey" select="/BugCollection/BugInstance[generate-id() = generate-id(key('bug-category-key',@category))]/@category"/-->

	<body>

	<h1>FindBugs Report</h1>
		<p>Produced using <a href="http://findbugs.sourceforge.net">FindBugs </a> <xsl:value-of select="/BugCollection/@version"/>.</p>
		<p>Project:
			<xsl:choose>
				<xsl:when test='string-length(/BugCollection/Project/@projectName)>0'><xsl:value-of select="/BugCollection/Project/@projectName" /></xsl:when>
				<xsl:otherwise><xsl:value-of select="/BugCollection/Project/@filename" /></xsl:otherwise>
			</xsl:choose>
		</p>

		<table style="width:90%;">
			<tr>
				<td>
					<h2>Metrics</h2>
					<xsl:apply-templates select="/BugCollection/FindBugsSummary"/>
				</td>
				<td>
					<h2>Summary</h2>
					<table cellpadding="5" cellspacing="2" style="width:90%;border-collapse: collapse;border-style:solid;border-width:thin;">
					    <tr class="tableheader">
							<th align="left">Warning Type</th>
							<th align="right">Number</th>
						</tr>

					<xsl:for-each select="$unique-catkey">
						<xsl:sort select="." order="ascending"/>
						<xsl:variable name="catkey" select="."/>
						<xsl:variable name="catdesc" select="/BugCollection/BugCategory[@category=$catkey]/Description"/>
						<xsl:variable name="styleclass">
							<xsl:choose><xsl:when test="position() mod 2 = 1">tablerow0</xsl:when>
								<xsl:otherwise>tablerow1</xsl:otherwise>
							</xsl:choose>
						</xsl:variable>

						<tr class="{$styleclass}">
							<td><a href="#Warnings_{$catkey}"><xsl:value-of select="$catdesc"/> Warnings</a></td>
							<td align="right"><xsl:value-of select="count(/BugCollection/BugInstance[@category=$catkey])"/></td>
						</tr>
					</xsl:for-each>

					<xsl:variable name="styleclass">
						<xsl:choose><xsl:when test="count($unique-catkey) mod 2 = 0">tablerow0</xsl:when>
							<xsl:otherwise>tablerow1</xsl:otherwise>
						</xsl:choose>
					</xsl:variable>
						<tr class="{$styleclass}">
						    <td><b>Total</b></td>
						    <td align="right"><b><xsl:value-of select="count(/BugCollection/BugInstance)"/></b></td>
						</tr>
					</table>
				</td>
			</tr>
		</table>

	<p><br/><br/></p>

	<h1>Warnings</h1>

	<p>Click on each warning link to see a full description of the issue, and
	    details of how to resolve it.</p>

	<xsl:for-each select="$unique-catkey">
		<xsl:sort select="." order="ascending"/>
		<xsl:variable name="catkey" select="."/>
		<xsl:variable name="catdesc" select="/BugCollection/BugCategory[@category=$catkey]/Description"/>

		<xsl:call-template name="generateWarningTable">
			<xsl:with-param name="warningSet" select="/BugCollection/BugInstance[@category=$catkey]"/>
			<xsl:with-param name="sectionTitle"><xsl:value-of select="$catdesc"/> Warnings</xsl:with-param>
			<xsl:with-param name="sectionId">Warnings_<xsl:value-of select="$catkey"/></xsl:with-param>
		</xsl:call-template>
	</xsl:for-each>

    <p><br/><br/></p>
	<h1><a name="Details">Warning Types</a></h1>

	<xsl:apply-templates select="/BugCollection/BugPattern">
		<xsl:sort select="@abbrev"/>
		<xsl:sort select="ShortDescription"/>
	</xsl:apply-templates>

	</body>
	</html>
</xsl:template>

<xsl:template match="BugInstance">
	<xsl:variable name="warningId"><xsl:value-of select="generate-id()"/></xsl:variable>

	<tr>
		<!-- class="tablerow{position() mod 2}" -->
		<xsl:choose>
			<xsl:when test="@priority = 1"><xsl:attribute name="class">high</xsl:attribute></xsl:when>
			<xsl:when test="@priority = 2"><xsl:attribute name="class">medium</xsl:attribute></xsl:when>
			<xsl:when test="@priority = 3"><xsl:attribute name="class">low</xsl:attribute></xsl:when>
			<xsl:otherwise><xsl:attribute name="bgcolor">#fdfdfd</xsl:attribute></xsl:otherwise>
		</xsl:choose>
		<td width="20%" valign="top">
			<a href="#{@type}"><xsl:value-of select="ShortMessage"/></a>
		</td>
		<td width="10%" valign="top" align="center">
			<xsl:choose>
				<xsl:when test="@priority = 1"><strong>High</strong></xsl:when>
				<xsl:when test="@priority = 2">Medium</xsl:when>
				<xsl:when test="@priority = 3">Low</xsl:when>
				<xsl:otherwise>Unknown</xsl:otherwise>
			</xsl:choose>
		</td>
		<td width="70%">
			<dl>
				<dt class='long_message'><xsl:value-of select="LongMessage"/></dt>
				<dd>
					<!--  add source filename and line number(s), if any -->
					<xsl:if test="SourceLine">
						In file <tt><strong><xsl:value-of select="SourceLine/@sourcefile"/></strong></tt>,
						<xsl:choose>
							<xsl:when test="SourceLine/@start = SourceLine/@end">
							line <xsl:value-of select="SourceLine/@start"/>
							</xsl:when>
							<xsl:otherwise>
							lines <xsl:value-of select="SourceLine/@start"/>
							    to <xsl:value-of select="SourceLine/@end"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:if>

					<xsl:for-each select="./*/Message">
						<br/><xsl:value-of select="text()"/>
					</xsl:for-each>
				</dd>
			</dl>
		</td>
	</tr>
</xsl:template>

<xsl:template match="BugPattern">
	<h2><a name="{@type}"><xsl:value-of select="ShortDescription"/></a></h2>
	<xsl:value-of select="Details" disable-output-escaping="yes"/>
	<p><br/><br/></p>
</xsl:template>

<xsl:template name="generateWarningTable">
	<xsl:param name="warningSet"/>
	<xsl:param name="sectionTitle"/>
	<xsl:param name="sectionId"/>

	<h2><a name="{$sectionId}"><xsl:value-of select="$sectionTitle"/></a></h2>
	<table class="warningtable" cellspacing="2" cellpadding="5" style="width:100%;border-collapse: collapse;border-style:solid;border-width:thin;">
		<xsl:copy-of select="$bugTableHeader"/>
		<xsl:choose>
		    <xsl:when test="count($warningSet) &gt; 0">
				<xsl:apply-templates select="$warningSet">
					<xsl:sort select="@priority"/>
					<xsl:sort select="@abbrev"/>
					<xsl:sort select="Class/@classname"/>
				</xsl:apply-templates>
		    </xsl:when>
		    <xsl:otherwise>
		        <tr><td colspan="2"><p><i>None</i></p></td></tr>
		    </xsl:otherwise>
		</xsl:choose>
	</table>
	<p><br/><br/></p>
</xsl:template>

<xsl:template match="FindBugsSummary">
    <xsl:variable name="kloc" select="@total_size div 1000.0"/>
    <xsl:variable name="format" select="'#######0.00'"/>

	<p><xsl:value-of select="@total_size"/> lines of code analysed,
	in <xsl:value-of select="@total_classes"/> classes,
	in <xsl:value-of select="@num_packages"/> packages.</p>
	<table cellpadding="5" cellspacing="2" style="width:90%;border-collapse: collapse;border-style:solid;border-width:thin;">
	    <tr class="tableheader">
			<th align="left">Metric</th>
			<th align="right">Total</th>
			<th align="right">Density*</th>
		</tr>
		<tr class="high" >
			<td>High Priority Warnings</td>
			<td align="right"><xsl:value-of select="@priority_1"/></td>
			<td align="right"><xsl:value-of select="format-number(@priority_1 div $kloc, $format)"/></td>
		</tr>
		<tr class="medium">
			<td>Medium Priority Warnings</td>
			<td align="right"><xsl:value-of select="@priority_2"/></td>
			<td align="right"><xsl:value-of select="format-number(@priority_2 div $kloc, $format)"/></td>
		</tr>

    <xsl:choose>
		<xsl:when test="@priority_3">
			<tr class="low">
				<td>Low Priority Warnings</td>
				<td align="right"><xsl:value-of select="@priority_3"/></td>
				<td align="right"><xsl:value-of select="format-number(@priority_3 div $kloc, $format)"/></td>
			</tr>
		</xsl:when>
	</xsl:choose>

		<tr bgcolor="#f0f0f0">
			<td><b>Total Warnings</b></td>
			<td align="right"><b><xsl:value-of select="@total_bugs"/></b></td>
			<td align="right"><b><xsl:value-of select="format-number(@total_bugs div $kloc, $format)"/></b></td>
		</tr>
	</table>
	<p><i>(* Defects per thousand lines of non-commenting source statements)</i></p>
	<p><br/><br/></p>

</xsl:template>

</xsl:stylesheet>