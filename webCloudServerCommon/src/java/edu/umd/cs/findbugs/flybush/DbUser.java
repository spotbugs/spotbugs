package edu.umd.cs.findbugs.flybush;

public interface DbUser extends Comparable<DbUser> {

    String getOpenid();

    String getEmail();

    Object createKeyObject();
}
