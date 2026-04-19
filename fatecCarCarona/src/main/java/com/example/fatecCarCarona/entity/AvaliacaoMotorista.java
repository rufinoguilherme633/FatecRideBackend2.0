package com.example.fatecCarCarona.entity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import org.springframework.data.annotation.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Document(collection  = "avaliacaoMotorista")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AvaliacaoMotorista {
	  @Id
	  private String id;
	  @Field(name = "id_usuario_motorista")
	  private Long idUsuarioMotorista;
	  private List<Comentario> comentarios= new ArrayList<>();;
}
