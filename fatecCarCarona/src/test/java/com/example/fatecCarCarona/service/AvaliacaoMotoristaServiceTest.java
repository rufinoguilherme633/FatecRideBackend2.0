package com.example.fatecCarCarona.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;

import com.example.fatecCarCarona.entity.AvaliacaoMotorista;
import com.example.fatecCarCarona.entity.Comentario;
import com.example.fatecCarCarona.entity.Course;
import com.example.fatecCarCarona.entity.Gender;
import com.example.fatecCarCarona.entity.User;
import com.example.fatecCarCarona.entity.UserType;
import com.example.fatecCarCarona.repository.AvaliacaoMotoristaRepository;
import com.example.fatecCarCarona.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AvaliacaoMotoristaServiceTest {

	
	
	@Mock
	UserRepository userRepository;
	
	@Mock
	AvaliacaoMotoristaRepository avaliacaoMotoristaRepository;

	
	
	@InjectMocks
	AvaliacaoMotoristaService avaliacaoMotoristaService;
	
	@Test
	@Description("Verify media of commets about ")
	void deveCalcularMediaDosComentarios() {
		
		
		 User user = new User();
		    user.setId(1L);

		    when(userRepository.findById(1L))
		        .thenReturn(Optional.of(user));
		    
		Comentario c1 = new Comentario("1", 1L, 5, "bom", 1L);
        Comentario c2 = new Comentario("2", 1L, 3, "ok", 2L);
        List<Comentario> comentarios = new ArrayList<>();
        comentarios.add(c1);
        comentarios.add(c2);
        
        
		AvaliacaoMotorista newavaliacaoMotorista = new AvaliacaoMotorista();
		newavaliacaoMotorista.setIdUsuarioMotorista(1L);
		newavaliacaoMotorista.setComentarios(comentarios);
		
		
		when(avaliacaoMotoristaRepository.findByIdUsuarioMotorista(1l)).thenReturn(newavaliacaoMotorista);
		double media = avaliacaoMotoristaService.pegarMediaComentarioCarona(1l, 1l);
		
		assertEquals(4.0, media);
		
		
	}
	
	
	
	@Test
	@Description("Verify media of commets about ")
	void deveCalcularMediaDosComentariosETerazerSemComentrios() {
		
		
		 User user = new User();
		    user.setId(1L);

		    when(userRepository.findById(1L))
		        .thenReturn(Optional.of(user));
		    
		
        List<Comentario> comentarios = new ArrayList<>();
        
        
        
		AvaliacaoMotorista newavaliacaoMotorista = new AvaliacaoMotorista();
		newavaliacaoMotorista.setIdUsuarioMotorista(1L);
		newavaliacaoMotorista.setComentarios(comentarios);
		
		
		when(avaliacaoMotoristaRepository.findByIdUsuarioMotorista(1l)).thenReturn(newavaliacaoMotorista);
		double media = avaliacaoMotoristaService.pegarMediaComentarioCarona(1l, 1l);
		
		assertEquals(0.0, media);
		
		
	}

}
