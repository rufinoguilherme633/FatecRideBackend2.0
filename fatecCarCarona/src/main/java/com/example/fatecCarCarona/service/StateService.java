package com.example.fatecCarCarona.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.fatecCarCarona.dto.StateDTO;
import com.example.fatecCarCarona.entity.State;
import com.example.fatecCarCarona.repository.StateRepository;
@Service
public class StateService {

	@Autowired
	StateRepository stateRepository;


	public List<StateDTO> getAll() {
		
			  List<State> allStates = stateRepository.findAll();
			  if (allStates.isEmpty()) {
		            throw new ResponseStatusException(
		                    HttpStatus.NOT_FOUND,
		                    "Nenhum estado encontrado"
		            );
		        }

		        return allStates.stream()
		                .map(state -> new StateDTO(
		                        state.getId(),
		                        state.getNome(),
		                        state.getUf(),
		                        state.getIbge(),
		                        state.getPais(),
		                        state.getDdd()
		                ))
		                .collect(Collectors.toList());
		 
	}



	public Optional<State> get(Long id) throws Exception {	
			return validateStateExists(id);
	}


	public Optional<State>  validateStateExists(Long stateId) throws Exception{
		Optional<State> stateExists = stateRepository.findById(stateId);
		if(stateExists.isEmpty()) {
			//throw new Exception("Estado inexistente");
			 throw new ResponseStatusException(
	                    HttpStatus.BAD_REQUEST,
	                    "Estado inexistente"
	            );
		}
		
		return stateExists;

	}

}
