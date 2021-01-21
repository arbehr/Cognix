package com.cognitivabrasil.cognix.entities;

/**
 *
 * @author andre
 */
public class Email {
    
    private String name;
    private String email;
    private String message;
    private String ip;

    
    public Email() { 
    }

    Email(String name,String email, String message, String ip){
        this();
        this.name = name;
        this.email = email;
        this.message = message;
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

   public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getIP() {
        return ip;
    }

    public void setIP(String ip) {
        this.ip = ip;
    }
    
}
