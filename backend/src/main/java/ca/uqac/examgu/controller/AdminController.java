package ca.uqac.examgu.controller;

import ca.uqac.examgu.model.*;
import ca.uqac.examgu.model.Enumerations.TypeEvenement;
import ca.uqac.examgu.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/reinitialiser-mot-de-passe")
    public ResponseEntity<?> reinitialiserMotDePasseAdmin(@RequestParam String nouveauMotDePasse) {
        return adminService.reinitialiserMotDePasseAdmin(nouveauMotDePasse);
    }

    @GetMapping
    public ResponseEntity<Admin> getAdminSysteme() {
        return ResponseEntity.ok(adminService.getAdminSingleton());
    }

    @GetMapping("/logs")
    public ResponseEntity<List<LogEntry>> consulterLogs() {
        return ResponseEntity.ok(adminService.consulterLogs());
    }

    @GetMapping("/logs-par-type")
    public ResponseEntity<?> consulterLogsParType(@RequestParam(required = true) TypeEvenement type) {
        return ResponseEntity.ok(adminService.consulterLogsParType(type));
    }

    @GetMapping("/logs-par-user")
    public ResponseEntity<?> consulterLogsParUser(@RequestParam(required = true) String email) {
        return ResponseEntity.ok(adminService.consulterLogsParUtilisateur(email));
    }

}