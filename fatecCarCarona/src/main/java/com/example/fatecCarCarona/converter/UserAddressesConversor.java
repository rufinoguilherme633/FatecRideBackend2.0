package com.example.fatecCarCarona.converter;

import org.springframework.stereotype.Component;

import com.example.fatecCarCarona.dto.UserAddressesDTO;
import com.example.fatecCarCarona.entity.City;
import com.example.fatecCarCarona.entity.User;
import com.example.fatecCarCarona.entity.UserAddresses;

@Component
public class UserAddressesConversor {

	public UserAddressesDTO convertUserAddressesToDTO(UserAddresses saved) {

	    return new UserAddressesDTO(
	        saved.getCity().getId(),
	        saved.getLogradouro(),
	        saved.getNumero(),
	        saved.getBairro(),
	        saved.getCep()
	    );
	}


	public UserAddresses convertDTOTOUserAddresses(UserAddressesDTO userAddressesDTO, User user, City city,String latitude, String longitude ) {
	    UserAddresses userAddresses = new UserAddresses();
	    userAddresses.setUser(user);
	    userAddresses.setCity(city);
	    userAddresses.setLogradouro(userAddressesDTO.logradouro());
	    userAddresses.setNumero(userAddressesDTO.numero());
	    userAddresses.setBairro(userAddressesDTO.bairro());
	    userAddresses.setCep(userAddressesDTO.cep());
	    userAddresses.setLatitude(latitude);
	    userAddresses.setLongitude(longitude);

		return userAddresses;
	}
	
	
	public UserAddresses convertDTOTOUserAddresses(UserAddressesDTO userAddressesDTO, User user, City city) {
	    UserAddresses userAddresses = new UserAddresses();
	    userAddresses.setUser(user);
	    userAddresses.setCity(city);
	    userAddresses.setLogradouro(userAddressesDTO.logradouro());
	    userAddresses.setNumero(userAddressesDTO.numero());
	    userAddresses.setBairro(userAddressesDTO.bairro());
	    userAddresses.setCep(userAddressesDTO.cep());


		return userAddresses;
	}



}
