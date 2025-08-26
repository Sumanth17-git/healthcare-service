package com.healthcare.healthcare_service;


public class Patient {
    private String id;
    private String name;
    private int age;
    private String gender;
    private String condition;

    public Patient() {}
    public Patient(String id, String name, int age, String gender, String condition) {
        this.id = id; this.name = name; this.age = age; this.gender = gender; this.condition = condition;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getCondition() { return condition; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setAge(int age) { this.age = age; }
    public void setGender(String gender) { this.gender = gender; }
    public void setCondition(String condition) { this.condition = condition; }
}
