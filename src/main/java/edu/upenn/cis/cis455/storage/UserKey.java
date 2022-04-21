package edu.upenn.cis.cis455.storage;

import java.io.Serializable;

@SuppressWarnings("serial")
public class UserKey implements Serializable
{
    private String username;

    public UserKey(String username) {
        this.username = username;
    }

    public final String getUsername() {
        return username;
    }
} 