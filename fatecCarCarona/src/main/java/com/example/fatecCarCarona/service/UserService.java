package com.example.fatecCarCarona.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.fatecCarCarona.converter.UserConversor;
import com.example.fatecCarCarona.dto.UserAddressesDTO;
import com.example.fatecCarCarona.dto.UserBaseDTO;
import com.example.fatecCarCarona.dto.UserDTO;
import com.example.fatecCarCarona.dto.UserDriverDTO;
import com.example.fatecCarCarona.dto.VehicleDTO;
import com.example.fatecCarCarona.entity.Course;
import com.example.fatecCarCarona.entity.Gender;
import com.example.fatecCarCarona.entity.User;
import com.example.fatecCarCarona.entity.UserType;
import com.example.fatecCarCarona.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserService {

	@Autowired
	UserRepository userRepository;
	@Autowired
	GenderService genderService;
	@Autowired
	CourseService courseService;
	@Autowired
	UserTypeService userTypeService;
	@Autowired
	UserAddressesService userAddressesService;
	@Autowired
	VehicleService vehicleService;
	@Autowired
	private  PasswordEncoder passwordEncoder;	
	@Autowired
	UserConversor userconversor;


	public void existeEmail(String email,Long id_usuario ) throws Exception {
		Optional<User> existingEmail = userRepository.findByEmail(email);
		if(existingEmail.isPresent() && !existingEmail.get().getId().equals(id_usuario)) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"Email já cadastrado por outro usuário"
				);
		}
	}

	
	public User existUser(long idLong) {
		User user = userRepository.findById(idLong).orElseThrow(() -> new ResponseStatusException(
		        HttpStatus.NOT_FOUND, "usuario não encontrado"));
		return user;
	}
	
	
	public User createUser(User user){
		return userRepository.save(user);
	}


	@Transactional(rollbackOn = Exception.class)
	public UserDriverDTO  cadastrarDrivers(UserDriverDTO userDriverDTO) throws Exception {
			
		User user  = userconversor.convertDtoToUser(userDriverDTO);

		existeEmail(userDriverDTO.email(), null);
		user.setSenha(passwordEncoder.encode(userDriverDTO.senha()));

		if (userDriverDTO.vehicleDTO() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "É preciso cadastrar pelo menos um carro");
           // throw new Exception("É preciso cadastrar pelo menos um carro");
        }
		if (userDriverDTO.userAddressesDTO() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "É preciso cadastrar endereço");
			//throw new Exception("É preciso cadastrar endereço");
        }
		if(user.getUserType().getNome().equalsIgnoreCase("passageiro") ) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário passageiro não pode cadastrar carro");
	    
			//throw new Exception("usuario passageiros não podem cadastrar carros");
		}

			User newUser = this.createUser(user);
			UserAddressesDTO createUserAddresses = userAddressesService.cadastrarUserAddresses(userDriverDTO.userAddressesDTO() ,newUser);

			VehicleDTO vehicleDTO= vehicleService.cadastrarVehehicle(userDriverDTO.vehicleDTO(),newUser);

		    return userconversor.convertUserDriverToUserDriverDto(newUser, createUserAddresses, vehicleDTO);
	}

	
		@Transactional(rollbackOn = Exception.class)
		public UserDTO  cadastrarUser(UserDTO userDTO) throws Exception {
	
			User user  = userconversor.convertDtoToUser(userDTO);
			user.setSenha(passwordEncoder.encode(userDTO.senha()));
	
			if (userDTO.userAddressesDTO() == null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "É preciso cadastrar endereço");
	            //throw new Exception("É preciso cadastrar endereço");
	        }
			if(!user.getUserType().getNome().equalsIgnoreCase("passageiro")) {
	
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "se quiser ser passageiro escolha tipo passageiro");
				//throw new Exception("se quiser ser passageiro escolha tipo passageiro");
			}
	
				User newUser = this.createUser(user);
				UserAddressesDTO createUserAddresses = userAddressesService.cadastrarUserAddresses(userDTO.userAddressesDTO() ,newUser);
	
			    return  userconversor.convertUserToUserDto(newUser, createUserAddresses);
	
		}



	public UserBaseDTO putCarByDriver(Long idLong, UserBaseDTO userBaseDTO) throws Exception {
		
		User user = this.existUser(idLong);
		Course course = courseService.validateCourse(userBaseDTO.courseId());
		Gender gender =  genderService.validateGender(userBaseDTO.genderId());
		UserType userType = userTypeService.validateUserType(userBaseDTO.userTypeId());

		user.setNome(userBaseDTO.nome());
		user.setSobrenome(userBaseDTO.sobrenome());

		existeEmail(userBaseDTO.email(), idLong);
	    user.setEmail(userBaseDTO.email());
	    
		user.setTelefone(userBaseDTO.telefone());
		user.setSenha(userBaseDTO.senha());
		user.setFoto(userBaseDTO.foto());
		user.setUserType(userType);
		user.setGender(gender);
		user.setCourse(course);
		user.setSenha(passwordEncoder.encode(userBaseDTO.senha()));
		createUser(user);

		return userconversor.convertUserToUserBaseDto(user);
	}



	public void deleteUser(Long idLong) throws Exception {
		User user = this.existUser(idLong);
		userRepository.delete(user);
	}


	public UserBaseDTO getUser(Long idLong) throws Exception {
		User user = this.existUser(idLong);
		return userconversor.convertUserToUserBaseDto(user);

	}
}
