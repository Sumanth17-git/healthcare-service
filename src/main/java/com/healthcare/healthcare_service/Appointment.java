package com.healthcare.healthcare_service;


import java.time.LocalDateTime;

public class Appointment {
    private String id;
    private String patientId;
    private String doctor;
    private LocalDateTime time;
    private String status;

    public Appointment() {}
    public Appointment(String id, String patientId, String doctor, LocalDateTime time, String status) {
        this.id = id; this.patientId = patientId; this.doctor = doctor; this.time = time; this.status = status;
    }

    public String getId() { return id; }
    public String getPatientId() { return patientId; }
    public String getDoctor() { return doctor; }
    public LocalDateTime getTime() { return time; }
    public String getStatus() { return status; }

    public void setId(String id) { this.id = id; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public void setDoctor(String doctor) { this.doctor = doctor; }
    public void setTime(LocalDateTime time) { this.time = time; }
    public void setStatus(String status) { this.status = status; }
}
