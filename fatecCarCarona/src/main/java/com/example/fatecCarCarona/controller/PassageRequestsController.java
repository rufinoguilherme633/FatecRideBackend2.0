package com.example.fatecCarCarona.controller;

import com.example.fatecCarCarona.dto.*;
import com.example.fatecCarCarona.service.PassageRequestAutomaticService;
import com.example.fatecCarCarona.service.PassageRequestsService;
import com.example.fatecCarCarona.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/solicitacao")
@Tag(name = "Solicitações", description = "Endpoints de solicitação de carona e fluxo automático")
public class PassageRequestsController {
    @Autowired
    PassageRequestsService passageRequestsService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private PassageRequestAutomaticService passageRequestAutomaticService;

    @PostMapping("/proximos")
    public ResponseEntity<List<NearbyDriversDTO>> findNearbyDrivers(@RequestBody PassengerSearchRequest passengerSearchRequest) throws Exception {
        List<NearbyDriversDTO> motoristaProximos = passageRequestsService.findNearbyDrivers(passengerSearchRequest);

        return new ResponseEntity<>(motoristaProximos, HttpStatus.OK);

    }

    @PostMapping
    public ResponseEntity<PassageRequestsCreateResponseDTO> create(@RequestHeader("Authorization") String authHeader, @RequestBody PassageRequestsDTO passageRequests) throws Exception {
        Long idLong = tokenService.extractUserIdFromHeader(authHeader);
        PassageRequestsCreateResponseDTO passageRequestsDTO = passageRequestsService.create(passageRequests, idLong);
        return ResponseEntity.ok(passageRequestsDTO);
    }


    @PutMapping("cancelar/{id_solicitacao}")
    public ResponseEntity<String> cancel(@RequestHeader("Authorization") String authHeader, @PathVariable Long id_solicitacao) {

        Long idLong = tokenService.extractUserIdFromHeader(authHeader);

        passageRequestsService.cancelar(idLong, id_solicitacao);
        return ResponseEntity.ok("Solicitação cancelada com sucesso.");

    }

    @GetMapping("/concluidas")
    public ResponseEntity<?> listarSolicitacoesConcluidas(@RequestHeader("Authorization") String authorizationHeader, @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "10") int itens) {
        try {
            Long userId = tokenService.extractUserIdFromHeader(authorizationHeader);
            Page<CompletedPassengerRequestDTO> solicitacoesConcluidas = passageRequestsService.buscarSolicitacoesConcluidas(userId, pagina, itens);

            if (solicitacoesConcluidas.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(solicitacoesConcluidas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido ou usuário não autorizado.");
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PendingPassengerRequestDTO>> getPendingRequests(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            Long userId = tokenService.extractUserIdFromHeader(authorizationHeader);
            List<PendingPassengerRequestDTO> dtoList = passageRequestsService.getPendingRequests(userId);
            return ResponseEntity.ok(dtoList);
        } catch (IllegalArgumentException e) {
            // usuário não encontrado ou token inválido/ausente
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            // erro inesperado
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Etapa 5: Inicia o fluxo automático de carona
     * Busca motoristas próximos e começa a enviar notificações
     */
    @PostMapping("/automatico/iniciar")
    @Operation(summary = "Iniciar fluxo automático", description = "Busca motoristas próximos e inicia o envio em cascata até haver aceite ou falha final.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fluxo automático iniciado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada"),
            @ApiResponse(responseCode = "500", description = "Erro ao iniciar o fluxo automático")
    })
    public ResponseEntity<?> iniciarFluxoAutomatico(@RequestBody IniciarFluxoAutomaticoDTO request) {
        try {
            if (request == null || request.solicitacaoId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "solicitacaoId é obrigatório"));
            }
            passageRequestAutomaticService.iniciarFluxoAutomatico(
                    request.solicitacaoId(),
                    request.latitudeOrigem(),
                    request.longitudeOrigem(),
                    request.latitudeDestino(),
                    request.longitudeDestino()
            );
            return ResponseEntity.ok(Map.of("message", "Fluxo automático iniciado com sucesso"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erro ao iniciar fluxo: " + e.getMessage()));
        }
    }

    /**
     * Motorista aceita a solicitação via SSE
     */
    @PostMapping("/automatico/aceitar")
    @Operation(summary = "Aceitar solicitação automática", description = "Marca a solicitação como aceita pelo motorista autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitação aceita com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token inválido ou ausente"),
            @ApiResponse(responseCode = "404", description = "Fila ou solicitação não encontrada")
    })
    public ResponseEntity<?> aceitarSolicitacaoAutomatica(
            @Parameter(description = "JWT Bearer token", required = true)
            @RequestHeader("Authorization") String authHeader,
            @RequestBody RespostaMotoristaDTO request) {
        try {
            Long motoristaId = tokenService.extractUserIdFromHeader(authHeader);
            passageRequestAutomaticService.handleMotoristaAceita(
                    request.filaId(),
                    request.solicitacaoId(),
                    motoristaId
            );
            return ResponseEntity.ok(Map.of("message", "Solicitação aceita com sucesso"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erro ao aceitar: " + e.getMessage()));
        }
    }

    /**
     * Motorista recusa a solicitação via SSE
     */
    @PostMapping("/automatico/recusar")
    @Operation(summary = "Recusar solicitação automática", description = "Marca a solicitação na fila como recusada e avança para o próximo motorista.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitação recusada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token inválido ou ausente"),
            @ApiResponse(responseCode = "404", description = "Fila ou solicitação não encontrada")
    })
    public ResponseEntity<?> recusarSolicitacaoAutomatica(
            @Parameter(description = "JWT Bearer token", required = true)
            @RequestHeader("Authorization") String authHeader,
            @RequestBody RespostaMotoristaDTO request) {
        try {
            Long motoristaId = tokenService.extractUserIdFromHeader(authHeader);
            passageRequestAutomaticService.handleMotoristaRecusa(
                    request.filaId(),
                    request.solicitacaoId(),
                    motoristaId
            );
            return ResponseEntity.ok(Map.of("message", "Solicitação recusada"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erro ao recusar: " + e.getMessage()));
        }
    }
}

