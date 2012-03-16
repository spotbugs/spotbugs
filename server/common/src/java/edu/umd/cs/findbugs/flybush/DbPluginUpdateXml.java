package edu.umd.cs.findbugs.flybush;

import java.util.Date;


/*
<?xml version="1.0" encoding="UTF-8"?>
<fb-plugin-updates>
    <plugin id="edu.umd.cs.findbugs.plugins.core">
        <release
                date="12/21/2011 10:00 am EST"
                version="2.0.0"
                url="http://findbugs.cs.umd.edu/">
           <message>FindBugs 2.0.0 has been released</message>
        </release>
    </plugin>
</fb-plugin-updates>
*/

/**
 * This has two uses:
 *
 * 1. getContents() returns a full XML response to /update-check
 * -or-
 * 2. contents is left blank, and info for a particular plugin release is stored in the other fields (plugin id,
 *    release date, etc)
 */
public interface DbPluginUpdateXml {
    String getContents();
    void setContents(String contents);

    void setDate(Date date);
    Date getDate();

    void setUser(String email);
    String getUser();

    String getPluginId();
    void setPluginId(String pluginId);
    
    Date getReleaseDate();
    void setReleaseDate(Date date);
    
    String getVersion();
    void setVersion(String version);

    String getChannel();
    void setChannel(String channel);

    String getUrl();
    void setUrl(String url);
    
    String getMessage();
    void setMessage(String message);
    
}
