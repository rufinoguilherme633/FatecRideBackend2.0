# 📚 ÍNDICE - DOCUMENTOS DE VALIDAÇÃO

## 📖 Guia Rápido

Esta pasta contém **4 documentos principais** para validação completa do fluxo automático:

---

## 📄 DOCUMENTO 1: PROMPT_COPIAR_COLAR.md
**Melhor para:** Usar IMEDIATAMENTE com um agente IA

```
✅ Use este arquivo para:
   - Copiar e colar em ChatGPT, Claude ou outro agente
   - Fazer uma validação RÁPIDA e DIRETA
   - Obter lista de problemas em minutos

⏱️ Tempo: ~15-30 minutos para agente responder
📊 Output: Lista de bugs, severidade, localização
```

**Como usar:**
1. Abra `PROMPT_COPIAR_COLAR.md`
2. Copie TODO o conteúdo entre as linhas de código (` ``` `)
3. Cole em uma conversa com um agente IA
4. Aguarde a resposta

**Exemplo de resposta esperada:**
```
### ❌ Problema 1: marcarSolicitacaoComoRecusada não salva no BD
Severidade: CRÍTICO
Localização: PassageRequestAutomaticService.java, linhas 376-385
...
```

---

## 📄 DOCUMENTO 2: PROMPT_VALIDACAO_FLUXO.md
**Melhor para:** Validação COMPLETA e PROFUNDA

```
✅ Use este arquivo para:
   - Entender completamente a regra de negócio
   - Fazer validação manual ponto por ponto
   - Servir como checklist durante code review
   - Passar para outra pessoa revisar o código

⏱️ Tempo: ~2-3 horas para leitura + análise
📊 Output: Relatório detalhado com tudo validado
```

**Como usar:**
1. Leia a seção "REGRA DE NEGÓCIO"
2. Trace o código seguindo "Flow Esperado"
3. Valide cada função na seção "CHECKLIST DE VALIDAÇÃO"
4. Revise "PROBLEMAS CONHECIDOS A INVESTIGAR"
5. Documente tudo usando o "FORMATO DE RESPOSTA ESPERADO"

**Estrutura:**
- Flow esperado
- Tabelas de estados
- Checklist visual
- Problemas a investigar
- Arquivos principais
- Testes a validar
- Formato de documentação

---

## 📄 DOCUMENTO 3: SUMARIO_VALIDACAO.md
**Melhor para:** Referência RÁPIDA durante validação

```
✅ Use este arquivo para:
   - Ter um mapa mental do fluxo
   - Lista rápida de funções críticas
   - Anomalias reportadas
   - Cenários de teste
   - Checklist pós-validação

⏱️ Tempo: ~5 minutos para ler
📊 Output: Referência e checklist visual
```

**Como usar:**
1. Mantenha aberto enquanto valida
2. Use como checklist visual
3. Consulte quando tiver dúvidas sobre prioridades

**Seções principais:**
- Mapa mental do fluxo
- Tabela de funções críticas
- Anomalias reportadas (com SQL)
- Cenários de teste A, B, C
- Checklist pós-validação

---

## 📄 DOCUMENTO 4: TESTE_FLUXO_AUTOMATICO_COMPLETO.md
**Melhor para:** Testar NA PRÁTICA o que foi corrigido

```
✅ Use este arquivo para:
   - Testar o fluxo end-to-end
   - Executar passo a passo
   - Validar com curl/Postman
   - Verificar SSE funcionando
   - Confirmar dados no BD

⏱️ Tempo: ~30-45 minutos para testar tudo
📊 Output: Confirmação de que tudo funciona
```

**Como usar:**
1. Crie os usuários conforme instruções
2. Execute os passos NA ORDEM
3. Observe SSE funcionando no Terminal 1
4. Compare tudo com as JSONs esperadas
5. Verifique dados no BD

---

## 🔄 FLUXO RECOMENDADO DE USO

### Dia 1: Validação Inicial
```
1. Leia SUMARIO_VALIDACAO.md (~5 min)
   └─ Entenda o mapa mental

2. Use PROMPT_COPIAR_COLAR.md (~30 min)
   └─ Agente encontra problemas rapidamente

3. Documente os problemas encontrados
   └─ Use formato de PROMPT_VALIDACAO_FLUXO.md
```

### Dia 2-3: Validação Profunda (Optional)
```
4. Leia PROMPT_VALIDACAO_FLUXO.md (~2h)
   └─ Faça validação manual completa

5. Trace o código em detalhes
   └─ Verifique cada função

6. Documente achados no próprio PROMPT_VALIDACAO_FLUXO.md
   └─ Use checklist visual
```

### Dia 4+: Correções
```
7. Corrija cada problema encontrado
   └─ Comece pelos CRÍTICOS

8. Execute TESTE_FLUXO_AUTOMATICO_COMPLETO.md
   └─ Valide que tudo funciona

9. Repita PROMPT_COPIAR_COLAR.md
   └─ Confirme que problemas foram resolvidos
```

---

## 🎯 MATRIZ DE SEVERIDADE

| Severidade | Exemplo | Ação |
|-----------|---------|------|
| 🔴 CRÍTICO | marcarSolicitacao não salva | Corrigir HOJE |
| 🟠 ALTO | tentativa_atual errado | Corrigir antes de merge |
| 🟡 MÉDIO | Log faltando | Corrigir em próximo sprint |
| 🟢 BAIXO | Comentário impreciso | Melhorar quando tiver tempo |

---

## 📋 CHECKLIST: O QUE FAZER AGORA

- [ ] **Imediatamente:** Abra `PROMPT_COPIAR_COLAR.md`
- [ ] **Próximos 30 min:** Cole em um agente IA
- [ ] **Próxima 1h:** Revise os problemas encontrados
- [ ] **Próximas 2-3h:** Corrija problemas CRÍTICOS e ALTOS
- [ ] **Depois:** Use `TESTE_FLUXO_AUTOMATICO_COMPLETO.md` para validar
- [ ] **Final:** Confirme que tudo funciona

---

## 🤔 DÚVIDAS FREQUENTES

### P: Qual documento devo usar primeiro?
**R:** `PROMPT_COPIAR_COLAR.md` - É o mais rápido e direto

### P: Tenho pouco tempo, qual devo ler?
**R:** `SUMARIO_VALIDACAO.md` - Resume tudo em 5 minutos

### P: Quero validação completa, qual devo usar?
**R:** `PROMPT_VALIDACAO_FLUXO.md` + `TESTE_FLUXO_AUTOMATICO_COMPLETO.md`

### P: Já tenho uma lista de problemas, o que fazer?
**R:** Use `PROMPT_VALIDACAO_FLUXO.md` para documentar tudo uniformemente

### P: Quero garantir que tudo funciona, o que testar?
**R:** `TESTE_FLUXO_AUTOMATICO_COMPLETO.md` - é o teste end-to-end

---

## 📊 MAPA DE DEPENDÊNCIAS

```
PROMPT_COPIAR_COLAR.md
    ↓ (Gera lista de problemas)
    ↓
SUMARIO_VALIDACAO.md
    ↓ (Consulta para referência)
    ↓
PROMPT_VALIDACAO_FLUXO.md
    ↓ (Valida em detalhes)
    ↓
TESTE_FLUXO_AUTOMATICO_COMPLETO.md
    ↓ (Testa práticas)
    ↓
✅ VALIDAÇÃO COMPLETA
```

---

## 🚀 PRÓXIMOS PASSOS

1. **Agora:** Leia este documento (✓ você está aqui)
2. **Próximo:** Abra `PROMPT_COPIAR_COLAR.md`
3. **Depois:** Use com agente IA
4. **Então:** Corrija os problemas
5. **Final:** Teste tudo com `TESTE_FLUXO_AUTOMATICO_COMPLETO.md`

---

## 📞 REFERÊNCIA RÁPIDA

| Necessidade | Documento | Tempo |
|-----------|-----------|-------|
| Validação rápida | PROMPT_COPIAR_COLAR.md | 30 min |
| Referência | SUMARIO_VALIDACAO.md | 5 min |
| Validação profunda | PROMPT_VALIDACAO_FLUXO.md | 2-3h |
| Teste prático | TESTE_FLUXO_AUTOMATICO_COMPLETO.md | 45 min |

---

**Criado em:** 2026-05-06
**Status:** ✅ Pronto para validação
**Próximo Passo:** Abra PROMPT_COPIAR_COLAR.md

