package edu.umd.cs.findbugs.flybush;

import java.util.Date;

public interface DbPluginUpdateXml {
    String getContents();
    void setContents(String contents);

    void setDate(Date date);
    Date getDate();

    void setUser(String email);
    String getUser();
}
