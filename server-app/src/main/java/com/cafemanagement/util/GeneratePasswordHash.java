package com.cafemanagement.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class  GeneratePasswordHash {
    public static void main(String[] args) {
        String password = "123";
        String hashedPassword = BCrypt.withDefaults().hashToString(10, password.toCharArray());
        System.out.println("Hashed password: " + hashedPassword);
    }
}
