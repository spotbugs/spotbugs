<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version='1.0'
                xmlns="http://www.w3.org/TR/xhtml1/transitional"
                exclude-result-prefixes="#default">

<!-- build.xml will substitute the real path to fo/docbook.xsl here. -->
<xsl:import href="@FO_XSL_STYLESHEET@"/>

<!-- Enumerate sections. -->
<xsl:variable name="section.autolabel">1</xsl:variable>

<!-- Use graphics in admonitions -->
<xsl:variable name="admon.graphics">1</xsl:variable>

<!-- Admonition graphics are in the "manual" subdirectory. -->
<xsl:variable name="admon.graphics.path">manual/</xsl:variable>

<!-- Included graphics are also in the "manual" subdirectory. -->
<xsl:variable name="img.src.path">manual/</xsl:variable>

<!-- Default image width is 5 inches - otherwise, they become much too large.
     FIXME: for some reason, this isn't honored.  Blech.
-->
<xsl:variable name="default.image.width">5in</xsl:variable>

<!-- Just put chapters and sect1s in the TOC. -->
<xsl:variable name="toc.section.depth">1</xsl:variable>

</xsl:stylesheet>
