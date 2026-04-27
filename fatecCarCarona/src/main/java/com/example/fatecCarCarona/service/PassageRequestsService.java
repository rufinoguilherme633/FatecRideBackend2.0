package com.example.fatecCarCarona.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.fatecCarCarona.dto.CompletedPassengerRequestDTO;
import com.example.fatecCarCarona.dto.DestinationDTO;
import com.example.fatecCarCarona.dto.DestinationResponseDTO;
import com.example.fatecCarCarona.dto.NearbyDriversDTO;
import com.example.fatecCarCarona.dto.OpenstreetmapDTO;
import com.example.fatecCarCarona.dto.OriginDTO;
import com.example.fatecCarCarona.dto.OriginResponseDTO;
import com.example.fatecCarCarona.dto.PassageRequestsDTO;
import com.example.fatecCarCarona.dto.PassengerSearchRequest;
import com.example.fatecCarCarona.dto.PendingPassengerRequestDTO;
import com.example.fatecCarCarona.dto.RouteCoordinatesDTO;
import com.example.fatecCarCarona.dto.ViaCepDTO;
import com.example.fatecCarCarona.entity.City;
import com.example.fatecCarCarona.entity.Destination;
import com.example.fatecCarCarona.entity.Origin;
import com.example.fatecCarCarona.entity.PassageRequests;
import com.example.fatecCarCarona.entity.Ride;
import com.example.fatecCarCarona.entity.User;
import com.example.fatecCarCarona.repository.PassageRequestsRepository;
import com.example.fatecCarCarona.repository.RideRepository;
import com.example.fatecCarCarona.repository.UserRepository;

//import com.example.fatecCarCarona.exception.RideException;
import jakarta.transaction.Transactional;

@Service
public class PassageRequestsService {
	
	@Autowired 
	OpenstreetmapService openstreetmapService;
	@Autowired
	FindNearbyDrivers findNearbyDrivers;
	@Autowired
	RideRepository rideRepository;
	@Autowired
	ViaCepService viaCepService;
	@Autowired
	CityService cityService;
	@Autowired
	OriginService originService;
	@Autowired
	DestinationService destinationService;
	@Autowired
	UserRepository userRepository;
	@Autowired
	PassageRequestsStatusService passageRequestsStatusService;
	@Autowired
	PassageRequestsRepository passageRequestsRepository;
	protected Optional<OpenstreetmapDTO> buscarLocalizacao(String endereco) throws Exception {

		Optional<OpenstreetmapDTO> resultado = openstreetmapService.buscarLocal(endereco);

		if (resultado.isEmpty()) {
			throw new Exception("Endereço não encontrado no OpenStreetMap: " + endereco);
		}

		return resultado;
	}
	protected void validateAddress(String cep, String cidade, String logradouro, String bairro) {
		Optional<ViaCepDTO> viaCepDTO = viaCepService.buscarCep(cep);

		if (viaCepDTO.isEmpty()) {
			//throw new RideException("CEP não encontrado: " + cep);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND,"CEP não encontrado: " + cep);
		}

		//boolean isValid = viaCepDTO.get().localidade().equals(cidade) &&
		//				 viaCepDTO.get().logradouro().equals(logradouro) &&
		//				 viaCepDTO.get().bairro().equals(bairro);

		//if (!isValid) {
		//	throw new RideException("Endereço não corresponde ao CEP informado");
		//}
	}
	
	protected Origin criarOrigem(OriginDTO originDTO, City cidade, OpenstreetmapDTO localizacao) {
		Origin origem = new Origin();
		origem.setCity(cidade);
		origem.setLogradouro(originDTO.logradouro());
		origem.setNumero(originDTO.numero());
		origem.setBairro(originDTO.bairro());
		origem.setCep(originDTO.cep());
		origem.setLatitude(Double.parseDouble(localizacao.lat()));
		origem.setLongitude(Double.parseDouble(localizacao.lon()));
		return origem;
	}

	protected Destination criarDestino(DestinationDTO destinationDTO, City cidade, OpenstreetmapDTO localizacao) {
		Destination destino = new Destination();
		destino.setCity(cidade);
		destino.setLogradouro(destinationDTO.logradouro());
		destino.setNumero(destinationDTO.numero());
		destino.setBairro(destinationDTO.bairro());
		destino.setCep(destinationDTO.cep());
		destino.setLatitude(Double.parseDouble(localizacao.lat()));
		destino.setLongitude(Double.parseDouble(localizacao.lon()));
		return destino;
	}
	public List<NearbyDriversDTO> findNearbyDrivers(PassengerSearchRequest passengerSearchRequest) throws Exception{
		/* Optional<OpenstreetmapDTO> origem = buscarLocalizacao(passengerSearchRequest.ruaOrigem());
		Optional<OpenstreetmapDTO> destino = buscarLocalizacao(passengerSearchRequest.ruaDestino());
		
		 List<NearbyDriversDTO> motoristasProximos = findNearbyDrivers.NearbyDriversService(new RouteCoordinatesDTO(
				 Double.parseDouble(origem.get().lat()),
				 Double.parseDouble(origem.get().lon()),
				 Double.parseDouble(destino.get().lat()),
				 Double.parseDouble(destino.get().lon())
				 ));*/
		 List<NearbyDriversDTO> motoristasProximos = findNearbyDrivers.NearbyDriversService(new RouteCoordinatesDTO(
				 passengerSearchRequest.latitudeOrigem(),
				 passengerSearchRequest.longitudeOrigem(),
				 passengerSearchRequest.latitudeDestino(),
				 passengerSearchRequest.longitudeDestino()
				 ));
		
		return motoristasProximos;
		
	}
	
	
	@Transactional(rollbackOn = Exception.class)
	public PassageRequestsDTO create(PassageRequestsDTO passageRequests, Long idLong) throws Exception {
		User user = userRepository.findById(idLong).orElseThrow(() -> new RuntimeException("usuario não encontrado"));

		System.out.println(passageRequests.id_carona());
		
		Ride ride = rideRepository.findById(passageRequests.id_carona())
		        .orElseThrow(() -> new RuntimeException("Carona não encontrada"));
		
		validateAddress(passageRequests.originDTO().cep(), passageRequests.originDTO().cidade(),
				   passageRequests.originDTO().logradouro(), passageRequests.originDTO().bairro());
		
		validateAddress(passageRequests.destinationDTO().cep(), passageRequests.destinationDTO().cidade(),
					   passageRequests.destinationDTO().logradouro(), passageRequests.destinationDTO().bairro());
	
		City cidadeOrigem = cityService.validateCity(passageRequests.originDTO().cidade());
		
		City cidadeDestino = cityService.validateCity(passageRequests.destinationDTO().cidade());
	
		String enderecoOrigem = String.format("%s %s", passageRequests.originDTO().logradouro(), cidadeOrigem.getNome());
		String enderecoDestino = String.format("%s %s", passageRequests.destinationDTO().logradouro(), cidadeDestino.getNome());
	
		OpenstreetmapDTO localizacaoOrigem = buscarLocalizacao(enderecoOrigem).get();
		OpenstreetmapDTO localizacaoDestino = buscarLocalizacao(enderecoDestino).get();
	
		Origin origem = criarOrigem(passageRequests.originDTO(), cidadeOrigem, localizacaoOrigem);
		Destination destino = criarDestino(passageRequests.destinationDTO(), cidadeDestino, localizacaoDestino);
	
		Origin origemSalva = originService.createOrigin(origem);
		Destination destinoSalvo = destinationService.createDestination(destino);

		PassageRequests newPassageRequests = new PassageRequests();
		newPassageRequests.setCarona(ride);
		newPassageRequests.setPassageiro(user);
		newPassageRequests.setOrigin(origemSalva);
		newPassageRequests.setDestination(destino);
		newPassageRequests.setDataHora(LocalDateTime.now());
		newPassageRequests.setStatus(passageRequestsStatusService.findByNome("pendente"));
		
		newPassageRequests = passageRequestsRepository.save(newPassageRequests);
		
	    OriginDTO originDTO = new OriginDTO(
	    		origem.getCity().getNome(),
	    		origem.getLogradouro(),
	    		origem.getNumero(),
	    		origem.getBairro(),
	    		origem.getCep()
		    );

		    DestinationDTO destinationDTO = new DestinationDTO(
		    	destino.getCity().getNome(),
		    	destino.getLogradouro(),
		    	destino.getNumero(),
		    	destino.getBairro(),
		    	destino.getCep()
		    );
		
		PassageRequestsDTO passageRequestsCreate = new PassageRequestsDTO(
				originDTO,
				destinationDTO,
				ride.getId()
				);
		
		return passageRequestsCreate;
	}
	
	public void cancelar(Long userId, Long id_solicitacao) {
		PassageRequests passageRequest = passageRequestsRepository.findById(id_solicitacao).orElseThrow(() -> new RuntimeException("nenhuma solicitação encontrada"));
		
		
		passageRequest.setStatus(passageRequestsStatusService.findByNome("cancelada"));
		passageRequestsRepository.save(passageRequest);
		 
	}
	public Page<CompletedPassengerRequestDTO> buscarSolicitacoesConcluidas(Long userId, int page, int size) {
	    Page<PassageRequests> paginaDeSolicitacoes = passageRequestsRepository.findPassagerFinalizadas(
	        userId,
	        PageRequest.of(page, size)
	    );

	    if (paginaDeSolicitacoes.isEmpty()) {
	        return new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
	    }

	    List<CompletedPassengerRequestDTO> dtos = paginaDeSolicitacoes.getContent().stream().map(p -> {
	        OriginResponseDTO originDTO = new OriginResponseDTO(
	            p.getOrigin().getId(),
	            p.getOrigin().getCity().getNome(),
	            p.getOrigin().getLogradouro(),
	            p.getOrigin().getNumero(),
	            p.getOrigin().getBairro(),
	            p.getOrigin().getCep()
	        );

	        DestinationResponseDTO destinationDTO = new DestinationResponseDTO(
	            p.getDestination().getId(),
	            p.getDestination().getCity().getNome(),
	            p.getDestination().getLogradouro(),
	            p.getDestination().getNumero(),
	            p.getDestination().getBairro(),
	            p.getDestination().getCep()
	        );

	        return new CompletedPassengerRequestDTO(
	            p.getId(),
	            originDTO,
	            destinationDTO,
	            p.getCarona().getId(),
	            p.getDataHora(), // novo campo incluído
	            p.getStatus().getNome(),        // ✅ ADICIONAR: ex: "ACEITO", "PENDENTE", etc
	            p.getStatus().getId(),           // ✅ ADICIONAR: ex: 2L para ACEITO
	         // ✅ ADICIONAR: Dados do motorista
	            p.getCarona().getDriver().getNome(),
	            p.getCarona().getDriver().getFoto(),
	            p.getCarona().getDriver().getCourse().getName(),
	            
	            // ✅ ADICIONAR: Dados do veículo
	            p.getCarona().getVehicle().getModelo(),
	            p.getCarona().getVehicle().getMarca(),
	            p.getCarona().getVehicle().getPlaca(),
	            p.getCarona().getVehicle().getCor()

	        );
	    }).toList();

	    return new PageImpl<>(dtos, paginaDeSolicitacoes.getPageable(), paginaDeSolicitacoes.getTotalElements());
	}
	public List<PendingPassengerRequestDTO> getPendingRequests(Long userId) {
	    User user = userRepository.findById(userId)
	        .orElseThrow(() -> new IllegalArgumentException("usuario não encontrado"));

	    // IDs de status que queremos retornar ao passageiro (pendente e aceita)
	    List<Long> statusIds = Arrays.asList(1L, 2L); // ajuste se os IDs forem diferentes

	    List<PassageRequests> passageRequestsList = passageRequestsRepository.findByPassengerIdAndStatusIdIn(userId, statusIds);

	    if (passageRequestsList == null || passageRequestsList.isEmpty()) {
	        return Collections.emptyList();
	    }

	    return passageRequestsList.stream().map(passageRequests -> {
	        OriginDTO originDTO = new OriginDTO(
	            passageRequests.getOrigin().getCity().getNome(),
	            passageRequests.getOrigin().getLogradouro(),
	            passageRequests.getOrigin().getNumero(),
	            passageRequests.getOrigin().getBairro(),
	            passageRequests.getOrigin().getCep()
	        );

	        DestinationDTO destinationDTO = new DestinationDTO(
	            passageRequests.getDestination().getCity().getNome(),
	            passageRequests.getDestination().getLogradouro(),
	            passageRequests.getDestination().getNumero(),
	            passageRequests.getDestination().getBairro(),
	            passageRequests.getDestination().getCep()
	        );

	        Long idMotorista = null;
	        String nomeMotorista = null;
	        String fotoMotorista = null;
	        String cursoMotorista = null;
	        if (passageRequests.getCarona() != null && passageRequests.getCarona().getDriver() != null) {
	            idMotorista = passageRequests.getCarona().getDriver().getId();
	            nomeMotorista = passageRequests.getCarona().getDriver().getNome();
	            fotoMotorista = passageRequests.getCarona().getDriver().getFoto();
	            cursoMotorista = passageRequests.getCarona().getDriver().getCourse() != null
	                ? passageRequests.getCarona().getDriver().getCourse().getName() : null;
	        }

	        return new PendingPassengerRequestDTO(
	            passageRequests.getId(),
	            idMotorista,
	            nomeMotorista,
	            fotoMotorista,
	            cursoMotorista,
	            originDTO,
	            destinationDTO,
	            passageRequests.getCarona() != null ? passageRequests.getCarona().getId() : null,
	            passageRequests.getStatus() != null ? passageRequests.getStatus().getNome() : null,
	            passageRequests.getStatus() != null ? passageRequests.getStatus().getId() : null
	        );
	    }).collect(Collectors.toList());
	}
	

}
