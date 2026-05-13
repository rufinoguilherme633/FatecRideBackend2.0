package com.example.fatecCarCarona.converter;

import org.springframework.stereotype.Component;

import com.example.fatecCarCarona.dto.DestinationDTO;
import com.example.fatecCarCarona.dto.OriginDTO;
import com.example.fatecCarCarona.dto.RideDTO;
import com.example.fatecCarCarona.entity.Ride;

@Component
public class RideConversor {
	
	public RideDTO convertToRideDTO(Ride ride) {
	    OriginDTO originDTO = new OriginDTO(
	        ride.getOrigin().getCity().getNome(),
	        ride.getOrigin().getLogradouro(),
	        ride.getOrigin().getNumero(),
	        ride.getOrigin().getBairro(),
	        ride.getOrigin().getCep()
	    );

	    DestinationDTO destinationDTO = new DestinationDTO(
	        ride.getDestination().getCity().getNome(),
	        ride.getDestination().getLogradouro(),
	        ride.getDestination().getNumero(),
	        ride.getDestination().getBairro(),
	        ride.getDestination().getCep()
	    );

	    return new RideDTO(
	        originDTO,
	        destinationDTO,
	        ride.getAvailableSeats(),
	        ride.getVehicle().getId()
	    );
	}

}
