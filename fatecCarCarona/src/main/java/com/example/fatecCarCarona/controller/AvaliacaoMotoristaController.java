package com.example.fatecCarCarona.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fatecCarCarona.dto.ComentarioDTO;
import com.example.fatecCarCarona.dto.Resquest.ComentarioRequestDTO;
import com.example.fatecCarCarona.service.AvaliacaoMotoristaService;
import com.example.fatecCarCarona.service.TokenService;

@RestController
@RequestMapping("/comentar")
public class AvaliacaoMotoristaController {

	  @Autowired
	  AvaliacaoMotoristaService avaliacaoMotoristaService;
	
	  @Autowired
	  private TokenService tokenService;
	  
	  @PostMapping("/{id_solicitacao}") 
	  public void registrarComentarioCarona(@RequestHeader("Authorization") String authHeader , @RequestBody ComentarioRequestDTO comentarioDTO,@PathVariable("id_solicitacao") Long id_solicitacao){
		  Long idLong = tokenService.extractUserIdFromHeader(authHeader);
		  avaliacaoMotoristaService.registrarComentarioCarona(idLong,comentarioDTO,id_solicitacao);
	  }
	  
	  @GetMapping("/{id_motorista}")
	  public double pegarMediaComentarioCarona(@RequestHeader("Authorization") String authHeader,@PathVariable("id_motorista") Long id_motorista){
		  Long idLong = tokenService.extractUserIdFromHeader(authHeader);
		  
		  
		  return avaliacaoMotoristaService.pegarMediaComentarioCarona(idLong,id_motorista);
	    
	  }
}
