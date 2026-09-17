---
name: simple-layered-architecture
description: >
  Direciona a criação e organização de classes Java/Spring Boot utilizando arquitetura
  em camadas simples (Controller, Service, Repository, Entity, DTO/Record). Use sempre que
  for implementar novas funcionalidades, endpoints, entidades ou regras de negócio no projeto.
license: MIT
---

# simple-layered-architecture

Esta skill define as convenções, padrões e regras para implementação de código no backend utilizando uma arquitetura em camadas simples, limpa e desacoplada (**Controller $\rightarrow$ Service $\rightarrow$ Repository $\rightarrow$ Entity**).

---

## 1. Visão Geral das Camadas

```
[ HTTP Request / JSON ]
           │
           ▼
┌────────────────────────────────────────┐
│               Controller               │  - Recebe requisições HTTP
│  (REST Endpoints, OpenAPI, Validação)   │  - Valida DTOs de entrada (@Valid)
└──────────────────┬─────────────────────┘  - Mapeia para Response DTOs
                   │ DTO / Records
                   ▼
┌────────────────────────────────────────┐
│                Service                 │  - Regras de negócio e orquestração
│       (Lógica de Domínio, Transações)  │  - @Transactional quando aplicável
└──────────────────┬─────────────────────┘  - Isolamento por usuário (multitenancy)
                   │ Entity
                   ▼
┌────────────────────────────────────────┐
│               Repository               │  - Acesso a dados (Spring Data JPA)
│     (Spring Data JPA / pgvector)       │  - Queries derivadas e @Query nativo
└──────────────────┬─────────────────────┘
                   │ SQL / JPA
                   ▼
┌────────────────────────────────────────┐
│             Entity / Table             │  - Mapeamento objeto-relacional (@Entity)
│       (PostgreSQL + pgvector)          │  - Tabelas e constraints do banco
└────────────────────────────────────────┘
```

---

## 2. Estrutura de Pacotes

Organize por feature/domínio seguindo o padrão base `github.salessew.notebooklm.<dominio>`:

```
github.salessew.notebooklm.<dominio>/
├── controller/
│   ├── <Feature>Controller.java
│   └── dto/
│       ├── <Feature>Request.java       # Java record com anotações Jakarta Validation
│       └── <Feature>Response.java      # Java record imutável
├── service/
│   ├── <Feature>Service.java           # Interface ou Service direto
│   └── <Feature>ServiceImpl.java       # Implementação com regras de negócio
├── repository/
│   └── <Feature>Repository.java        # Interface estendendo JpaRepository
└── entity/
    └── <Feature>Entity.java            # Entidade JPA mapeando a tabela do banco
```

---

## 3. Diretrizes por Camada

### 3.1. Entity (`entity/`)
- Mapeia diretamente as tabelas do banco de dados (conforme especificado em `DOMAIN.md`).
- Utilizar anotações Jakarta Persistence (`@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`).
- Definir construtores apropriados, getters/setters e métodos auxiliares para integridade.
- Nunca expor entidades diretamente na resposta de controllers.

```java
package github.salessew.notebooklm.<dominio>.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notebooks")
public class NotebookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // Construtores, Getters e Setters
}
```

### 3.2. Repository (`repository/`)
- Interfaces estendendo `JpaRepository<Entity, ID>`.
- Métodos de busca devem respeitar rigorosamente o isolamento por usuário (`owner_id` ou `cognito_sub`).
- Usar Spring Data method queries (`findByOwnerId`, `findByIdAndOwnerId`) ou `@Query` quando houver joins específicos ou pgvector.

```java
package github.salessew.notebooklm.<dominio>.repository;

import github.salessew.notebooklm.<dominio>.entity.NotebookEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotebookRepository extends JpaRepository<NotebookEntity, UUID> {
    List<NotebookEntity> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    Optional<NotebookEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
}
```

### 3.3. DTOs / Records (`controller/dto/`)
- Sempre utilizar **Java 21 `record`** para DTOs (imutáveis por design).
- Aplicar validações com `jakarta.validation.constraints` (`@NotBlank`, `@NotNull`, `@Size`, etc.).

```java
package github.salessew.notebooklm.<dominio>.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record CreateNotebookRequest(
    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 1, max = 100, message = "O nome deve ter entre 1 e 100 caracteres")
    String name,

    @Size(max = 500, message = "A descrição não pode exceder 500 caracteres")
    String description
) {}

public record NotebookResponse(
    UUID id,
    String name,
    String description,
    Instant createdAt,
    Instant updatedAt
) {}
```

### 3.4. Service (`service/`)
- Contém toda a lógica de negócio, regras de validação e orquestração.
- Injeção de dependências via construtor (preferencialmente sem anotação explícita ou com `@Autowired` no construtor único).
- Gerenciamento declarativo de transações via `@Transactional` (com `readOnly = true` para consultas).
- Lança exceções de negócio específicas capturadas pelo `@RestControllerAdvice`.

```java
package github.salessew.notebooklm.<dominio>.service;

import github.salessew.notebooklm.<dominio>.controller.dto.CreateNotebookRequest;
import github.salessew.notebooklm.<dominio>.controller.dto.NotebookResponse;
import github.salessew.notebooklm.<dominio>.entity.NotebookEntity;
import github.salessew.notebooklm.<dominio>.repository.NotebookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotebookService {

    private final NotebookRepository notebookRepository;

    public NotebookService(NotebookRepository notebookRepository) {
        this.notebookRepository = notebookRepository;
    }

    @Transactional(readOnly = true)
    public List<NotebookResponse> listByOwner(UUID ownerId) {
        return notebookRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NotebookResponse create(UUID ownerId, CreateNotebookRequest request) {
        var entity = new NotebookEntity();
        entity.setOwnerId(ownerId);
        entity.setName(request.name());
        entity.setDescription(request.description());

        var saved = notebookRepository.save(entity);
        return toResponse(saved);
    }

    private NotebookResponse toResponse(NotebookEntity entity) {
        return new NotebookResponse(
            entity.getId(),
            entity.getName(),
            entity.getDescription(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
```

### 3.5. Controller (`controller/`)
- Exposto via `@RestController` e `@RequestMapping`.
- Valida entradas com `@Valid`.
- Nunca contém lógica de negócio ou chamadas diretas a repositórios.
- Retorna `ResponseEntity` com status codes HTTP adequados (`200 OK`, `201 Created`, `202 Accepted`, `204 No Content`).

```java
package github.salessew.notebooklm.<dominio>.controller;

import github.salessew.notebooklm.<dominio>.controller.dto.CreateNotebookRequest;
import github.salessew.notebooklm.<dominio>.controller.dto.NotebookResponse;
import github.salessew.notebooklm.<dominio>.service.NotebookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notebooks")
public class NotebookController {

    private final NotebookService notebookService;

    public NotebookController(NotebookService notebookService) {
        this.notebookService = notebookService;
    }

    @GetMapping
    public ResponseEntity<List<NotebookResponse>> list() {
        UUID authenticatedUserId = getAuthenticatedUserId(); // Extraído do JWT / SecurityContext
        return ResponseEntity.ok(notebookService.listByOwner(authenticatedUserId));
    }

    @PostMapping
    public ResponseEntity<NotebookResponse> create(@Valid @RequestBody CreateNotebookRequest request) {
        UUID authenticatedUserId = getAuthenticatedUserId();
        var response = notebookService.create(authenticatedUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private UUID getAuthenticatedUserId() {
        // Obter cognito_sub do SecurityContextHolder / JwtAuthenticationToken
        return UUID.randomUUID();
    }
}
```

---

## 4. Regras Obrigatórias e Invariantes

1. **Separação Estrita de Responsabilidades:**
   - Controller valida requisição e delega.
   - Service executa lógica e regras de negócio.
   - Repository apenas persiste e recupera dados.
   - Entity reflete o modelo persistente; nunca vaza diretamente na resposta HTTP.
2. **Isolamento Multitenancy:**
   - Toda query e alteração de dados DEVE considerar o ID do usuário autenticado (`owner_id`).
3. **Padrão Async Request-Reply (quando aplicável):**
   - Para processos de longa duração (ex: ingestão de arquivos/URLs), o controller retorna `HTTP 202 Accepted` e delega para processamento assíncrono via Service.
4. **Testes e Qualidade:**
   - Seguir a skill `java-quality-gate` para escrever testes unitários JUnit 5 + Mockito cobrindo Services e componentes com lógica de branch, garantindo 100% de cobertura e aprovação no quality gate.
