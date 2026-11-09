# Uso de Inteligência Artificial

Usei o Claude e o Chatgpt como parceiros de desenvolvimento e revisão técnica, trabalhando por etapas: propus um raciocinio principal que primeiramente foi moldado pelo gpt e apontado pontos importantes, então logo após eu executava, validava e decidia. Nenhum código foi aceito sem eu rodar os testes, e o assistente **não executou nada**: toda validação (compilar, rodar testes, quebrar o código de propósito) foi feita por mim.

## Principais prompts

1. **Contexto geral e regras de trabalho.** Colei o enunciado do case meu entendimento e forma como queria trabalhar (inclusive linguagem que iria utilizar e porque) e defini como queria trabalhar: por etapas, justificando decisões, registrando o uso de IA e sem commits automáticos. Pedi análise do projeto, arquitetura simples, plano em etapas e decisões a tomar antes de codar.
2. **Ponto de partida do projeto.** Enviei o link do repositório e informei meu fluxo de Git (branch `develop`, branches de feature e merge final na `main`). Pedi commits de teste separados em cada etapa e aprovei as decisões propostas.
5. **Verificação crítica.** "São 17 testes e não 16, por que isso acontece?" e "qual é esse `StockItem.release`?" — no momento da codificação foi necessário fazer essas perguntas para entender divergências entre a resposta e o que vi na tela.
6. **Avanço etapa a etapa.** A cada etapa concluída e mergeada, pedia a ele testes positivos e negativos para eu fazer manualmente e ver se estava tudo nos conformes.

## Onde a IA mais ajudou

- **Transformar o enunciado em plano.** Dividir em etapas com tempo estimado e listar decisões antes de implementar.
- **Mapear casos-limite.** Liberação duplicada, liberação de retenção de outro produto, entrada que cria produto novo, falha que não pode deixar estado parcial, consulta no meio de uma operação.
- **Provar que os testes valem.** Sugerir quebrar o código de propósito (mutação manual) para ver o teste falhar.
- **Setup e ambiente.** Conversão para Maven e layout de pastas.

## Sugestões que modifiquei ou não utilizei

1. **Nome do pacote (modificada).** A IA sugeriu `com.lucasdsl.estoque`. Usei `br.com.java.lucas.estoque`, que é o padrão que já uso.
2. **Spring Boot, banco e API REST (não utilizados).** A própria análise recomendou não usar: o case avalia regras de negócio, consistência e testes, e o prazo é curto. A API ficou como evolução documentada.

## Erros, limitações e problemas identificados

1. **Contagem errada de testes.** A IA informou que a suíte teria 16 testes; o IntelliJ mostrou 17. A causa foi o `@NullAndEmptySource`, que gera dois casos (`null` e vazio) e não um. Percebi pela divergência na tela, e a lição foi conferir o resultado em vez de confiar no número informado.
2. **Orientação incompleta sobre pacotes na IDE.** A IA afirmou que o `package` do `Main` continuaria válido depois de mover as pastas. Na prática o IntelliJ já tinha calculado o pacote a partir da raiz antiga e a primeira linha ficou `main.java.br...`. Corrigi trocando o texto e conferindo o caminho físico.
3. **Limitação geral.** A IA não viu meu projeto local nem executou o código: dependeu dos prints e do que eu colei, e qualquer erro de compilação ou de comportamento só apareceu quando eu rodei.

## Como validei as respostas da IA
- Rodei a suíte a cada etapa antes de qualquer merge.
- Quebrei o código de propósito (trocar `+=` por `-=`, remover a checagem de liberação duplicada, anular o lock) para confirmar que os testes falham.
- Conferi o `Main` contra uma tabela de valores esperados, passo a passo.
- Li os arquivos gerados para conseguir explicar cada decisão.