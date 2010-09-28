package edu.umd.cs.findbugs.flybush;

public interface DbUser extends Comparable<DbUser> {

    String getOpenid();

    String getEmail();

    String getUploadToken();

    void setUploadToken(String token);

    Object createKeyObject();
}
