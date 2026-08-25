# Coralink API

## 1. Visao Geral

O Coralink API e um servico backend desenvolvido em Java 21 com Spring Boot, projetado para agregar, filtrar, estruturar e disponibilizar oportunidades academicas e profissionais voltadas a estudantes do ensino superior da Regiao Metropolitana do Recife (RMR), englobando Recife, Olinda e Paulista.

O sistema monitora continuamente portais institucionais e centros tecnologicos de referencia (como CIn-UFPE, UFPE, IFPE, UPE, Porto Digital, CESAR School, UNIBRA e UNIFAFIRE), extrai noticias brutas, realiza triagem semantica utilizando Modelos de Linguagem (Google Gemini via Spring AI) e consolida oportunidades categorizadas (cursos, estagios, editais, hackathons, palestras e bolsas) em uma API REST de alta performance.

---

## 2. Arquitetura do Sistema

A aplicacao adota uma arquitetura em camadas orientada a servicos (Service-Layered Architecture), combinada com pipelines agendados de ingestao de dados e processamento assincrono.

### Diagrama Arquitetural de Alto Nivel

```
+-----------------------------------------------------------------------------------+
|                                 Fontes Externas                                   |
|   (Portais WordPress REST API / Paginas HTML Institucionais / Centros de Inovacao) |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          | Jsoup / HTTP Client
                                          v
+-----------------------------------------------------------------------------------+
|                               Camada de Ingestao                                  |
|  - Collectors Especializados (CIn, UFPE, IFPE, UPE, Porto Digital, CESAR, etc.)   |
|  - ScrapingService: Ingestao e deduplicacao de URLs em tempo real                 |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          | Raw Opportunities
                                          v
+-----------------------------------------------------------------------------------+
|                        Pipeline de Processamento com IA                           |
|  1. ScreeningService: Classificacao binaria de relevancia pratica (Gemini Flash)  |
|  2. ExtractionService: Extracao de entidades, datas, público e modalidade (Gemini)|
|  3. PipelinePersistenceService: Gravacao transacional isolada (@Transactional)     |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          | Entidades Consolidadas
                                          v
+-----------------------------------------------------------------------------------+
|                               Camada de Persistencia                              |
|  - PostgreSQL 16: Modelo relacional normalizado                                   |
|  - Flyway: Controle de versao e migracao automatica de schema                     |
|  - Spring Data JPA + Specifications: Consultas dinamicas com predicados compostos |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          | Caffeine Cache / DTO Mappers
                                          v
+-----------------------------------------------------------------------------------+
|                               Camada de Apresentacao                              |
|  - REST Controllers: /opportunities, /suggestion, /admin                          |
|  - RateLimitFilter: Protecao com Bucket4j e deteccao de IP via X-Forwarded-For    |
|  - GlobalExceptionHandler: Padronizacao uniforme de erros (RFC 7807 / ApiError)   |
|  - OpenAPI / Swagger UI: Documentacao interativa de contratos                     |
+-----------------------------------------------------------------------------------+
```

---

## 3. Pipeline de Scraping e Inteligencia Artificial

O pipeline e executado automaticamente em intervalos programados (configurado via propriedade `coralink.scheduler.source-check-rate-ms`, com padrao de 12 horas) ou sob demanda.

### Fluxo Detalhado de Processamento

```
[ Scheduled Trigger / Run Pipeline ]
                |
                v
+-------------------------------+
| Fase 1: Coleta e Ingestao     |
+-------------------------------+
  - Os coletores herdam de WordPressCollector ou HtmlCollector.
  - As fontes sao consultadas para recuperar titulos, resumos breves e URLs.
  - ScrapingService consulta o repositorio para identificar URLs ja cadastradas.
  - Novas noticias sao persistidas na tabela `raw_opportunities` com status `screened_relevant = NULL`.
                |
                v
+-------------------------------+
| Fase 2: Triagem (Screening)   |
+-------------------------------+
  - Recupera registros em `raw_opportunities` onde `screened_relevant IS NULL`.
  - Agrupa os registros em lotes (batches de ate 10 itens).
  - Executa chamada ao LLM (Google Gemini) com prompt especializado em avaliar o impacto
    pratico do conteudo na vida do universitario (excluindo noticias puramente institucionais
    ou sem acao direta para o aluno).
  - Atualiza `screened_relevant` como `TRUE` ou `FALSE`.
                |
                v
+-------------------------------+
| Fase 3: Extracao Estruturada  |
+-------------------------------+
  - Recupera registros com `screened_relevant = TRUE` e `became_opportunity = FALSE`.
  - Para cada item relevante, o coletor executa `detailedCollect(url)` para recuperar o texto
    integral e imagem destacada da noticia.
  - O conteudo completo e enviado ao LLM para extracao de metadados tipados:
    * Resumo conciso focado no estudante.
    * Tipo de oportunidade (OpportunityType).
    * Area tematica (ex: Desenvolvimento Web, Ciencia de Dados).
    * Publico-alvo categorizado (TargetCourseAudience).
    * Modalidade (ONLINE, IN_PERSON, HYBRID).
    * Datas de inicio, termino e prazo de inscricao (formato ISO-8601).
    * Localidade e gratuidade (isFree).
    * Escopo de acesso (isForAll).
    * Indice de confianca da extracao (confidenceScore).
  - PipelinePersistenceService salva a entidade final em `opportunities` e marca
    `became_opportunity = TRUE` na tabela bruta.
  - Invalida a regiao de cache correspondente para garantir consistencia aos clientes da API.
```

---

## 4. Catalogo de Endpoints da API REST

### Base URL: `/`

---

### 4.1 Oportunidades (`/opportunities`)

#### `GET /opportunities`
Recupera uma lista paginada de oportunidades ativas e relevantes, com suporte a filtros combinados.

* **Parametros de Consulta (Query Params):**
  * `type` (opcional): Tipo da oportunidade (ex: `COURSE`, `HACKATHON`, `INTERNSHIP_PROGRAM`, `WORKSHOP`, `SCHOLARSHIP`, `EDITAL`).
  * `targetCourseAudience` (opcional): Publico-alvo academico (ex: `ADS`, `COMPUTER_SCIENCE`, `SOFTWARE_ENGINEERING`, `UNIVERSITY_STUDENTS`).
  * `modality` (opcional): Modalidade (`ONLINE`, `IN_PERSON`, `HYBRID`).
  * `isFree` (opcional): Booleano indicando gratuidade (`true` ou `false`).
  * `isForAll` (opcional): `true` para oportunidades abertas ao publico amplo; `false` para oportunidades restritas a alunos da propria instituicao.
  * `page` (opcional, padrao 0): Indice da pagina.
  * `size` (opcional, padrao 20): Quantidade de itens por pagina.
  * `sort` (opcional): Campo de ordenacao (ex: `startDate,desc`, `createdAt,desc`).

* **Resposta de Sucesso (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "title": "Programa de Estagio em Engenharia de Software 2026",
      "summary": "Inscricoes abertas para estagio presencial no Porto Digital voltado a alunos de tecnologia.",
      "type": "INTERNSHIP_PROGRAM",
      "thematicArea": "Engenharia de Software",
      "targetCourseAudiences": [
        "COMPUTER_SCIENCE",
        "SOFTWARE_ENGINEERING",
        "ADS"
      ],
      "modality": "IN_PERSON",
      "startDate": "2026-09-01",
      "endDate": "2027-08-31",
      "registrationDeadline": "2026-08-30",
      "location": "Bairro do Recife, Recife - PE",
      "officialUrl": "https://www.portodigital.org/noticias/exemplo",
      "sourceName": "PORTO_DIGITAL",
      "imageUrl": "https://www.portodigital.org/imagem.png",
      "isFree": true,
      "isForAll": true
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

#### `GET /opportunities/{id}`
Obtem os detalhes completos de uma oportunidade especifica pelo seu identificador unico.

* **Parametros de Caminho (Path Params):**
  * `id` (obrigatorio): Identificador numerico da oportunidade.

* **Resposta de Sucesso (200 OK):**
```json
{
  "id": 1,
  "title": "Programa de Estagio em Engenharia de Software 2026",
  "summary": "Inscricoes abertas para estagio presencial no Porto Digital voltado a alunos de tecnologia.",
  "type": "INTERNSHIP_PROGRAM",
  "thematicArea": "Engenharia de Software",
  "targetCourseAudiences": [
    "COMPUTER_SCIENCE",
    "SOFTWARE_ENGINEERING",
    "ADS"
  ],
  "modality": "IN_PERSON",
  "startDate": "2026-09-01",
  "endDate": "2027-08-31",
  "registrationDeadline": "2026-08-30",
  "location": "Bairro do Recife, Recife - PE",
  "officialUrl": "https://www.portodigital.org/noticias/exemplo",
  "sourceName": "PORTO_DIGITAL",
  "imageUrl": "https://www.portodigital.org/imagem.png",
  "isFree": true,
  "isForAll": true
}
```

* **Resposta de Erro (404 Not Found):**
```json
{
  "timestamp": "2026-08-24T22:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Not found opportunity if id: 99"
}
```

---

#### `GET /opportunities/search`
Realiza busca textual por titulo de oportunidades com paginacao.

* **Parametros de Consulta (Query Params):**
  * `title` (obrigatorio): Termo de busca no titulo (insensivel a maiusculas/minusculas).
  * `page`, `size`, `sort`: Parametros padrao de paginacao.

---

#### `GET /opportunities/quantity`
Retorna a quantidade numerica de oportunidades ativas/futuras disponiveis no sistema.

* **Resposta de Sucesso (200 OK):**
```json
42
```

---

### 4.2 Sugestoes e Suporte do Usuario (`/suggestion`)

#### `POST /suggestion/create`
Registra uma nova sugestao, relato de problema ou feedback de usuario.

* **Corpo da Requisicao (Request Body):**
```json
{
  "type": "FEATURE",
  "suggestion": "Gostaria de poder filtrar oportunidades especificas por bairro da RMR.",
  "userEmail": "estudante@ufpe.br"
}
```

* **Validacoes:**
  * `type`: Obrigatorio (`FEATURE`, `BUG`, `OPINION`, `OTHER`).
  * `suggestion`: Obrigatorio, nao vazio, maximo de 5000 caracteres.
  * `userEmail`: Opcional, formato de email valido se preenchido.

* **Resposta de Sucesso (200 OK):**
```json
{
  "id": 1,
  "type": "FEATURE",
  "suggestion": "Gostaria de poder filtrar oportunidades especificas por bairro da RMR.",
  "userEmail": "estudante@ufpe.br"
}
```

---

#### `GET /suggestion`
Lista paginada de todas as sugestoes submetidas.

---

#### `GET /suggestion/{type}`
Lista paginada de sugestoes filtradas por categoria (`FEATURE`, `BUG`, `OPINION`, `OTHER`).

---

### 4.3 Administracao e Monitoramento (`/admin`)

#### `GET /admin/healthy-check`
Verifica a integridade operacional da aplicacao.

* **Resposta de Sucesso (200 OK):**
```text
Everything is OK
```

---

## 5. Modelo de Dados

As entidades sao gerenciadas via PostgreSQL com versionamento de schema pelo Flyway:

1. **`raw_opportunities`**: Armazena as noticias brutas extraidas dos coletores antes do processamento pela IA.
   * `id`: Chave primaria auto-incremental.
   * `title`: Titulo original da publicacao.
   * `short_summary`: Resumo inicial extraido do RSS/HTML.
   * `news_url`: URL unica da publicacao fonte (restricao UNIQUE).
   * `source_name`: Identificador da instituicao (enum SourceName).
   * `screened_relevant`: Resultado da triagem semantica (`TRUE`, `FALSE` ou `NULL`).
   * `became_opportunity`: Flag indicando se a noticia gerou registro final.
   * `found_at`: Data e hora da identificacao da noticia.

2. **`opportunities`**: Registro final da oportunidade estruturada e pronta para consumo.
   * `id`: Chave primaria auto-incremental.
   * `raw_opportunity_id`: Referencia 1:1 para a noticia bruta de origem.
   * `title`, `summary`, `thematic_area`, `location`, `official_url`, `image_url`.
   * `type`: Categoria formal (OpportunityType).
   * `modality`: Modalidade (Modality: `ONLINE`, `IN_PERSON`, `HYBRID`).
   * `start_date`, `end_date`, `registration_deadline`: Controle temporal.
   * `confidence_score_ai`: Grau de confianca retornado pelo modelo de linguagem (0.0 a 1.0).
   * `is_free`, `is_for_all`: Regras de custo e abrangencia institucional.
   * `created_at`: Carimbo de data/hora de criacao.

3. **`opportunity_target_audiences`**: Tabela associativa de relacionamento 1:N contendo as areas e cursos aos quais a oportunidade se destina (TargetCourseAudience).

4. **`user_help`**: Tabela de registro de sugestoes e feedbacks de usuarios.

---

## 6. Seguranca, Rate Limiting e Resiliencia

* **Rate Limiting por IP (Bucket4j + Caffeine Cache):**
  * Limite padrao de 20 requisicoes por minuto por endereco IP.
  * O filtro inspeciona os cabecalhos `X-Forwarded-For` e `X-Real-IP` para operar de forma transparente e justa atras de balanceadores de carga e proxies reversos (Vercel, AWS ALB, Nginx, Cloudflare).
  * Os buckets em memoria possuem expiracao automatica baseada em tempo de inatividade via Caffeine Cache, impedindo vazamentos de memoria (memory leaks).
  * Endpoints de documentacao (`/swagger-ui/**`, `/v3/api-docs/**`) sao desonerados da cota de requisicoes.

* **Tratamento Centralizado de Excecoes:**
  * O `GlobalExceptionHandler` intercepta erros de validacao de campos (`MethodArgumentNotValidException`), recursos nao encontrados (`NotFoundException`), falhas em servicos upstream (`CollectException`, `AiCallException`) e inconsistencias de payload, retornando sempre a estrutura padronizada `ApiError`.

---

## 7. Configuracao e Execucao do Projeto

### Pre-requisitos
* Java Development Kit (JDK) versao 21.
* Apache Maven 3.9+ (ou utilizacao do wrapper `./mvnw`).
* Docker e Docker Compose (para execucao em containers ou banco PostgreSQL local).
* Chave de API do Google Gemini (Google AI Studio).

### 7.1 Variaveis de Ambiente

Crie ou configure o arquivo `.env` na raiz do projeto:

```env
SPRING_PROFILES_ACTIVE=dev
GEMINI_API_KEY=sua_chave_gemini_aqui
DB_URL=jdbc:postgresql://localhost:5432/coralink
DB_USERNAME=coralink
DB_PASSWORD=coralink
```

### 7.2 Execucao com Docker Compose

Para inicializar a aplicacao e a base de dados PostgreSQL em ambiente conteinerizado:

```bash
docker compose up --build -d
```

A API ficara disponivel em `http://localhost:8080`.

### 7.3 Execucao Local para Desenvolvimento

1. Inicialize apenas o banco de dados via Docker:
```bash
docker compose up db -d
```

2. Execute a aplicacao Spring Boot com o perfil de desenvolvimento:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 7.4 Documentacao Interativa OpenAPI / Swagger

Com a aplicacao em execucao, a interface interativa do Swagger pode ser acessada em:
```
http://localhost:8080/swagger-ui/index.html
```