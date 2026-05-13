package com.example.fatecCarCarona.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.fatecCarCarona.entity.PassageRequestsPipelineStatus;
import com.example.fatecCarCarona.repository.PassageRequestsPipelineStatusRepository;

@Service
public class PassageRequestsPipelineStatusService {

	@Autowired
	private PassageRequestsPipelineStatusRepository repository;

	public PassageRequestsPipelineStatus findByNome(String nome) {
		return repository.findByNome(nome).orElse(null);
	}

	public PassageRequestsPipelineStatus findById(Long id) {
		return repository.findById(id).orElse(null);
	}
}

