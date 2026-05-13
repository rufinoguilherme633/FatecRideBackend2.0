package com.example.fatecCarCarona.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "enderecos_usuario")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserAddresses {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_endereco")
     private Long id;

	@OneToOne
	@JoinColumn(name="id_usuario")
	private User user;

	//@OneToOne(cascade = CascadeType.ALL)
	@ManyToOne
	@JoinColumn(name="id_cidade")
	private City city;

	private String logradouro;
	private String numero;
	private String bairro;
	private String cep;
	private String latitude;
	private String longitude;





}
