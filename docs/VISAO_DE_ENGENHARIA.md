# Visão de engenharia
Os cinco aspectos que considero mais importantes para esta solução, e o que cada um garante.

## 1. Regras e invariantes protegidos no domínio
**Garante:** qualidade e confiabilidade.
O invariante `total = disponível + reservado + bloqueado` vive dentro de `StockItem`, e o total é derivado. Nenhum chamador (serviço, demonstração ou uma futura API) consegue deixar o estoque inconsistente, porque a validação acontece antes de qualquer alteração e uma operação rejeitada não deixa estado parcial.

## 2. Testes que falham quando o código quebra
**Garante:** qualidade e evolução segura.
Os testes descrevem as regras (saída só do disponível, liberação única, retenção de outro produto) e foram validados quebrando o código de propósito. Um teste que sempre passa não protege ninguém; essa verificação encontrou uma lacuna real na primeira etapa. Com isso, mudar a implementação ou trocar a persistência tem uma rede de segurança.

## 3. Consistência sob concorrência
**Garante:** confiabilidade.
Em estoque, o erro típico é vender a mesma unidade duas vezes. Um lock por produto torna "ler, decidir, escrever" atômico, inclusive nas consultas, e os testes disputam o mesmo produto com muitas threads. O limite está documentado: a garantia vale numa só JVM e, em produção, passa para o banco (atualização condicional ou lock otimista).

## 4. Simplicidade proporcional e eficiência pragmática
**Garante:** eficiência e manutenção.
Sem framework, banco ou API: cada tecnologia teria de ser explicada e testada sem melhorar a avaliação das regras. A eficiência vem de escolhas baratas e justificadas: contadores com consulta em tempo constante e lock por produto, que não bloqueia produtos diferentes. Não otimizei além disso sem medir.

## 5. Fronteiras claras e limitações documentadas
**Garante:** evolução.
Domínio, aplicação e infraestrutura separados, com a persistência atrás de uma interface, permitem trocar o armazenamento sem mexer nas regras. O que não foi feito (idempotência, expiração, histórico, API, várias instâncias) está listado com o caminho de evolução, e os commits pequenos por etapa deixam a história do projeto legível.  