package com.example.fatecCarCarona.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Update;

import com.example.fatecCarCarona.entity.AvaliacaoMotorista;
import com.example.fatecCarCarona.entity.Comentario;

public interface AvaliacaoMotoristaRepository extends MongoRepository<AvaliacaoMotorista,String>{
	  
	AvaliacaoMotorista findByIdUsuarioMotorista(Long long1);
	  
}
