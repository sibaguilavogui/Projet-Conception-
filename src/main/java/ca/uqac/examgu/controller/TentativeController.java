package ca.uqac.examgu.controller;

import ca.uqac.examgu.model.*;
import ca.uqac.examgu.service.TentativeService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tentatives")
public class TentativeController {

    private final TentativeService service;

    public TentativeController(TentativeService service) {
        this.service = service;
    }


}
