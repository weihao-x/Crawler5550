package edu.upenn.cis.cis455.storage;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Md5Key implements Serializable {
    private String md5;

    public Md5Key(String md5) {
        this.md5 = md5;
    }

    public final String getMd5() {
        return md5;
    }
} 