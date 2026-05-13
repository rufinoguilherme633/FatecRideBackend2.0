package com.example.fatecCarCarona.converter;

import org.springframework.stereotype.Component;

import com.example.fatecCarCarona.dto.VehicleDTO;
import com.example.fatecCarCarona.dto.VehicleResponseDTO;
import com.example.fatecCarCarona.entity.User;
import com.example.fatecCarCarona.entity.Vehicle;

@Component
public class VehicleConversor {

	public Vehicle convertDtoToVehicle(VehicleDTO vehicleDTO, User user) {
		Vehicle  vehicle = new  Vehicle ();
		vehicle.setUser(user);
		vehicle.setModelo(vehicleDTO.modelo());
		vehicle.setMarca(vehicleDTO.marca());
		vehicle.setPlaca(vehicleDTO.placa());
		vehicle.setCor(vehicleDTO.cor());
		vehicle.setAno(vehicleDTO.ano());
		vehicle.setAvailableSeats(vehicleDTO.vagas_disponiveis());

		return vehicle;
	}

		public VehicleDTO convertVehicleToDTO(Vehicle vehicle) {
			 return new VehicleDTO(
					 vehicle.getModelo(),
					 vehicle.getMarca(),
					 vehicle.getPlaca(),
					 vehicle.getCor(),
					 vehicle.getAno(),
					 vehicle.getAvailableSeats()
		         );
		}
		public VehicleResponseDTO convertVehicleToReponseDTO(Vehicle vehicle) {

			 return new VehicleResponseDTO(
					 vehicle.getId(),
					 vehicle.getModelo(),
					 vehicle.getMarca(),
					 vehicle.getPlaca(),
					 vehicle.getCor(),
					 vehicle.getAno(),
					 vehicle.getAvailableSeats()
		         );
		}


}
