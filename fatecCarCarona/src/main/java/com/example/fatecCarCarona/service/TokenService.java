package com.example.fatecCarCarona.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.example.fatecCarCarona.entity.User;

@Service
public class TokenService {
	@Value("${api.security.token.secret}")
	private String secret;
	public String generateToken(User user) {
		try {
			Algorithm algorithm = Algorithm.HMAC256(secret);

			String token = JWT.create()
					.withIssuer("fatecCarCarona")
					.withSubject(user.getId().toString())
					.withExpiresAt(this.generateExpirationDate())
					.sign(algorithm);
			return token;
		} catch (JWTCreationException e) {
			throw new ResponseStatusException(
	                HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao gerar token de autenticação", e);
		}
	}

	public String validateToken(String token) {
		try {
			Algorithm algorithm = Algorithm.HMAC256(secret);
			return JWT.require(algorithm)
					.withIssuer("fatecCarCarona")
					.build()
					.verify(token)
					.getSubject();
		} catch (JWTVerificationException e) {
			return null;
		}
	}
	private Instant generateExpirationDate() {
		return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
	}


	public Long extractUserIdFromHeader(String authHeader) {

		String token = authHeader.replace("Bearer ","");
		String userId = this.validateToken(token);

		try {
			return Long.parseLong(userId);

		} catch (Exception e) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user ID format", e);
		}



	}
}
