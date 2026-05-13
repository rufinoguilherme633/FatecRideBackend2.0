package com.example.fatecCarCarona.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.fatecCarCarona.dto.OpenstreetmapDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OpenstreetmapService {
	 private final String baseUrl = "https://nominatim.openstreetmap.org/search?q=";
	    public OpenstreetmapDTO buscarLocal(String local) {
	        try {
	            String localEncoded = URLEncoder.encode(local, StandardCharsets.UTF_8);
	            String urlString = baseUrl + localEncoded + "&format=json&addressdetails=1"; 
	            URL url = new URL(urlString);

	            HttpURLConnection conexao = (HttpURLConnection) url.openConnection();
	            conexao.setRequestMethod("GET");
	            conexao.setRequestProperty("User-Agent", "fatec-carona-app");
	            if (conexao.getResponseCode() != 200) {
	            	throw new ResponseStatusException(
	                        HttpStatus.BAD_GATEWAY,
	                        "Erro ao consultar serviço de geolocalização"
	                );
	            }

	            BufferedReader br = new BufferedReader(new InputStreamReader(conexao.getInputStream()));
	            StringBuilder retorno = new StringBuilder();
	            String line;

	            while ((line = br.readLine()) != null) {
	                retorno.append(line);
	            }

	            ObjectMapper objectMapper = new ObjectMapper();
	            OpenstreetmapDTO[] resultados = objectMapper.readValue(retorno.toString(), OpenstreetmapDTO[].class);

	            if (resultados.length > 0) {
	            	
	            	return resultados[0]; // retorna o primeiro resultado
	            } else {
	            	throw new ResponseStatusException(
	            	        HttpStatus.NOT_FOUND,
	            	        "Endereço não encontrado"
	            	);
	            }

	        } catch (ResponseStatusException e) {
	            throw e; // mantém o erro original
	        }
	        catch (Exception e) {
	            throw new ResponseStatusException(
	                HttpStatus.BAD_GATEWAY,
	                "Erro ao buscar endereço externo"
	            );
	        }
	    }
}
