package com.tictactoe;

import java.util.UUID;

public class User {

    private UUID id;

    private String name;

    public User(){

    }

    public User(UUID userId, String name){
        this.id = userId;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
