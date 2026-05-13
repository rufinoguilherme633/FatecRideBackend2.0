package com.example.fatecCarCarona.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.fatecCarCarona.dto.UserTypeDTO;
import com.example.fatecCarCarona.entity.UserType;
import com.example.fatecCarCarona.repository.UserTypeRepository;
@Service
public class UserTypeService {
	@Autowired
	UserTypeRepository userTypeRepository;


	public UserType validateUserType(long id) {
		//return userTypeRepository.findById(id).orElseThrow(() -> new RuntimeException("Tipo de Usuario não cadastrado"));
		
		return userTypeRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
				HttpStatus.NOT_FOUND,"Tipo de Usuario não cadastrado"));
}

	public List<UserTypeDTO> allUserType(){
		List<UserType> allUserType = userTypeRepository.findAll();
		List<UserTypeDTO> allUserTypeDTOs = allUserType.stream()
				.map(userType -> new UserTypeDTO(userType.getId(), userType.getNome()))
				.collect(Collectors.toList());

		return allUserTypeDTOs;

	}

}
