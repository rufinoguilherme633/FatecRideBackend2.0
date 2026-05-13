package com.example.fatecCarCarona.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.fatecCarCarona.entity.Destination;
import com.example.fatecCarCarona.repository.DestinationRepository;

@Service
public class DestinationService {
	@Autowired
	DestinationRepository destinationRepository;

	public Destination createDestination(Destination destination) {
		return destinationRepository.save(destination);
	}

	public Destination findById(Long id) {
		// TODO Auto-generated method stub
		return destinationRepository.findById(id)
				 .orElseThrow(() -> new ResponseStatusException(
	                        HttpStatus.NOT_FOUND,
	                        "Destino não encontrado"
	                ));
	}
}
