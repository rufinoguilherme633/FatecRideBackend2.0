package com.example.fatecCarCarona.converter;

import org.springframework.stereotype.Component;

import com.example.fatecCarCarona.dto.ComentarioDTO;
import com.example.fatecCarCarona.dto.Resquest.ComentarioRequestDTO;
import com.example.fatecCarCarona.entity.Comentario;

@Component
public class ConversorAvaliacao {
	  public ComentarioDTO toDTO(Comentario comentario,Long id_carona ){
		    
		    ComentarioDTO comentarioDTO = new ComentarioDTO(
		    		comentario.getId_carona(),
		    		comentario.getTexto(),
		    		comentario.getAvaliacao(),
		    		comentario.getId_pasageiro()
		    );
		
		    
		    return comentarioDTO;
		  }
		  
		  public Comentario toEntity(Long id_carona, Long id_pasageiro,ComentarioRequestDTO comentarioDTO){
		    Comentario comentario = new Comentario();
		    comentario.setId_carona(id_carona);
		    comentario.setAvaliacao(comentarioDTO.avaliacao()); 
		    comentario.setTexto(comentarioDTO.texto());
		    comentario.setId_pasageiro(id_pasageiro);
		    return comentario;
		  }
}
