package com.example.fatecCarCarona.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.fatecCarCarona.converter.VehicleConversor;
import com.example.fatecCarCarona.dto.VehicleDTO;
import com.example.fatecCarCarona.dto.VehicleResponseDTO;
import com.example.fatecCarCarona.entity.User;
import com.example.fatecCarCarona.entity.Vehicle;
import com.example.fatecCarCarona.repository.UserRepository;
import com.example.fatecCarCarona.repository.VehicleRepository;

@Service
public class VehicleService {
	@Autowired
	VehicleRepository vehicleRepository;
	@Autowired
	TokenService tokenService;
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	VehicleConversor vehicleConversor;

	private User existUser(Long idLong) {
        return userRepository.findById(idLong)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }
	
	public void validatePlacaExists(String placa,Long id_usuario) throws Exception {

            Optional<Vehicle> existingVehicleByPlaca = vehicleRepository.findByPlaca(placa);

            if (existingVehicleByPlaca.isPresent() && !existingVehicleByPlaca.get().getUser().getId().equals(id_usuario)) {
            	//throw new Exception("Placa já cadastrada por outro usuário");
            	throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Placa já cadastrada por outro usuário"
                );
            }
	}



		public void validateUserIsVehicleOwner(Long idLong, Long id_veiculo) throws Exception {
			Vehicle vehicle = vehicleRepository.findById(id_veiculo).orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Veículo não encontrado"
            ));

			if(!vehicle.getUser().getId().equals(idLong)) {
				throw new ResponseStatusException(
	                    HttpStatus.FORBIDDEN,
	                    "Esse veículo não pertence ao usuário"
	            );
			}
			
		}

	public Vehicle createVehicle(Vehicle vehicle) {
		return  vehicleRepository.save(vehicle);
	}


	public VehicleDTO cadastrarVehehicle( VehicleDTO vehicleDTO, User user) {
		if(vehicleDTO.vagas_disponiveis() <= 0) {
			//throw new RuntimeException("Vagas não podem ser iguais ou menor a 0");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário passageiro não pode cadastrar carro");

		}
		Vehicle convertDtoToVehicle = vehicleConversor.convertDtoToVehicle(vehicleDTO , user);
		Vehicle save =  createVehicle(convertDtoToVehicle);
		return vehicleConversor.convertVehicleToDTO(save);

	}

	public List<VehicleResponseDTO> getAllCarsByDriver(Long idLong) {

		User user = existUser(idLong);
		List<Vehicle> vehicles = vehicleRepository.findAllByUser(user);

		if (vehicles.isEmpty()) {
		        //throw new RuntimeException("Nenhum veículo encontrado para este motorista.");
		        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum veículo encontrado para este motorista.");
		    }
		List<VehicleResponseDTO> vehiclesDTO = new ArrayList<>();

		 for(Vehicle v : vehicles) {
			 vehiclesDTO.add(vehicleConversor.convertVehicleToReponseDTO(v));
		 }

		return vehiclesDTO;
	}

	public VehicleDTO getCarByDriver(Long idLong, Long id_veiculo) throws Exception {
		validateUserIsVehicleOwner(idLong, id_veiculo);

        Vehicle vehicle = vehicleRepository.findById(id_veiculo).get();

        return vehicleConversor.convertVehicleToDTO(vehicle);
	}

	public VehicleDTO putCarByDriver(Long idLong, Long id_veiculo,VehicleDTO vehicleDTO) throws Exception {
		validateUserIsVehicleOwner(idLong, id_veiculo);

        Vehicle vehicle = vehicleRepository.findById(id_veiculo).get();

		validatePlacaExists(vehicleDTO.placa() ,idLong);
		
		vehicle.setPlaca(vehicleDTO.placa());

	
		if(vehicleDTO.vagas_disponiveis() <= 0) {
			 throw new ResponseStatusException(
	                    HttpStatus.BAD_REQUEST,
	                    "Vagas não podem ser menor ou igual a 0"
	            );
		}
		
		vehicle.setModelo(vehicleDTO.modelo());
		vehicle.setMarca(vehicleDTO.marca());
		vehicle.setCor(vehicleDTO.cor());
		vehicle.setAno(vehicleDTO.ano());
		vehicle.setAvailableSeats(vehicleDTO.vagas_disponiveis());

		createVehicle(vehicle);

		return vehicleConversor.convertVehicleToDTO(vehicle);
	}

	public VehicleDTO postCarByDriver(Long idLong, VehicleDTO vehicleDTO) throws Exception {
		User user = existUser(idLong);
		
		if(user.getUserType().getNome().equals("passageiro")) {
			throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Passageiros não podem cadastrar veículos"
            );
		}
		if(vehicleDTO.vagas_disponiveis() <= 0) {
			 throw new ResponseStatusException(
	                    HttpStatus.BAD_REQUEST,
	                    "Vagas não podem ser menor ou igual a 0"
	            );
		}
		
		
		VehicleDTO newVehicleDTO =  cadastrarVehehicle(vehicleDTO,user);
		return newVehicleDTO;
	}


	public void deleteCarByDriver(Long idLong, Long id_veiculo) throws Exception {
		 validateUserIsVehicleOwner(idLong, id_veiculo);
		 Vehicle vehicle = vehicleRepository.findById(id_veiculo).get();
	        vehicleRepository.delete(vehicle);
	}
}
