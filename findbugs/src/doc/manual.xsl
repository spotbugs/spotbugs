<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version='1.0'
                xmlns="http://www.w3.org/TR/xhtml1/transitional"
                exclude-result-prefixes="#default">

<!-- build.xml will substitute the real path to chunk.xsl here. -->
<xsl:import href="@HTML_XSL_STYLESHEET@"/>

<xsl:template name="user.header.content">
@GOOGLE_ANALYTICS@
</xsl:template>

<!-- This causes the stylesheet to put chapters in a single HTML file,
     rather than putting individual sections into seperate files. -->
<xsl:variable name="chunk.section.depth">0</xsl:variable>

<!-- Put the HTML in the "manual" directory. -->
<xsl:variable name="base.dir">manual/</xsl:variable>

<!-- Enumerate sections. -->
<xsl:variable name="section.autolabel">1</xsl:variable>

<!-- Name the HTML files based on the id of the document elements. -->
<xsl:variable name="use.id.as.filename">1</xsl:variable>

<!-- Use graphics in admonitions -->
<xsl:variable name="admon.graphics">1</xsl:variable>

<!-- Admonition graphics are in the same place as the generated HTML. -->
<xsl:variable name="admon.graphics.path"></xsl:variable>

<!-- Just put chapters and sect1s in the TOC. -->
<xsl:variable name="toc.section.depth">1</xsl:variable>

</xsl:stylesheet>
