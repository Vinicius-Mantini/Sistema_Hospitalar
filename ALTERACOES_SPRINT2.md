# Registro de alterações — Sprint 2

## Correções aplicadas

- Busca por especialidade movida para `MedicoEspecialistaRepository`, evitando consulta derivada a um campo inexistente na classe-base.
- Criação dos atendimentos via DTO com identificadores de paciente e profissional; resolução das referências JPA no serviço.
- Estados iniciais e transições de consultas, emergências e cirurgias controlados por regras de domínio e endpoints específicos.
- Cirurgia não pode ser concluída antes do início; horário efetivo é registrado quando o procedimento começa; observações pós-operatórias ficam disponíveis após a conclusão.
- Consulta especializada valida compatibilidade com a especialidade do profissional e trata campos nulos sem exceções inesperadas.
- Clínico geral pode registrar encaminhamento persistido para especialista; cirurgião pode consultar cirurgias sob sua responsabilidade.
- Emergências possuem prioridade enumerada e estados controlados (aguardando, iniciado, concluído).
- Internações possuem alta e cancelamento com liberação de leitos em transações; quartos são bloqueados pessimisticamente ao ocupar/liberar vagas.
- Quartos possuem tipo enumerado, consulta por tipo e operações controladas de manutenção; ocupação não é informada pelo cliente.
- Histórico médico passa a ser criado para paciente novo e atualizado ao registrar atendimento e internação.
- Diagrama de classes atualizado e exportado em três formatos (`.puml`, `.svg`, `.png`).
- Casos de teste de domínio e integração ampliados; documentação de requisições e respostas no README.
- Maven Wrapper configurado para a distribuição Maven 3.9.11.

## Limitações conhecidas

- A camada HTML/CSS/JS continua demonstrativa (não integrada ao backend), conforme o README. A Sprint 2 pede a API e o domínio.
- Não foi possível executar Maven/testes no ambiente de preparação por falta da distribuição local e indisponibilidade de resolução DNS para downloads externos. Não há confirmação empírica de inicialização da API neste ambiente. Execute `./mvnw clean test` em um ambiente com dependências disponíveis antes da entrega final.
- O enunciado não determina os tipos específicos de quartos; foram adotados `ENFERMARIA`, `APARTAMENTO` e `UTI` como classificação de exemplo, sem regras de capacidade arbitrárias por categoria.
