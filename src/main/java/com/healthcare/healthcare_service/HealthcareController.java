package com.healthcare.healthcare_service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class HealthcareController {

    private static final Logger log = LoggerFactory.getLogger(HealthcareController.class);
    private final Map<String, Patient> patients = new LinkedHashMap<>();
    private final Map<String, Appointment> appts = new LinkedHashMap<>();

    public HealthcareController() {
        Patient p1 = new Patient("P-1001", "Aarav Kumar", 9, "M", "Asthma");
        Patient p2 = new Patient("P-1002", "Saanvi Rao", 6, "F", "Seasonal Allergy");
        patients.put(p1.getId(), p1);
        patients.put(p2.getId(), p2);

        appts.put("A-2001", new Appointment("A-2001", p1.getId(), "Dr. Nair",
                LocalDateTime.now().plusDays(1), "SCHEDULED"));
    }

    @GetMapping("/patients")
    public Collection<Patient> allPatients() {
        log.info("Fetching all patients");
        return patients.values();
    }

    @GetMapping("/patients/{id}")
    public Patient onePatient(@PathVariable String id) {
        log.info("Fetching patient by id={}", id);
        Patient p = patients.get(id);
        if (p == null) {
            log.warn("Patient not found id={}", id);
            throw new NoSuchElementException("Patient not found: " + id);
        }
        return p;
    }

    @GetMapping("/appointments")
    public Collection<Appointment> allAppointments() {
        log.info("Listing appointments");
        return appts.values();
    }

    @PostMapping("/appointments")
    public Appointment createAppointment(@RequestBody Map<String, String> body) {
        String patientId = body.getOrDefault("patientId", "UNKNOWN");
        String doctor = body.getOrDefault("doctor", "Dr. House");
        LocalDateTime time = LocalDateTime.now().plusDays(2);

        String id = "A-" + (2000 + appts.size() + 1);
        Appointment a = new Appointment(id, patientId, doctor, time, "SCHEDULED");
        appts.put(id, a);

        log.info("Created appointment id={} patientId={} doctor={} time={}", id, patientId, doctor, time);
        return a;
    }

    @GetMapping("/simulate-error")
    public Map<String, Object> simulateError() {
        try {
            log.info("Simulating error path");
            throw new IllegalStateException("Simulated backend failure");
        } catch (Exception e) {
            log.error("Error while processing simulate-error endpoint", e);
            Map<String, Object> m = new HashMap<>();
            m.put("status", "ERROR");
            m.put("message", e.getMessage());
            return m;
        }
    }
}
