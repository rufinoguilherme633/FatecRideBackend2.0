package com.example.fatecCarCarona.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Comentario {
	  private String id;
	  private Long id_carona;
	  private int avaliacao;
	  private String texto;
	  private Long id_pasageiro;
}
