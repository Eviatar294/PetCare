package com.example.petcare;

import java.io.Serializable;
import com.google.firebase.database.Exclude;

public class Pet implements Serializable {
    private String petId;
    private String name;
    private String petType;
    private String imageString;

    public Pet() {
    }

    public Pet(String petId, String name, String petType, String image) {
        this.petId = petId;
        this.name = name;
        this.petType = petType;
        this.imageString = image;
    }

    public Pet(String petId, String name, String petType) {
        this.petId = petId;
        this.name = name;
        this.petType = petType;
        this.imageString = "";
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPetId() {
        return petId;
    }

    public void setPetId(String petId) {
        this.petId = petId;
    }

    public String getPetType() {
        return petType;
    }

    public void setPetType(String petType) {
        this.petType = petType;
    }

    public String getImageString() {
        return imageString;
    }

    public void setImageString(String imageString) {
        this.imageString = imageString;
    }
}
