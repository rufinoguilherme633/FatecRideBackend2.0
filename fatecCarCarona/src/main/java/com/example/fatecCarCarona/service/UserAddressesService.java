package com.example.fatecCarCarona.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.fatecCarCarona.converter.UserAddressesConversor;
import com.example.fatecCarCarona.dto.OpenstreetmapDTO;
import com.example.fatecCarCarona.dto.UserAddressesDTO;
import com.example.fatecCarCarona.dto.UserAddressesResponseDTO;
import com.example.fatecCarCarona.dto.ViaCepDTO;
import com.example.fatecCarCarona.entity.City;
import com.example.fatecCarCarona.entity.User;
import com.example.fatecCarCarona.entity.UserAddresses;
import com.example.fatecCarCarona.repository.UserAddressesRepository;

@Service
public class UserAddressesService {
	@Autowired
	UserAddressesRepository userAddressesRepository;
	@Autowired
	CityService cityService;
	@Autowired
	OpenstreetmapService openstreetmapService;
	@Autowired
	ViaCepService viaCepService;
	@Autowired
	UserAddressesConversor userAddressesConversor;

	public OpenstreetmapDTO buscar(String local) {

		return openstreetmapService.buscarLocal(local);
	}


	public UserAddresses createUserAddresses(UserAddresses userAddresses) {
		return  userAddressesRepository.save(userAddresses);
	}


	
	public UserAddresses validateUserAddressesViaCep(User user, UserAddressesDTO userAddressesDTO ) {
	    City city = cityService.validateCity(userAddressesDTO.cityId());

	  
	     Optional<ViaCepDTO> viaCepDTO = viaCepService.buscarCep(userAddressesDTO.cep());
		if(viaCepDTO.isEmpty()) {
			throw new ResponseStatusException(
		            HttpStatus.BAD_REQUEST,
		            "CEP destino não encontrado"
		    );

		}
	   

	    String localString =
	    	    userAddressesDTO.logradouro() + " " +
	    	    city.getNome();


	    System.out.println(localString);


	    //OpenstreetmapDTO resultado = buscar(localString);
	  
	    //UserAddresses userAddresses = userAddressesConversor.convertDTOTOUserAddresses(userAddressesDTO,user, city,resultado.lat(),resultado.lon());
	    UserAddresses userAddresses = userAddressesConversor.convertDTOTOUserAddresses(userAddressesDTO,user, city);
	    return userAddresses;

	}



	public UserAddressesDTO cadastrarUserAddresses(UserAddressesDTO userAddressesDTO, User user) {
		UserAddresses address = validateUserAddressesViaCep(user, userAddressesDTO);
        UserAddresses saved = createUserAddresses(address);
        return userAddressesConversor.convertUserAddressesToDTO(saved);
	}



	public UserAddressesResponseDTO getMyAddresses(Long idLong) {
		UserAddresses address = userAddressesRepository.getByUser(idLong);
		UserAddressesResponseDTO userAddressesResponseDTO = new UserAddressesResponseDTO(
				address.getId(),
				address.getCity().getNome(),
				address.getLogradouro(),
				address.getNumero(),
				address.getBairro(),
				address.getCep()
				);
		return userAddressesResponseDTO;
	}


	public UserAddressesDTO updateMyAddresses(Long idLong, Long idAddres,UserAddressesDTO userAddressesDTO ) throws Exception {
		UserAddresses addresses = userAddressesRepository.findById(idAddres)
			    .orElseThrow(() -> new ResponseStatusException(
			            HttpStatus.NOT_FOUND,
			            "Endereço não encontrado"
			    ));

		if(!addresses.getUser().getId().equals(idLong)) {
			throw new ResponseStatusException(
		            HttpStatus.FORBIDDEN,
		            "Esse endereço não pertence ao usuário"
		    );


		}

	    City city = cityService.validateCity(userAddressesDTO.cityId());

	    Optional<ViaCepDTO> viaCepDTO = viaCepService.buscarCep(userAddressesDTO.cep());

		if(viaCepDTO.isEmpty()) {
			 throw new ResponseStatusException(
			            HttpStatus.BAD_REQUEST,
			            "CEP não encontrado"
			    );
			}
		addresses.setCep(city.getNome());
		addresses.setLogradouro(userAddressesDTO.logradouro());
		addresses.setNumero(userAddressesDTO.numero());
		addresses.setBairro(userAddressesDTO.bairro());
		addresses.setCep(userAddressesDTO.cep());

		String localString =
	    	    userAddressesDTO.logradouro() + " " +
	    	    city.getNome();


	    OpenstreetmapDTO resultado = buscar(localString);
	    

	    userAddressesRepository.save(addresses);
	    UserAddressesDTO addressesDTO =  userAddressesConversor.convertUserAddressesToDTO(addresses);
		return addressesDTO;
	}
}
