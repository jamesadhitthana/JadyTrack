package com.jady.jadytrack.entity;

public class User {

    public String name;
    public String email;
//    public String password; //Nov 10 2020 (Password is now encrypted) using SCRYPT hashing

    public User(String name, String email) { //,String password) {
        this.name = name;
        this.email = email;
//        this.password = password; //Nov 10 2020 (Password is now encrypted) using SCRYPT hashing
    }

}

