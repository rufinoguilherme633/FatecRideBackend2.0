package com.example.fatecCarCarona.dto;

public record NearbyDriversDTO(
		
		Long idCarona,
	    Long idMotorista,

	    String nome,
	    String sobrenome,
	    String email,
	    String telefone,
	    String foto,

	    String genero,
	    String curso,

	    String cidadeOrigem,
	    String logradouroOrigem,
	    String bairroOrigem,
	    Double latitudeOrigem,
	    Double longitudeOrigem,
	    Double distanciaOrigemKm,

	    String cidadeDestino,
	    String logradouroDestino,
	    String bairroDestino,
	    Double latitudeDestino,
	    Double longitudeDestino,

	    String modeloCarro,
	    String marcaCarro,
	    String placaCarro,
	    String corCarro,
	    Integer anoCarro,

	    int vagasDisponiveis,
	    int vagasOcupadas

		) {

}
