	package com.example.fatecCarCarona.service;

	import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.fatecCarCarona.dto.ViaCepDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

	@Service
	public class ViaCepService {


		private static final String baseUrl = "https://viacep.com.br/ws/";

		public Optional<ViaCepDTO> buscarCep(String cep){

		try {
			String urlString = baseUrl+ cep + "/json";
			URL url = new URL(urlString);

			HttpURLConnection conexao = (HttpURLConnection) url.openConnection();
	        conexao.setRequestMethod("GET");
	        if (conexao.getResponseCode() != 200) {
	        	throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CEP não encontrado: " + cep);
	        	//return Optional.empty();
	        }


	        BufferedReader br = new BufferedReader(new InputStreamReader(conexao.getInputStream()));
            StringBuilder retorno = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                retorno.append(line);
            }

            br.close();

			String json = retorno.toString();

			// ViaCEP returns 200 with {"erro": true} when CEP is not found.
			if (json.contains("\"erro\"")) {
				// include the raw response to help debugging when a CEP is not found
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CEP não encontrado: " + cep + " - viaCep response: " + json);
			}

			com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
			// Ignore unknown properties just in case the API returns extra fields
			objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			ViaCepDTO resultado = objectMapper.readValue(json, ViaCepDTO.class);

			return Optional.ofNullable(resultado);

        } catch (Exception e) {
        	//throw new RuntimeException("Erro ao buscar endereço: " + e.getMessage(), e);
        	
        	throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Erro ao buscar endereço: " + e.getMessage(), e);
        }
    }


	}
