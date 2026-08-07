# Códigos de Referência — Lote

Este documento serve como guia para a implementação inicial do módulo `Lote` no SGL.

> **Importante:** os códigos abaixo são referências de implementação e ainda não representam código aplicado ao backend.

A modelagem segue as decisões atuais do projeto:

```text
Produto
  └── EstoqueCentral (Unidade + Produto)
        ├── Lote A
        ├── Lote B
        └── Lote C
```

O `EstoqueCentral` mantém o saldo agregado persistido.

```text
EstoqueCentral.quantidadeAtual
=
soma de Lote.quantidadeDisponivel
```

A entrada física não deverá ser um simples `save` de lote. Ela deverá ser coordenada posteriormente pelo `MovimentacaoEstoqueService`, porque precisa criar o lote, atualizar o saldo agregado e registrar a movimentação na mesma transação.

---

## 1. Entidade `Lote`

Arquivo sugerido:

```text
src/main/java/com/sgl/model/Lote.java
```

```java
package com.sgl.model;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "lote",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_lote_estoque_numero",
                        columnNames = {"estoque_central_id", "numero_lote"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lote implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * O lote pertence a um EstoqueCentral específico.
     *
     * Através do EstoqueCentral já conseguimos descobrir Produto e Unidade,
     * portanto não é necessário repetir essas duas relações inicialmente.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estoque_central_id", nullable = false)
    private EstoqueCentral estoqueCentral;

    @Column(name = "numero_lote", nullable = false, length = 100)
    private String numeroLote;

    /* Quantidade recebida originalmente. Não deve diminuir após a entrada. */
    @Column(nullable = false)
    private Integer quantidadeInicial;

    /* Quantidade que ainda pode ser utilizada. */
    @Column(nullable = false)
    private Integer quantidadeDisponivel;

    @Column(nullable = false)
    private LocalDate dataEntrada;

    /*
     * Pode ser nula para produtos que não tenham controle de validade.
     * Para produto perecível, o Service deverá exigir o preenchimento.
     */
    private LocalDate dataValidade;

    @Column(nullable = false)
    private Boolean ativo = true;
}
```

### Por que o lote aponta para `EstoqueCentral`?

Porque o estoque já representa:

```text
Unidade + Produto
```

Logo:

```text
Lote
→ EstoqueCentral
→ Unidade
→ Produto
```

Isso evita armazenar no lote relações redundantes para `Produto` e `Unidade`.

---

## 2. DTO de leitura do lote

Arquivo sugerido:

```text
src/main/java/com/sgl/dto/LoteDTO.java
```

```java
package com.sgl.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sgl.model.Lote;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoteDTO {

    private Long id;

    private Long estoqueCentralId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long produtoId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String produtoNome;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long unidadeId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String unidadeNome;

    private String numeroLote;

    private Integer quantidadeInicial;

    private Integer quantidadeDisponivel;

    private LocalDate dataEntrada;

    private LocalDate dataValidade;

    private Boolean ativo;

    public LoteDTO(Lote entity) {
        this.id = entity.getId();
        this.estoqueCentralId = entity.getEstoqueCentral().getId();

        this.produtoId = entity.getEstoqueCentral().getProduto().getId();
        this.produtoNome = entity.getEstoqueCentral().getProduto().getNome();

        this.unidadeId = entity.getEstoqueCentral().getUnidade().getId();
        this.unidadeNome = entity.getEstoqueCentral().getUnidade().getNome();

        this.numeroLote = entity.getNumeroLote();
        this.quantidadeInicial = entity.getQuantidadeInicial();
        this.quantidadeDisponivel = entity.getQuantidadeDisponivel();
        this.dataEntrada = entity.getDataEntrada();
        this.dataValidade = entity.getDataValidade();
        this.ativo = entity.getAtivo();
    }
}
```

---

## 3. DTO específico para entrada física

Não é recomendado usar `LoteDTO` diretamente para registrar entrada.

A entrada é um caso de uso próprio e terá regras adicionais.

Arquivo sugerido:

```text
src/main/java/com/sgl/dto/EntradaLoteDTO.java
```

```java
package com.sgl.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntradaLoteDTO {

    @NotBlank(message = "Número do lote é obrigatório")
    private String numeroLote;

    @NotNull(message = "Quantidade da entrada é obrigatória")
    @Min(value = 1, message = "Quantidade da entrada deve ser maior que zero")
    private Integer quantidade;

    private LocalDate dataValidade;

    /*
     * Temporário enquanto a autenticação local ainda não fornece o usuário
     * pelo contexto autenticado.
     */
    @NotNull(message = "Usuário responsável é obrigatório")
    private Long usuarioId;

    private String observacao;
}
```

Quando a autenticação estiver pronta, `usuarioId` deverá deixar de vir do cliente e passar a ser obtido do contexto autenticado.

---

## 4. DTO para correção de dados cadastrais

Quantidade não deve ser alterada através de um `PUT` genérico.

Arquivo sugerido:

```text
src/main/java/com/sgl/dto/AtualizarLoteDTO.java
```

```java
package com.sgl.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AtualizarLoteDTO {

    @NotBlank(message = "Número do lote é obrigatório")
    private String numeroLote;

    private LocalDate dataValidade;

    private Boolean ativo;
}
```

Não incluir:

```text
quantidadeInicial
quantidadeDisponivel
estoqueCentralId
```

Esses dados não devem ser livremente editáveis.

---

## 5. Repository

Arquivo sugerido:

```text
src/main/java/com/sgl/repository/LoteRepository.java
```

```java
package com.sgl.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.Lote;

import jakarta.persistence.LockModeType;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {

    List<Lote> findByEstoqueCentralId(Long estoqueCentralId);

    List<Lote> findByEstoqueCentralIdAndAtivoTrue(Long estoqueCentralId);

    Optional<Lote> findByEstoqueCentralIdAndNumeroLote(
            Long estoqueCentralId,
            String numeroLote
    );

    boolean existsByEstoqueCentralIdAndNumeroLote(
            Long estoqueCentralId,
            String numeroLote
    );

    List<Lote> findByDataValidadeBeforeAndAtivoTrue(LocalDate data);

    /*
     * Consulta base para FEFO.
     *
     * Retorna somente lotes ativos com saldo positivo, ordenando primeiro os
     * que possuem validade mais próxima.
     *
     * A versão definitiva precisará definir o comportamento dos lotes sem
     * dataValidade para produtos não perecíveis.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT lote
            FROM Lote lote
            WHERE lote.estoqueCentral.id = :estoqueId
              AND lote.ativo = true
              AND lote.quantidadeDisponivel > 0
              AND lote.dataValidade IS NOT NULL
            ORDER BY lote.dataValidade ASC, lote.id ASC
            """)
    List<Lote> buscarDisponiveisPorFefoComBloqueio(
            @Param("estoqueId") Long estoqueId
    );
}
```

### Observação sobre produtos sem validade

Para produtos não perecíveis, podemos posteriormente criar outra consulta usando FIFO pela data de entrada:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
        SELECT lote
        FROM Lote lote
        WHERE lote.estoqueCentral.id = :estoqueId
          AND lote.ativo = true
          AND lote.quantidadeDisponivel > 0
        ORDER BY lote.dataEntrada ASC, lote.id ASC
        """)
List<Lote> buscarDisponiveisPorEntradaComBloqueio(
        @Param("estoqueId") Long estoqueId
);
```

A regra poderá ser:

```text
Produto perecível     → FEFO
Produto não perecível → FIFO por data de entrada
```

---

## 6. `LoteService`

O `LoteService` deve cuidar principalmente de consulta e manutenção cadastral do lote.

Ele não deve permitir alteração livre das quantidades.

Arquivo sugerido:

```text
src/main/java/com/sgl/service/LoteService.java
```

```java
package com.sgl.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.AtualizarLoteDTO;
import com.sgl.dto.LoteDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Lote;
import com.sgl.repository.LoteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoteService {

    private final LoteRepository loteRepository;

    @Transactional(readOnly = true)
    public List<LoteDTO> listarTodos() {
        return loteRepository.findAll()
                .stream()
                .map(LoteDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoteDTO buscarPorId(Long id) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Lote", id));

        return new LoteDTO(lote);
    }

    @Transactional(readOnly = true)
    public List<LoteDTO> listarPorEstoque(Long estoqueId) {
        return loteRepository.findByEstoqueCentralId(estoqueId)
                .stream()
                .map(LoteDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoteDTO> listarVencidos() {
        return loteRepository
                .findByDataValidadeBeforeAndAtivoTrue(LocalDate.now())
                .stream()
                .map(LoteDTO::new)
                .toList();
    }

    @Transactional
    public LoteDTO atualizar(Long id, AtualizarLoteDTO dto) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Lote", id));

        boolean numeroDuplicado =
                loteRepository.existsByEstoqueCentralIdAndNumeroLote(
                        lote.getEstoqueCentral().getId(),
                        dto.getNumeroLote()
                ) && !lote.getNumeroLote().equals(dto.getNumeroLote());

        if (numeroDuplicado) {
            throw new BusinessRuleException(
                    "Já existe lote com esse número neste estoque."
            );
        }

        lote.setNumeroLote(dto.getNumeroLote());
        lote.setDataValidade(dto.getDataValidade());

        if (dto.getAtivo() != null) {
            lote.setAtivo(dto.getAtivo());
        }

        return new LoteDTO(loteRepository.save(lote));
    }

    @Transactional
    public void inativar(Long id) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Lote", id));

        if (lote.getQuantidadeDisponivel() > 0) {
            throw new BusinessRuleException(
                    "Lote com saldo disponível não pode ser inativado diretamente."
            );
        }

        lote.setAtivo(false);
    }
}
```

Observe que não existe neste Service:

```java
criarEntrada(...)
alterarQuantidade(...)
baixarQuantidade(...)
```

Essas operações pertencem ao fluxo de movimentação física.

---

## 7. Controller de lote

Arquivo sugerido:

```text
src/main/java/com/sgl/controller/LoteController.java
```

```java
package com.sgl.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.AtualizarLoteDTO;
import com.sgl.dto.LoteDTO;
import com.sgl.service.LoteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/lotes")
@RequiredArgsConstructor
public class LoteController {

    private final LoteService loteService;

    @GetMapping
    public ResponseEntity<List<LoteDTO>> listarTodos() {
        return ResponseEntity.ok(loteService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoteDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(loteService.buscarPorId(id));
    }

    @GetMapping("/por-estoque")
    public ResponseEntity<List<LoteDTO>> listarPorEstoque(
            @RequestParam Long estoqueId) {
        return ResponseEntity.ok(loteService.listarPorEstoque(estoqueId));
    }

    @GetMapping("/vencidos")
    public ResponseEntity<List<LoteDTO>> listarVencidos() {
        return ResponseEntity.ok(loteService.listarVencidos());
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoteDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarLoteDTO dto) {
        return ResponseEntity.ok(loteService.atualizar(id, dto));
    }
}
```

A inativação pode ser exposta depois, quando a regra operacional estiver fechada.

---

## 8. Endpoint futuro de entrada física

A criação física do lote deverá passar por `MovimentacaoEstoqueService`.

Uma opção de endpoint:

```text
POST /api/v1/estoques/{estoqueId}/lotes
```

Exemplo de Controller futuro:

```java
@PostMapping("/api/v1/estoques/{estoqueId}/lotes")
public ResponseEntity<LoteDTO> registrarEntrada(
        @PathVariable Long estoqueId,
        @Valid @RequestBody EntradaLoteDTO dto) {

    LoteDTO lote = movimentacaoEstoqueService
            .registrarEntradaLote(estoqueId, dto);

    return ResponseEntity.status(HttpStatus.CREATED).body(lote);
}
```

O método de referência do futuro Service deverá ter aproximadamente esta responsabilidade:

```java
@Transactional
public LoteDTO registrarEntradaLote(
        Long estoqueId,
        EntradaLoteDTO dto) {

    // 1. validar quantidade
    // 2. localizar e bloquear EstoqueCentral
    // 3. validar produto e validade
    // 4. impedir lote duplicado no mesmo estoque
    // 5. localizar usuário responsável
    // 6. criar Lote
    // 7. quantidadeInicial = quantidade recebida
    // 8. quantidadeDisponivel = quantidade recebida
    // 9. aumentar EstoqueCentral.quantidadeAtual
    // 10. criar MovimentacaoEstoque ENTRADA
    // 11. persistir tudo na mesma transação

    return null;
}
```

Esse método substituirá conceitualmente o atual:

```java
EstoqueCentralService.entrada(...)
```

---

## 9. Regras mínimas de criação do lote

Ao registrar uma entrada física, validar:

```text
quantidade > 0
estoque existe
estoque está ativo
produto está ativo
número do lote preenchido
número do lote não duplicado naquele estoque
produto perecível → dataValidade obrigatória
produto perecível → lote não pode entrar já vencido, salvo regra explícita futura
usuário responsável existe e está ativo
```

A data de entrada deve preferencialmente ser gerada pelo backend:

```java
dataEntrada = LocalDate.now();
```

---

## 10. FEFO — referência para a saída

Exemplo de algoritmo que será implementado posteriormente em `MovimentacaoEstoqueService`:

```java
int restante = quantidadeSolicitada;

List<Lote> lotes = loteRepository
        .buscarDisponiveisPorFefoComBloqueio(estoqueId);

for (Lote lote : lotes) {

    if (restante == 0) {
        break;
    }

    int disponivel = lote.getQuantidadeDisponivel();
    int retirar = Math.min(disponivel, restante);

    lote.setQuantidadeDisponivel(disponivel - retirar);
    restante -= retirar;
}

if (restante > 0) {
    throw new BusinessRuleException("Estoque insuficiente.");
}
```

Esse é apenas o núcleo da seleção FEFO. A implementação real ainda deverá:

- validar o saldo agregado antes da baixa;
- ignorar lotes vencidos para saídas normais;
- bloquear estoque e lotes;
- registrar quais lotes foram consumidos;
- atualizar `EstoqueCentral.quantidadeAtual`;
- criar `MovimentacaoEstoque`;
- realizar rollback integral em caso de erro.

---

## 11. Próximos testes sugeridos

Depois da implementação inicial de `Lote`:

```text
LoteServiceTest

- deveBuscarLotePorId
- deveListarLotesPorEstoque
- deveListarLotesVencidos
- deveAtualizarDadosCadastraisDoLote
- deveImpedirNumeroDuplicadoNoMesmoEstoque
- deveImpedirInativacaoComSaldoDisponivel
```

Depois da entrada migrar para `MovimentacaoEstoqueService`:

```text
MovimentacaoEstoqueServiceTest

- deveCriarLoteEAumentarSaldoAoRegistrarEntrada
- deveRegistrarMovimentacaoDeEntrada
- deveExigirValidadeParaProdutoPerecivel
- deveImpedirEntradaComQuantidadeZero
- deveImpedirLoteDuplicadoNoMesmoEstoque
- deveImpedirEntradaPorUsuarioInativo
```

Depois da implementação FEFO:

```text
- deveConsumirPrimeiroOLoteQueVenceAntes
- deveConsumirMaisDeUmLoteQuandoNecessario
- deveIgnorarLoteSemSaldo
- deveIgnorarLoteVencidoNaSaidaNormal
- deveImpedirSaidaQuandoSomaDosLotesForInsuficiente
```

---

## 12. Ordem recomendada para implementar

```text
1. Lote entity
2. LoteDTO + AtualizarLoteDTO + EntradaLoteDTO
3. LoteRepository
4. LoteService somente para consultas/manutenção cadastral
5. LoteController
6. testes básicos de LoteService
7. MovimentacaoEstoqueService
8. entrada física por lote
9. remover EstoqueCentralService.entrada()
10. testes da nova entrada
11. saída FEFO
12. adaptação de PedidoService
13. descarte por lote
14. cancelamento/devolução por lote
15. PostgreSQL + Flyway
```

---

## Regra arquitetural central

O ponto mais importante da nova modelagem é:

```text
Lote descreve ONDE está a quantidade e QUAL é sua validade.
EstoqueCentral informa QUANTO existe no total.
MovimentacaoEstoque registra O QUE aconteceu.
MovimentacaoEstoqueService executa COMO a quantidade mudou.
```

Essa separação deve orientar as próximas alterações no backend.
