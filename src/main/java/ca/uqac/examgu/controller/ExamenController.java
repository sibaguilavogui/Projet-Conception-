package ca.uqac.examgu.controller;

import ca.uqac.examgu.model.Enseignant;
import ca.uqac.examgu.model.Examen;
import ca.uqac.examgu.service.ExamenService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/examens")
public class ExamenController {

    private final ExamenService service;

    public ExamenController(ExamenService service) {
        this.service = service;
    }

    @PostMapping
    public Examen creer(@RequestBody Examen ex) {
        return service.creer(ex);
    }

    @GetMapping
    public Map<String, Object> testJson() {
        Map<String, Object> res = new HashMap<>();
        res.put("status", "ok");
        res.put("message", "Ceci vient de l'API");
        return res;
    }
}

