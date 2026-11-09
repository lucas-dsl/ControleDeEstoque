# Controle de Estoque

Solução em Java para controle de estoque de produtos, com quantidades **total**, **disponível**, **reservada** e **bloqueada**, preservando a consistência em todas as operações.

## Visão geral

Para cada produto o sistema mantém quatro quantidades ligadas por uma regra única:

```
total = disponível + reservado + bloqueado
```

O total **não é armazenado**: é sempre calculado a partir das outras três, então ele não tem como divergir delas.

| Operação | Total | Disponível | Reservado | Bloqueado |
|---|---|---|---|---|
| Entrada | + | + | | |
| Saída | − | − | | |
| Reservar | | − | + | |
| Bloquear | | − | | + |
| Liberar reserva | | + | − | |
| Liberar bloqueio | | + | | − |
| Consultar | não altera | | | |

Cada reserva e cada bloqueio recebe um identificador (UUID) e guarda o produto, a quantidade, o tipo, o status e a solicitação associada. A liberação é feita por esse identificador.

## Arquitetura e estrutura

Java 21+, Maven e JUnit 5. Sem framework, sem banco e sem API: o case avalia regras de negócio, consistência e testes, e nenhuma dessas tecnologias agregaria a isso no prazo proposto.

```
src/main/java/br/com/java/lucas/estoque
├── Main.java                        demonstração em console
├── application
│   ├── StockService.java            casos de uso e controle de concorrência
│   └── StockSummary.java            visão somente leitura para consulta
├── domain
│   ├── StockItem.java               estoque de um produto e suas regras
│   ├── StockHold.java               uma reserva ou um bloqueio
│   ├── HoldType.java / HoldStatus.java
│   ├── StockRepository.java         interface de persistência
│   └── exception/                   erros de regra de negócio
└── infrastructure
    └── InMemoryStockRepository.java implementação em memória
```

- **domain** contém as regras e não depende de nada. É onde o invariante é protegido.
- **application** orquestra: valida o identificador, busca o produto, chama o domínio e salva. Também garante o acesso exclusivo por produto.
- **infrastructure** implementa a persistência. Hoje é um `ConcurrentHashMap`.

## Principais decisões técnicas

| Decisão | Motivo | Alternativa considerada |
|---|---|---|
| Total derivado, sem campo próprio | Única fonte da verdade; não há como divergir | Armazenar o total e atualizar junto: exige lembrar de atualizar em cada operação |
| Regras dentro de `StockItem` | Qualquer chamador recebe a mesma proteção | Validar só no serviço |
| Uma classe `StockHold` para reserva e bloqueio, com `HoldType` | Comportamento idêntico (tira do disponível, é liberado por id); evita duplicar código | Duas classes; separar se as regras divergirem |
| Retenção liberada continua registrada (`RELEASED`) | Distingue "não existe" de "já liberada" e prepara um status de expiração | Remover do mapa |
| Retenções ficam dentro do `StockItem` do produto | Liberar uma retenção de outro produto falha por escopo | Índice global de retenções |
| `release()` e construtor de `StockHold` visíveis só no pacote | Só `StockItem` altera retenções e contadores juntos | Métodos públicos |
| Valida tudo antes de alterar | Operação rejeitada não deixa estado parcial | |
| Exceções não verificadas com base `StockException` | Regra violada não polui assinaturas; um único `catch` pega todas | Exceções verificadas |
| `long` para quantidades | Evita estouro silencioso de `int` | `int` |
| Interface `StockRepository` | A persistência é o ponto de mudança mais provável | `Map` direto no serviço |
| Consulta devolve `StockSummary` (record) | Não expõe a entidade mutável | Devolver `StockItem` |
| Um lock por produto (`synchronized`) | "Ler, decidir, escrever" precisa ser atômico; produtos diferentes não se bloqueiam | Lock global; `AtomicLong` (não cobre dois campos juntos) |

## Premissas

- A **entrada** em um produto inexistente cria o produto. Saída, reserva, bloqueio, liberação e consulta em produto inexistente falham com `ProductNotFoundException`.
- Quantidades são inteiros positivos. Zero e negativos são rejeitados.
- A **reserva exige** identificador de solicitação. No **bloqueio** ele é opcional, mas, se informado, não pode ser vazio.
- Uma mesma solicitação pode ter várias reservas no mesmo produto.
- A saída considera só a quantidade **disponível**. O que está reservado ou bloqueado não pode ser retirado.
- A liberação é **total**: libera toda a quantidade da retenção. Não há liberação parcial.
- Identificadores de produto são tratados como estão: diferenciam maiúsculas de minúsculas e não são aparados (`"A"` e `" A "` são produtos diferentes).
- Retenção de outro produto é tratada como inexistente no produto consultado.

## Limitações conhecidas

- **Dados em memória:** tudo se perde ao encerrar a aplicação.
- **Concorrência só dentro de uma JVM:** com várias instâncias, o `synchronized` não protege.
- **Mapa de locks cresce** com o número de produtos e nunca é limpo.
- **Sem idempotência:** repetir uma chamada por timeout cria uma segunda reserva.
- **Sem expiração** de reservas e **sem histórico** de movimentações.
- A mensagem de liberar retenção de outro produto é a mesma de "não encontrada".
- As retenções devolvidas ao chamador são **referências vivas**, não cópias imutáveis.
- Sem API; a demonstração é um `Main` em console, sem teste automatizado.

## Como executar

Requisitos: **JDK 21 ou superior** (desenvolvido e testado com JDK 23). O Maven é baixado pelo wrapper, então não precisa estar instalado.

**Testes (Windows):**
```powershell
.\mvnw.cmd test
```

**Testes (Linux/Mac):**
```bash
./mvnw test
```

**Demonstração (Windows):**
```powershell
.\mvnw.cmd compile
java -cp target\classes br.com.java.lucas.estoque.Main
```

**Demonstração (Linux/Mac):**
```bash
./mvnw compile
java -cp target/classes br.com.java.lucas.estoque.Main
```

A demonstração percorre um cenário completo (entrada, reserva, bloqueio, saída, liberações e erros esperados) e imprime o estado a cada passo.

## Testes

JUnit 5, sem mocks: os testes do serviço usam o repositório em memória real.

- **Domínio:** entrada, saída, consulta; quantidade inválida; saída maior que o disponível; reserva e bloqueio (incluindo vários por produto); liberação (inexistente, duplicada, de outro produto); saída não retira o que está retido.
- **Invariante:** reservar, bloquear e liberar não alteram o total; os contadores batem com a soma das retenções ativas; operações rejeitadas não alteram o estoque.
- **Serviço:** fluxo completo, produto inexistente, identificadores inválidos, isolamento entre produtos.
- **Concorrência:** reservas simultâneas nunca excedem o disponível; primeiras entradas simultâneas não perdem unidades; liberações simultâneas da mesma reserva valem uma vez; consultas durante escritas veem sempre um estado consistente.

Os testes de concorrência foram validados quebrando o lock de propósito, e então falham. Mesmo assim, teste de concorrência não prova ausência de corrida: a garantia vem do lock por produto.

## O que eu faria diferente em produção

- **Banco de dados transacional.** A atomicidade passaria para o banco: `UPDATE ... SET available = available - :q WHERE product_id = :id AND available >= :q` (checando as linhas afetadas), ou lock otimista com campo `version` e nova tentativa, ou `SELECT ... FOR UPDATE`. Isso resolve também o cenário de várias instâncias.
- **Expiração de reservas.** Status `EXPIRED` e uma rotina que libera reservas vencidas.
- **Histórico de movimentações.** Registrar cada entrada, saída, reserva e liberação (quem, quando, por quê), para auditoria e reconciliação.
- **API REST** com DTOs, validação de entrada e mapeamento dos erros de domínio para códigos HTTP (404, 409, 422).
- **Retorno imutável** das retenções, em vez de referências vivas.
- **Observabilidade:** logs estruturados com o identificador da operação, métricas de rejeições por insuficiência e rastreamento.
- **Testes de integração** com o banco real e **CI** executando `mvn test` a cada push.
- **Escala:** particionar por produto e tratar picos com fila.

## Documentação complementar

- [Uso de IA](docs/USO_DE_IA.md)
- [Visão de engenharia](docs/VISAO_DE_ENGENHARIA.md)