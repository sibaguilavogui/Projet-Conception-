package ca.uqac.examgu.controller;

import ca.uqac.examgu.model.*;
import ca.uqac.examgu.service.ExamenService;
import ca.uqac.examgu.service.InscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inscriptions")
public class InscriptionController {
    private final InscriptionService inscriptionService;
    private final ExamenService examenService;

    public InscriptionController(InscriptionService inscriptionService, ExamenService examenService) {
        this.inscriptionService = inscriptionService;
        this.examenService = examenService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<?> getInscriptions(){
        return ResponseEntity.ok(inscriptionService.getInscriptions());
    }


}

