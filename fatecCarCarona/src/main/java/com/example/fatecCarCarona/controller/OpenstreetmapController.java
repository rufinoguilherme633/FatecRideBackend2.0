package com.example.fatecCarCarona.controller;


import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.fatecCarCarona.dto.OpenstreetmapDTO;
import com.example.fatecCarCarona.service.OpenstreetmapService;

@RestController
@RequestMapping
public class OpenstreetmapController {

    @Autowired
    OpenstreetmapService openstreetmapService;


    @GetMapping("/local")
    public ResponseEntity<OpenstreetmapDTO> buscarLocal(@RequestParam String local) {
        OpenstreetmapDTO resultado = openstreetmapService.buscarLocal(local);
        return ResponseEntity.ok(resultado);
    }
}
