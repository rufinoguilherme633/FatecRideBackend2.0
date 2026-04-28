package com.example.fatecCarCarona.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.example.fatecCarCarona.dto.IniciarFluxoAutomaticoDTO;
import com.example.fatecCarCarona.dto.RespostaMotoristaDTO;
import com.example.fatecCarCarona.entity.PassageRequestQueue;
import com.example.fatecCarCarona.entity.PassageRequests;
import com.example.fatecCarCarona.repository.PassageRequestQueueRepository;
import com.example.fatecCarCarona.repository.PassageRequestsRepository;
import com.example.fatecCarCarona.repository.UserRepository;
import com.example.fatecCarCarona.service.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Testes E2E - Fluxo Automático de Caronas")
@Transactional
class PassageRequestAutomaticE2ETest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PassageRequestsRepository passageRequestsRepository;

	@Autowired
	private PassageRequestQueueRepository passageRequestQueueRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TokenService tokenService;

	private IniciarFluxoAutomaticoDTO iniciarFluxoDTO;
	private String passengerAuthHeader;
	private String driverAuthHeader;

	@BeforeEach
	void setUp() {
		var passenger = userRepository.findById(1L)
				.orElseThrow(() -> new AssertionError("Passageiro seed não encontrado"));
		var driver = userRepository.findById(2L)
				.orElseThrow(() -> new AssertionError("Motorista seed não encontrado"));

		passengerAuthHeader = "Bearer " + tokenService.generateToken(passenger);
		driverAuthHeader = "Bearer " + tokenService.generateToken(driver);

		// DTO para iniciar fluxo automático (Solicitação 1)
		// Record: IniciarFluxoAutomaticoDTO(Long passageRequestId, Double latitudeOrigem, Double longitudeOrigem, Double latitudeDestino, Double longitudeDestino)
		iniciarFluxoDTO = new IniciarFluxoAutomaticoDTO(1L, -23.5505, -46.6333, -23.5710, -46.6560);
	}

	private PassageRequestQueue obterPrimeiraFilaDaSolicitacao() {
		List<PassageRequestQueue> fila = passageRequestQueueRepository.findBySolicitacaoIdOrderByOrdemFilaAsc(1L);
		assertFalse(fila.isEmpty(), "Fila não deveria estar vazia");
		return fila.get(0);
	}

	private RespostaMotoristaDTO criarRespostaMotoristaDTO(Long filaId) {
		return new RespostaMotoristaDTO(1L, filaId, 2L, "aceitar");
	}

	@Test
	@DisplayName("E2E-01: Fluxo completo - Passageiro cria solicitação e motorista aceita")
	void testFluxoCompletoMotoristaAceita() throws Exception {
		// 1️⃣ ACT - Passageiro inicia fluxo automático
		MvcResult result = mockMvc.perform(post("/solicitacao/automatico/iniciar")
				.header("Authorization", passengerAuthHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(iniciarFluxoDTO)))
				.andExpect(status().is2xxSuccessful())
				.andReturn();

		// 2️⃣ ASSERT - Validar resposta HTTP
		assertNotNull(result.getResponse().getContentAsString());

		// 3️⃣ ASSERT - Validar BD: Solicitação deve ter status "pendente"
		PassageRequests solicitacao = passageRequestsRepository.findById(1L)
				.orElseThrow(() -> new AssertionError("Solicitação não encontrada"));
		assertEquals("pendente", solicitacao.getStatus().getNome());

		// 4️⃣ ASSERT - Validar BD: Fila foi criada com status "enviada"
		PassageRequestQueue fila = obterPrimeiraFilaDaSolicitacao();
		assertEquals(1, passageRequestQueueRepository.findBySolicitacaoIdOrderByOrdemFilaAsc(1L).size(), "Deveria ter 1 motorista na fila");
		assertEquals("enviada", fila.getStatus().getNome());

		// 5️⃣ ACT - Motorista aceita solicitação
		mockMvc.perform(post("/solicitacao/automatico/aceitar")
				.header("Authorization", driverAuthHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(criarRespostaMotoristaDTO(fila.getId()))))
				.andExpect(status().is2xxSuccessful());

		// 6️⃣ ASSERT - Validar BD: Fila deve ter status "aceita"
		PassageRequestQueue filaAtualizada = passageRequestQueueRepository.findById(fila.getId())
				.orElseThrow(() -> new AssertionError("Fila não encontrada"));
		assertEquals("aceita", filaAtualizada.getStatus().getNome());

		// 7️⃣ ASSERT - Validar BD: Solicitação deve ter status "aceita" na pipeline
		PassageRequests solicitacaoAceita = passageRequestsRepository.findById(1L).get();
		assertEquals("aceita", solicitacaoAceita.getStatusPipeline().getNome());
	}

	@Test
	@DisplayName("E2E-02: Fluxo com recusa - Motorista recusa e passa para próximo")
	void testFluxoMotoristaRecusa() throws Exception {
		// 1️⃣ ACT - Passageiro inicia fluxo
		mockMvc.perform(post("/solicitacao/automatico/iniciar")
				.header("Authorization", passengerAuthHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(iniciarFluxoDTO)))
				.andExpect(status().is2xxSuccessful());

		// 2️⃣ ASSERT - Validar que fila foi criada
		PassageRequestQueue filaInicial = obterPrimeiraFilaDaSolicitacao();
		assertEquals("enviada", filaInicial.getStatus().getNome());

		// 3️⃣ ACT - Motorista recusa solicitação
		mockMvc.perform(post("/solicitacao/automatico/recusar")
				.header("Authorization", driverAuthHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(criarRespostaMotoristaDTO(filaInicial.getId()))))
				.andExpect(status().is2xxSuccessful());

		// 4️⃣ ASSERT - Validar BD: Primeira fila deve estar "recusada"
		PassageRequestQueue filaRecusada = passageRequestQueueRepository.findById(filaInicial.getId()).get();
		assertEquals("recusada", filaRecusada.getStatus().getNome());

		// 5️⃣ ASSERT - Validar BD: Solicitação continua "pendente" (aguardando próximo motorista)
		PassageRequests solicitacao = passageRequestsRepository.findById(1L).get();
		assertEquals(1, solicitacao.getTentativaAtual(), "Tentativa deve ter incrementado");
	}

	@Test
	@DisplayName("E2E-03: Validar que fila de motoristas foi criada corretamente")
	void testFilaMotoristasCriada() throws Exception {
		// 1️⃣ ACT - Iniciar fluxo automático
		mockMvc.perform(post("/solicitacao/automatico/iniciar")
				.header("Authorization", passengerAuthHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(iniciarFluxoDTO)))
				.andExpect(status().is2xxSuccessful());

		// 2️⃣ ASSERT - Validar BD: Fila deve existir
		List<PassageRequestQueue> fila = passageRequestQueueRepository
				.findBySolicitacaoIdOrderByOrdemFilaAsc(1L);
		assertFalse(fila.isEmpty(), "Fila não deveria estar vazia");

		// 3️⃣ ASSERT - Validar BD: Primeiro motorista tem ordem_fila = 1
		PassageRequestQueue primeiroNaFila = fila.get(0);
		assertEquals(1, primeiroNaFila.getOrdemFila());
		assertEquals("enviada", primeiroNaFila.getStatus().getNome());
		assertNotNull(primeiroNaFila.getDataEnvio(), "Data de envio não deveria ser nula");

		// 4️⃣ ASSERT - Validar BD: Motorista está preenchido
		assertNotNull(primeiroNaFila.getMotorista());
		assertNotNull(primeiroNaFila.getRide());
		assertTrue(primeiroNaFila.getDistanciaOrigemKm() >= 0, "Distância deveria ser maior ou igual a 0");
	}

	@Test
	@DisplayName("E2E-04: Validar tentativa_atual incrementando corretamente")
	void testTentativaAtualIncrementa() throws Exception {
		// 1️⃣ ASSERT - Inicial: tentativa_atual = 0
		PassageRequests solicitacaoInicial = passageRequestsRepository.findById(1L).get();
		assertEquals(0, solicitacaoInicial.getTentativaAtual());

		// 2️⃣ ACT - Iniciar fluxo (tenta primeiro motorista)
		mockMvc.perform(post("/solicitacao/automatico/iniciar")
				.header("Authorization", passengerAuthHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(iniciarFluxoDTO)))
				.andExpect(status().is2xxSuccessful());

		// 3️⃣ ASSERT - Após iniciar: tentativa_atual = 1
		PassageRequests solicitacaoAposPrimeira = passageRequestsRepository.findById(1L).get();
		assertEquals(1, solicitacaoAposPrimeira.getTentativaAtual(), "Primera tentativa deveria ser 1");

		// 4️⃣ ACT - Motorista recusa
		mockMvc.perform(post("/solicitacao/automatico/recusar")
				.header("Authorization", driverAuthHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(criarRespostaMotoristaDTO(obterPrimeiraFilaDaSolicitacao().getId()))))
				.andExpect(status().is2xxSuccessful());

		// 5️⃣ ASSERT - Após recusa: tentativa_atual permanece 1 porque não há próximo motorista na fila
		PassageRequests solicitacaoAposRecusa = passageRequestsRepository.findById(1L).get();
		assertEquals(1, solicitacaoAposRecusa.getTentativaAtual(), "Tentativa deveria permanecer em 1 quando não houver próximo motorista");
	}

	@Test
	@DisplayName("E2E-05: Validar timestamps (data_envio, data_resposta)")
	void testTimestampsPreenchidos() throws Exception {
		// 1️⃣ ACT - Iniciar fluxo
		mockMvc.perform(post("/solicitacao/automatico/iniciar")
				.header("Authorization", passengerAuthHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(iniciarFluxoDTO)))
				.andExpect(status().is2xxSuccessful());

		// 2️⃣ ASSERT - Validar data_envio foi preenchida
		PassageRequestQueue fila = obterPrimeiraFilaDaSolicitacao();
		assertNotNull(fila.getDataEnvio(), "data_envio não deveria ser nula");

		// 3️⃣ ASSERT - Validar que data_resposta é nula (ainda não respondeu)
		assertNull(fila.getDataResposta(), "data_resposta deveria ser nula antes de responder");

		// 4️⃣ ACT - Motorista aceita
		mockMvc.perform(post("/solicitacao/automatico/aceitar")
				.header("Authorization", driverAuthHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(criarRespostaMotoristaDTO(fila.getId()))))
				.andExpect(status().is2xxSuccessful());

		// 5️⃣ ASSERT - Validar data_resposta foi preenchida
		PassageRequestQueue filaComResposta = passageRequestQueueRepository.findById(fila.getId()).get();
		assertNotNull(filaComResposta.getDataResposta(), "data_resposta deveria ser preenchida");
		assertTrue(filaComResposta.getDataResposta().isAfter(filaComResposta.getDataEnvio()),
				"data_resposta deveria ser depois de data_envio");
	}

	@Test
	@DisplayName("E2E-06: Validar status_pipeline mudando corretamente")
	void testStatusPipelineTransicoes() throws Exception {
		// 1️⃣ ASSERT - Inicial: status_pipeline = "aguardando"
		PassageRequests solicitacaoInicial = passageRequestsRepository.findById(1L).get();
		assertEquals("aguardando", solicitacaoInicial.getStatusPipeline().getNome());

		// 2️⃣ ACT - Iniciar fluxo
		mockMvc.perform(post("/solicitacao/automatico/iniciar")
				.header("Authorization", passengerAuthHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(iniciarFluxoDTO)))
				.andExpect(status().is2xxSuccessful());

		// 3️⃣ ASSERT - Validar status continua "aguardando"
		PassageRequests solicitacaoAposIniciar = passageRequestsRepository.findById(1L).get();
		assertEquals("aguardando", solicitacaoAposIniciar.getStatusPipeline().getNome());

		// 4️⃣ ACT - Motorista aceita
		mockMvc.perform(post("/solicitacao/automatico/aceitar")
				.header("Authorization", driverAuthHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(criarRespostaMotoristaDTO(obterPrimeiraFilaDaSolicitacao().getId()))))
				.andExpect(status().is2xxSuccessful());

		// 5️⃣ ASSERT - status_pipeline muda para "aceita"
		PassageRequests solicitacaoFinal = passageRequestsRepository.findById(1L).get();
		assertEquals("aceita", solicitacaoFinal.getStatusPipeline().getNome());
	}

	@Test
	@DisplayName("E2E-07: Validar erro HTTP para solicitação inválida")
	void testErroSolicitacaoInvalida() throws Exception {
		// 1️⃣ ARRANGE - DTO com ID inválido (999L não existe no BD)
		IniciarFluxoAutomaticoDTO dtoInvalido = new IniciarFluxoAutomaticoDTO(999L, -23.5505, -46.6333, -23.5710, -46.6560);

		// 2️⃣ ACT & ASSERT - Deve retornar erro
		mockMvc.perform(post("/solicitacao/automatico/iniciar")
				.header("Authorization", passengerAuthHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dtoInvalido)))
				.andExpect(status().is5xxServerError());
	}

	@Test
	@DisplayName("E2E-08: Validar que motorista recusado tem status correto na fila")
	void testMotoristaRecusadoTemStatusCorreto() throws Exception {
		// 1️⃣ ACT - Iniciar fluxo
		mockMvc.perform(post("/solicitacao/automatico/iniciar")
				.header("Authorization", passengerAuthHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(iniciarFluxoDTO)))
				.andExpect(status().is2xxSuccessful());

		// 2️⃣ ASSERT - Validar status inicial "enviada"
		PassageRequestQueue filaAntes = obterPrimeiraFilaDaSolicitacao();
		assertEquals("enviada", filaAntes.getStatus().getNome());

		// 3️⃣ ACT - Motorista recusa
		mockMvc.perform(post("/solicitacao/automatico/recusar")
				.header("Authorization", driverAuthHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(criarRespostaMotoristaDTO(filaAntes.getId()))))
				.andExpect(status().is2xxSuccessful());

		// 4️⃣ ASSERT - Validar status muda para "recusada"
		PassageRequestQueue filaDepois = passageRequestQueueRepository.findById(filaAntes.getId()).get();
		assertEquals("recusada", filaDepois.getStatus().getNome());
	}
}



