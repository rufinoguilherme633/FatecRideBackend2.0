package com.example.fatecCarCarona.service;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.fatecCarCarona.converter.ConversorAvaliacao;
import com.example.fatecCarCarona.dto.ComentarioDTO;
import com.example.fatecCarCarona.dto.Resquest.ComentarioRequestDTO;
import com.example.fatecCarCarona.entity.AvaliacaoMotorista;
import com.example.fatecCarCarona.entity.Comentario;
import com.example.fatecCarCarona.entity.PassageRequests;
import com.example.fatecCarCarona.entity.Ride;
import com.example.fatecCarCarona.entity.User;
import com.example.fatecCarCarona.repository.AvaliacaoMotoristaRepository;
import com.example.fatecCarCarona.repository.PassageRequestsRepository;
import com.example.fatecCarCarona.repository.RideRepository;
import com.example.fatecCarCarona.repository.UserRepository;
@Service
public class AvaliacaoMotoristaService {
	 
	  @Autowired
	  AvaliacaoMotoristaRepository avaliacaoMotoristaRepository;
	  @Autowired
	  UserRepository userRepository;
	  @Autowired
	  RideRepository rideRepository;
	  @Autowired
	  PassageRequestsRepository passageRequestsRepository;
	  @Autowired
	  ConversorAvaliacao conversorAvaliacao;
	  
	  public void registrarComentarioCarona(Long idLong, ComentarioRequestDTO comentarioRequestDTO, Long id_solicitacao){
	    
	    //procurar pra ver se existe 
		  User user = userRepository.findById(idLong).orElseThrow(() -> new ResponseStatusException(
			        HttpStatus.NOT_FOUND, "usuario não encontrado"));
	    //se existir salvar
	    
	    
		  //validar se carona já foi comentada
		  
		  //
		PassageRequests passageRequest = passageRequestsRepository.findById(id_solicitacao).orElseThrow(() -> new ResponseStatusException(
		        HttpStatus.NOT_FOUND, "nenhuma solicitação encontrada"));
		 
		if (!passageRequest.getPassageiro().getId().equals(user.getId())) {
			throw new ResponseStatusException(
				    HttpStatus.FORBIDDEN,
				    "Esta solicitacao não pertence a este passageiro."
				);
		    }
		    
		AvaliacaoMotorista avaliacao = avaliacaoMotoristaRepository
	    	    .findByIdUsuarioMotorista(passageRequest.getCarona().getDriver().getId());
			
		
		if (avaliacao == null) {
			avaliacao = new AvaliacaoMotorista();
			avaliacao.setIdUsuarioMotorista(passageRequest.getCarona().getDriver().getId());
			avaliacao.setComentarios(new ArrayList<>());
			
    	    avaliacaoMotoristaRepository.save(avaliacao);
    	}
		
		for(Comentario i :avaliacao.getComentarios()) {
			
			if(i.getId_pasageiro() != null &&
				    i.getId_carona() != null && i.getId_pasageiro().equals(user.getId()) && i.getId_carona().equals(passageRequest.getCarona().getId())) {
				
				 throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
					        "passageiro ja fez comentario dessa carona.");
			}
			
		}
		

		avaliacao.getComentarios().add(conversorAvaliacao.toEntity(passageRequest.getCarona().getDriver().getId(),user.getId(),comentarioRequestDTO));
		    
	  
	    	avaliacaoMotoristaRepository.save(avaliacao);
	
	    
	  }
	  
	  
	  public double pegarMediaComentarioCarona(Long idLong,Long id_motorista){
		  int somaAvaliacoes = 0;
		  User user = userRepository.findById(idLong).orElseThrow(() -> new RuntimeException("usuario não encontrado"));
		 AvaliacaoMotorista avaliacoes= avaliacaoMotoristaRepository.findByIdUsuarioMotorista(id_motorista);
		
		 if (avaliacoes == null || avaliacoes.getComentarios().isEmpty()) {
			 System.out.println(0);   
			 return 0;
			}
		  for(Comentario i :avaliacoes.getComentarios()) {
			  
			somaAvaliacoes += i.getAvaliacao();  
			  
		  }
		  System.out.println(somaAvaliacoes / avaliacoes.getComentarios().size());
	
	    return (double) somaAvaliacoes / avaliacoes.getComentarios().size();
	    
	  }
}
