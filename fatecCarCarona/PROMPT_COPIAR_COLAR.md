# 🤖 PROMPT PRONTO PARA COPIAR E COLAR

## Para Usar com um Agente IA

Copie e cole este texto exatamente em uma conversa com um agente:

---

```
TAREFA: VALIDAÇÃO COMPLETA DO FLUXO AUTOMÁTICO DE CARONA

CONTEXTO:
Estou desenvolvendo um aplicativo de compartilhamento de carona em Spring Boot. 
Implementei um novo fluxo automático onde passageiros criam solicitações e 
motoristas respondem em tempo real via SSE (Server-Sent Events).

REGRA DE NEGÓCIO:
1. Passageiro cria solicitação (POST /solicitacao) → Status: PENDENTE
2. Passageiro inicia fluxo automático → Sistema busca motoristas próximos, 
   cria fila, envia 1ª notificação SSE
3. Motorista recebe notificação com 60 segundos para responder
4. Se ACEITA: Solicitação marcada ACEITA, fila marcada ACEITA, 
   outros motoristas rejeitados, passageiro notificado
5. Se RECUSA: Fila marcada RECUSADA, próximo motorista recebe notificação
6. Se TIMEOUT (>60s): Fila marcada TIMEOUT, próximo motorista recebe notificação
7. Depois de 3 tentativas falhadas: Solicitação marcada RECUSADA, 
   Pipeline marcado FALHA_FINAL

ENDPOINTS NOVOS:
- POST /solicitacao/automatico/{filaId}/aceitar/{solicitacaoId}
  └─ Motorista aceita solicitação
- POST /solicitacao/automatico/{filaId}/recusar/{solicitacaoId}
  └─ Motorista recusa solicitação

BANCO DE DADOS:
- Tabela: solicitacoes
  Campos: id, id_passageiro, id_carona, status_solicitacao, 
          id_status_pipeline, tentativa_atual
  
- Tabela: solicitacao_fila_motoristas
  Campos: id, id_solicitacao, id_motorista, id_carona, 
          id_status_fila, data_envio, data_resposta, ordem_fila

ARQUIVO PRINCIPAL:
src/main/java/com/example/fatecCarCarona/service/PassageRequestAutomaticService.java
- Método: iniciarFluxoAutomatico() [82-127]
- Método: criarFilaMotoristas() [132-164]
- Método: enviarProximoMotorista() [169-219]
- Método: handleMotoristaAceita() [224-258]
- Método: handleMotoristaRecusa() [263-283]
- Método: handleTimeoutMotorista() [288-308]
- Método: rejeitarOutrosMotoristas() [313-334]
- Método: marcarSolicitacaoComoRecusada() [376-385]
- Método: atualizarStatusPipeline() [365-374]

CONTROLLER NOVO:
src/main/java/com/example/fatecCarCarona/controller/
  PassageRequestResponseController.java
- Método: aceitar() - POST /solicitacao/automatico/{filaId}/aceitar/{solicitacaoId}
- Método: recusar() - POST /solicitacao/automatico/{filaId}/recusar/{solicitacaoId}

PROBLEMAS REPORTADOS:
1. Solicitação fica PENDENTE mesmo após limite de tentativas atingido
   └─ Esperado: Status = RECUSADA, Pipeline = FALHA_FINAL
2. data_resposta em solicitacao_fila_motoristas fica NULL em alguns casos
   └─ Esperado: Sempre preenchido quando há resposta
3. tentativa_atual não incrementa corretamente
   └─ Esperado: 0 → 1 → 2 → 3 (depois para)

SUA TAREFA:
1. Revise TODA a lógica de fluxo em PassageRequestAutomaticService.java
2. Verifique se cada função persiste dados corretamente no BD
3. Trace MANUALMENTE 3 cenários:
   - Cenário A: Motorista ACEITA (happy path)
   - Cenário B: Motorista RECUSA, próximo ACEITA
   - Cenário C: 3 tentativas falham (timeout/recusa), para de tentar
4. Identifique TODOS os bugs, erros lógicos e inconsistências
5. Documente CADA problema encontrado com:
   - Título descritivo
   - Localização exata (arquivo, linhas, método)
   - Descrição clara do problema
   - Comportamento atual vs esperado
   - Impacto para o sistema
   - Exemplo de como reproduzir
   - Sugestão de correção

FORMATO DE RESPOSTA:
Para cada problema encontrado, forneça:

---
### ❌ [Número]: [TÍTULO DO PROBLEMA]

**Severidade:** CRÍTICO | ALTO | MÉDIO | BAIXO

**Localização:**
- Arquivo: [caminho]
- Linhas: [números]
- Método: [nome]

**Descrição:**
[O que está errado]

**Comportamento Atual:**
[O que existe agora]

**Comportamento Esperado:**
[O que deveria acontecer per regra de negócio]

**Impacto:**
[Como afeta o usuário/sistema]

**Reprodução:**
[Passo a passo para ver o problema]

**Sugestão de Correção:**
[Código proposto ou lógica]

---

QUESTÕES CRÍTICAS A RESPONDER:
- [ ] A função marcarSolicitacaoComoRecusada() salva no BD?
- [ ] O timeout é cancelado quando um motorista aceita?
- [ ] data_resposta é preenchida em TODOS os casos de resposta?
- [ ] tentativa_atual incrementa APENAS quando novo motorista recebe notificação?
- [ ] O pipeline transiciona correctly de AGUARDANDO → ACEITA ou FALHA_FINAL?
- [ ] Outros motoristas são rejeitados quando um aceita?
- [ ] SSE notifica corretamente em todos os casos?
- [ ] Erros são tratados gracefully?

EXTRA:
Após encontrar os problemas, ordene-os por severidade e diga 
qual é o bloqueador crítico para colocar em produção.

Você está pronto para começar a validação?
```

---

## Como Usar:

1. **Copie o texto acima** (de "TAREFA:" até "pronto para começar?")
2. **Cole em uma conversa** com um agente IA (ChatGPT, Claude, etc.)
3. **Espere a resposta** com lista detalhada de problemas
4. **Use os problemas encontrados** para corrigir o código
5. **Volte para a conversa** para fazer perguntas de follow-up

---

## Exemplo de Follow-Up (depois que o agente responder):

```
Baseado nos problemas encontrados, responda:

1. Qual é o BLOQUEADOR CRÍTICO que impede produção?
2. Qual é a ordem de prioridade para correção?
3. Quais testes devem passar após cada correção?
4. Há alguma refatoração maior necessária?
5. O código está pronto para PR (pull request)?
```

---

## Dicas:

✅ **Faça:** Seja específico nas perguntas
✅ **Faça:** Pedir exemplos de código corrigido
✅ **Faça:** Pedir validação após suas correções
✅ **Faça:** Pedir sugestões de testes adicionais

❌ **Não faça:** Pedir a geração de todo código (peça por método)
❌ **Não faça:** Ignorar os problemas encontrados
❌ **Não faça:** Corrigir sem entender o problema primeiro

---

**Criado em:** 2026-05-06
**Pronto para uso:** ✅ SIM

