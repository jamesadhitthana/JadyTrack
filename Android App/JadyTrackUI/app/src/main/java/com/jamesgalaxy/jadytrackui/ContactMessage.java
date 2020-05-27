package com.jamesgalaxy.jadytrackui;

public class ContactMessage {

    public String name;
    public String email;
    public String message;

    public ContactMessage() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public ContactMessage(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.message = password;
    }

}

