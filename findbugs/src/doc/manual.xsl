<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version='1.0'
                xmlns="http://www.w3.org/TR/xhtml1/transitional"
                exclude-result-prefixes="#default">

<!-- build.xml will substitute the real path to chunk.xsl here. -->
<xsl:import href="@HTML_XSL_STYLESHEET@"/>

<!-- This causes the stylesheet to put chapters in a single HTML file,
     rather than putting individual sections into seperate files. -->
<xsl:variable name="chunk.section.depth">0</xsl:variable>

<!-- Put the HTML in the "manual" directory. -->
<xsl:variable name="base.dir">manual/</xsl:variable>

</xsl:stylesheet>
