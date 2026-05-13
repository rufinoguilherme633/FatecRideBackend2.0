package com.example.fatecCarCarona.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.fatecCarCarona.dto.LoginDTO;
import com.example.fatecCarCarona.dto.LoginReposnseDTO;
import com.example.fatecCarCarona.dto.UserBaseDTO;
import com.example.fatecCarCarona.dto.UserDTO;
import com.example.fatecCarCarona.dto.UserDriverDTO;
import com.example.fatecCarCarona.entity.User;
import com.example.fatecCarCarona.repository.UserRepository;
import com.example.fatecCarCarona.service.TokenService;
import com.example.fatecCarCarona.service.UserService;

@RestController
@RequestMapping("/users")
public class UserController {

	@Autowired
	private UserService userService;

	@Autowired
	UserRepository userRepository;
	@Autowired
	private  PasswordEncoder passwordEncoder;
	@Autowired
	private  TokenService tokenService;

	@PostMapping("/criarMotorista")
	public ResponseEntity<UserDriverDTO> createDriver(@RequestBody UserDriverDTO userDriverDTO) throws Exception{
		UserDriverDTO driver = userService.cadastrarDrivers(userDriverDTO);
		return new ResponseEntity<>(driver,HttpStatus.CREATED);
	}
	
	@PostMapping("/criarPassageiro")
	public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) throws Exception{
		UserDTO user = userService.cadastrarUser(userDTO);
		return new ResponseEntity<>(user,HttpStatus.CREATED);
	}
	
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginDTO body) {
	    User user = this.userRepository.findByEmail(body.email())
	    		.orElseThrow(() -> new ResponseStatusException(
	    		        HttpStatus.NOT_FOUND, "usuario não encontrado"));
	    
	    if(passwordEncoder.matches(body.senha(), user.getSenha())) {
	        String token = tokenService.generateToken(user);
	        
	        // Retornar dados completos do usuário no login
	        return ResponseEntity.ok(new LoginReposnseDTO(
	            user.getNome(), 
	            token,
	            user.getId(),
	            user.getUserType().getId()  // Tipo de usuário
	        ));
	    }
	    
	    return ResponseEntity.badRequest().build();
	}

	@PutMapping
	public ResponseEntity<UserBaseDTO> putCarByDriver(@RequestHeader("Authorization") String authHeader, @RequestBody UserBaseDTO userBaseDTO) throws Exception {
		Long idLong = tokenService.extractUserIdFromHeader(authHeader);
		UserBaseDTO user = userService.putCarByDriver(idLong, userBaseDTO);
		return  ResponseEntity.ok(user);

	}

	@DeleteMapping
	public ResponseEntity<?> deleteUser(@RequestHeader("Authorization") String authHeader) throws Exception{

		Long inLong = tokenService.extractUserIdFromHeader(authHeader);
		userService.deleteUser(inLong);
		return ResponseEntity.ok("Usuario deletado com sucesso");

	}
	@GetMapping
	public ResponseEntity<UserBaseDTO> getUser(@RequestHeader("Authorization") String authHeader) throws Exception{

		Long idLong = tokenService.extractUserIdFromHeader(authHeader);
		UserBaseDTO userBaseDTO = userService.getUser(idLong);


		return ResponseEntity.ok(userBaseDTO);

	}
}
