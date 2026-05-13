package com.example.fatecCarCarona.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
import com.example.fatecCarCarona.service.CourseService;
import com.example.fatecCarCarona.service.GenderService;
import com.example.fatecCarCarona.service.UserAddressesService;
import com.example.fatecCarCarona.service.UserTypeService;
import com.example.fatecCarCarona.service.VehicleService;


// 74 - 155
@Component
public class UserConversor {
	
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

	public User convertDtoToUser(UserDriverDTO userDriverDTO) {
		Course course = courseService.validateCourse(userDriverDTO.courseId());
		Gender gender =  genderService.validateGender(userDriverDTO.genderId());
		UserType userType = userTypeService.validateUserType(userDriverDTO.userTypeId());
		User user = new User();
		user.setNome(userDriverDTO.nome());
		user.setSobrenome(userDriverDTO.sobrenome());
		user.setEmail(userDriverDTO.email());
		user.setSenha(userDriverDTO.senha());
		user.setTelefone(userDriverDTO.telefone());
		user.setFoto(userDriverDTO.foto());
		user.setUserType(userType);
		user.setGender(gender);
		user.setCourse(course);
		return user;
	}

	public User convertDtoToUser(UserDTO userDTO) {
		Course course = courseService.validateCourse(userDTO.courseId());
		Gender gender =  genderService.validateGender(userDTO.genderId());
		UserType userType = userTypeService.validateUserType(userDTO.userTypeId());
		User user = new User();
		user.setNome(userDTO.nome());
		user.setSobrenome(userDTO.sobrenome());
		user.setEmail(userDTO.email());
		user.setSenha(userDTO.senha());
		user.setTelefone(userDTO.telefone());
		user.setFoto(userDTO.foto());
		user.setUserType(userType);
		user.setGender(gender);
		user.setCourse(course);

		return user;
	}

	public UserDriverDTO convertUserDriverToUserDriverDto(User user,UserAddressesDTO userAddressesDTO, VehicleDTO  vehicleDTO) {
	    return new UserDriverDTO(
	        user.getNome(),
	        user.getSobrenome(),
	        user.getEmail(),
	        user.getSenha(),
	        user.getTelefone(),
	        user.getFoto(),
	        user.getUserType().getId(),
	        user.getGender().getId(),
	        user.getCourse().getId(),
	        userAddressesDTO,
	        vehicleDTO

	    );
	}

	public UserDTO convertUserToUserDto(User user,UserAddressesDTO userAddressesDTO) {
	    return new UserDTO(
	        user.getNome(),
	        user.getSobrenome(),
	        user.getEmail(),
	        user.getSenha(),
	        user.getTelefone(),
	        user.getFoto(),
	        user.getUserType().getId(),
	        user.getGender().getId(),
	        user.getCourse().getId(),
	        userAddressesDTO
	    );
	}

	public UserBaseDTO convertUserToUserBaseDto(User user) {
	    return new UserBaseDTO(
	        user.getNome(),
	        user.getSobrenome(),
	        user.getEmail(),
	        user.getSenha(),
	        user.getTelefone(),
	        user.getFoto(),
	        user.getUserType().getId(),
	        user.getGender().getId(),
	        user.getCourse().getId()

	    );
	}


	
}
