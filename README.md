# Trabalho Prático – Sistema de Informação Hospitalar
## Sprint 1 — Programação Modular (Prof. Glender Brás)

Entregas desta sprint:

| Item | Onde está |
|---|---|
| 1. Front-end (telas sem funcionalidade) | `front-end/` — abrir `index.html` no navegador |
| 2. Diagrama de Classes | `diagrama/diagrama-classes.png` (imagem) e `.svg` (vetorial); fonte editável em `.puml` (PlantUML) |
| 3. Cartões CRC | `Cartoes_CRC_Sistema_Hospitalar.docx` (template do professor preenchido) |

> **Antes de entregar:** preencher os nomes dos integrantes na capa do docx.

## Modelagem (comum aos três artefatos)

Classes: `Paciente`, `ProfissionalSaude`, `Consulta`, `Internacao`, `Quarto`,
`HistoricoMedico`, e os enums `StatusConsulta` e `SituacaoQuarto`.

Relacionamentos e regras de negócio refletidas:
- `Paciente 1 — 0..* Consulta` e `1 — 0..* Internacao` (regra 1);
- `Consulta` associada a um `ProfissionalSaude` e a um `Paciente` (regra 2);
- `ProfissionalSaude.estaDisponivel(data, horario)` e `Consulta.conflitaCom(...)` dão suporte à regra 3 (sem dois atendimentos no mesmo horário);
- `Internacao` exige `Paciente` + `Quarto` disponível (regra 4), com `Quarto.temVagaDisponivel()`, `ocuparLeito()` e `liberarLeito()` garantindo a capacidade máxima (regra 5);
- `HistoricoMedico` em **composição** com `Paciente` reúne consultas e internações realizadas (regra 6).

Os atributos de cada classe seguem os mínimos do enunciado (Características e
Requisitos do Sistema), com `id: Long` já pensado na futura persistência via
Spring Boot (sprints seguintes).

## Front-end

Sete telas estáticas em HTML/CSS puro, sem JavaScript — apenas navegação entre
páginas (menu lateral), como pede a entrega "telas sem funcionalidade":
Painel, Pacientes, Profissionais, Consultas, Internações, Quartos e Histórico
Médico. Os formulários cobrem exatamente os campos mínimos do enunciado, e as
telas de Consultas e Internações anunciam as regras de negócio que serão
implementadas nas próximas sprints.

## Cartões CRC

Cinco casos de uso — Agendar Consulta, Cadastrar Paciente, Registrar
Internação, Gerenciar Quartos e Consultar Histórico Médico — com seis cartões
(Consulta, ProfissionalSaude, Paciente, Internacao, Quarto, HistoricoMedico),
consistentes com o diagrama de classes.

## Como editar o diagrama

O arquivo `diagrama/diagrama-classes.puml` pode ser editado em
https://www.plantuml.com/plantuml ou com a extensão PlantUML do VS Code, e
re-exportado para PNG/SVG.
