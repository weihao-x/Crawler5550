package edu.upenn.cis.cis455.storage;

import java.io.Serializable;

@SuppressWarnings("serial")
public class UserData implements Serializable {
	private String password;

    public UserData(String password) {
        this.password = password;
    }

    public final String getPassword() {
        return password;
    }
} 
