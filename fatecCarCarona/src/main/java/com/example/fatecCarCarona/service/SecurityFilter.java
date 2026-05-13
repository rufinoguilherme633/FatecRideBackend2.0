package com.example.fatecCarCarona.service;

import java.io.IOException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.fatecCarCarona.entity.User;
import com.example.fatecCarCarona.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityFilter extends OncePerRequestFilter {

	@Autowired
	TokenService tokenService;
	@Autowired
	UserRepository userRepository;
	@Override
	protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response, FilterChain filterChain)throws ServletException, IOException  {


		var token = this.recoverToken(request);
	       var login = tokenService.validateToken(token);

	       if(login != null) {
	    	   User user = userRepository.findById(Long.parseLong(login)).orElseThrow(() -> new RuntimeException("User Not Found"));

	    	   var authories = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
	    	   var authentication = new UsernamePasswordAuthenticationToken(user, null,authories);
	    	   SecurityContextHolder.getContext().setAuthentication(authentication);
	       }

	       filterChain.doFilter(request, response);
	}

	private String recoverToken(HttpServletRequest request) {
		var authHeader = request.getHeader("Authorization");
		if(authHeader != null && !authHeader.isBlank()) {
			return authHeader.replace("Bearer ", "");
		}
		// Fallback: accept token as query param (useful for EventSource browsers)
		var tokenParam = request.getParameter("token");
		if(tokenParam != null && !tokenParam.isBlank()) {
			return tokenParam;
		}
		return null;
	}

}
