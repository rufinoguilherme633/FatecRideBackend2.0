package com.example.fatecCarCarona.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.fatecCarCarona.dto.GenderDTO;
import com.example.fatecCarCarona.entity.Gender;
import com.example.fatecCarCarona.repository.GenderRepository;

@Service
public class GenderService {
	@Autowired
	GenderRepository genderRepository;

	public Gender validateGender(long id) {
		//return genderRepository.findById(id).orElseThrow(() -> new RuntimeException("Tipo de sexo não cadastrado"));
		return genderRepository.findById(id)
		.orElseThrow(() -> new ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"Tipo de sexo não cadastrado"
			));
}
	public List<GenderDTO> allGender() {
	    List<Gender> genders = genderRepository.findAll();
	    List<GenderDTO> genderDTO = genders.stream()
	        .map(gender -> new GenderDTO(gender.getId(), gender.getName()))
	        .collect(Collectors.toList());

	    return genderDTO;
	}

}
