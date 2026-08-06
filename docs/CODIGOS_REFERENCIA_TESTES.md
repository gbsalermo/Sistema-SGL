# Códigos de Referência — Testes Automatizados do SGL

Este documento acompanha a primeira etapa de testes automatizados do backend. A proposta é implementar os testes manualmente, entender cada parte e só depois ampliar a cobertura.

## Objetivo da primeira etapa

Criar testes unitários com JUnit e Mockito para proteger as regras mais críticas de `EstoqueCentralService` e `PedidoService`.

Nesta fase:

- o Spring não é iniciado;
- nenhum servidor HTTP é iniciado;
- nenhum banco de dados é usado;
- os repositories são substituídos por mocks;
- o foco está exclusivamente na regra de negócio do Service.

## Dependência recomendada

Confirme se o `pom.xml` possui o starter abaixo. Ele fornece JUnit Jupiter, Mockito, AssertJ e outras ferramentas de teste.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

Depois de salvar o `pom.xml`, atualize o projeto Maven.

## Estrutura de pastas

Crie a seguinte estrutura:

```text
backend/sgl-backend/src/test/java/com/sgl/service/
├── EstoqueCentralServiceTest.java
└── PedidoServiceTest.java
```

O pacote deve acompanhar o pacote da classe original:

```java
package com.sgl.service;
```

---

# Parte 1 — EstoqueCentralServiceTest

## Classe completa inicial

Crie o arquivo:

```text
src/test/java/com/sgl/service/EstoqueCentralServiceTest.java
```

Use este código:

```java
package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.EstoqueCentralDTO;
import com.sgl.dto.MovimentacaoEstoqueDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.Produto;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.MovimentacaoEstoqueRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class EstoqueCentralServiceTest {

    @Mock
    private EstoqueCentralRepository estoqueCentralRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private UnidadeRepository unidadeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @InjectMocks
    private EstoqueCentralService estoqueCentralService;

    private Unidade unidade;
    private Produto produto;
    private Usuario usuario;
    private EstoqueCentral estoque;

    @BeforeEach
    void prepararCenario() {
        unidade = Unidade.builder()
                .id(1L)
                .nome("Unidade Central")
                .sigla("UC")
                .build();

        produto = Produto.builder()
                .id(10L)
                .nome("Álcool 70%")
                .unidadeArmazenamento("Frasco de 1 L")
                .ativo(true)
                .build();

        usuario = new Usuario();
        usuario.setId(20L);
        usuario.setNome("Usuário de Teste");
        usuario.setAtivo(true);

        estoque = EstoqueCentral.builder()
                .id(30L)
                .unidade(unidade)
                .produto(produto)
                .quantidadeAtual(10)
                .quantidadeMinima(2)
                .ativo(true)
                .build();
    }

    @Test
    void deveAumentarSaldoERegistrarMovimentacaoAoRealizarEntrada() {
        MovimentacaoEstoqueDTO dto = criarMovimentacaoDTO(5);

        when(estoqueCentralRepository.findById(30L))
                .thenReturn(Optional.of(estoque));

        when(usuarioRepository.findById(20L))
                .thenReturn(Optional.of(usuario));

        EstoqueCentralDTO resultado = estoqueCentralService.entrada(30L, dto);

        assertEquals(15, estoque.getQuantidadeAtual());
        assertEquals(15, resultado.getQuantidadeAtual());

        verify(estoqueCentralRepository).save(estoque);

        ArgumentCaptor<MovimentacaoEstoque> captor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);

        verify(movimentacaoEstoqueRepository).save(captor.capture());

        MovimentacaoEstoque movimentacao = captor.getValue();

        assertEquals(TipoMovimentacao.ENTRADA, movimentacao.getTipoMovimentacao());
        assertEquals(OrigemMovimentacao.COMPRA, movimentacao.getOrigem());
        assertEquals(5, movimentacao.getQuantidadeMovimentada());
        assertEquals(10, movimentacao.getQuantidadeAnterior());
        assertEquals(15, movimentacao.getQuantidadeAtual());
        assertEquals(produto, movimentacao.getProduto());
        assertEquals(usuario, movimentacao.getUsuario());
        assertEquals(estoque, movimentacao.getEstoqueCentral());
    }

    @Test
    void deveReduzirSaldoERegistrarMovimentacaoAoRealizarSaida() {
        MovimentacaoEstoqueDTO dto = criarMovimentacaoDTO(4);

        when(estoqueCentralRepository.findById(30L))
                .thenReturn(Optional.of(estoque));

        when(usuarioRepository.findById(20L))
                .thenReturn(Optional.of(usuario));

        EstoqueCentralDTO resultado = estoqueCentralService.saida(30L, dto);

        assertEquals(6, estoque.getQuantidadeAtual());
        assertEquals(6, resultado.getQuantidadeAtual());

        verify(estoqueCentralRepository).save(estoque);

        ArgumentCaptor<MovimentacaoEstoque> captor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);

        verify(movimentacaoEstoqueRepository).save(captor.capture());

        MovimentacaoEstoque movimentacao = captor.getValue();

        assertEquals(TipoMovimentacao.SAIDA, movimentacao.getTipoMovimentacao());
        assertEquals(4, movimentacao.getQuantidadeMovimentada());
        assertEquals(10, movimentacao.getQuantidadeAnterior());
        assertEquals(6, movimentacao.getQuantidadeAtual());
    }

    @Test
    void deveImpedirSaidaQuandoEstoqueForInsuficiente() {
        MovimentacaoEstoqueDTO dto = criarMovimentacaoDTO(15);

        when(estoqueCentralRepository.findById(30L))
                .thenReturn(Optional.of(estoque));

        when(usuarioRepository.findById(20L))
                .thenReturn(Optional.of(usuario));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> estoqueCentralService.saida(30L, dto)
        );

        assertEquals(
                "Estoque insuficiente. Disponível: 10, solicitado: 15",
                exception.getMessage()
        );

        assertEquals(10, estoque.getQuantidadeAtual());

        verify(estoqueCentralRepository, never()).save(any());
        verify(movimentacaoEstoqueRepository, never()).save(any());
    }

    @Test
    void deveRejeitarQuantidadeZeroAntesDeConsultarOsRepositories() {
        MovimentacaoEstoqueDTO dto = criarMovimentacaoDTO(0);

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> estoqueCentralService.saida(30L, dto)
        );

        assertEquals(
                "A quantidade deve ser maior que zero.",
                exception.getMessage()
        );

        verifyNoInteractions(
                estoqueCentralRepository,
                usuarioRepository,
                movimentacaoEstoqueRepository
        );
    }

    private MovimentacaoEstoqueDTO criarMovimentacaoDTO(Integer quantidade) {
        MovimentacaoEstoqueDTO dto = new MovimentacaoEstoqueDTO();
        dto.setUsuarioId(20L);
        dto.setQuantidadeMovimentada(quantidade);
        dto.setOrigem(OrigemMovimentacao.COMPRA);
        dto.setObservacao("Movimentação criada durante teste");
        return dto;
    }
}
```

---

## Como ler a classe

### `@ExtendWith(MockitoExtension.class)`

Ativa a integração do Mockito com o JUnit. Sem ela, os campos anotados com `@Mock` não são inicializados automaticamente.

### `@Mock`

Cria uma versão falsa de uma dependência.

```java
@Mock
private EstoqueCentralRepository estoqueCentralRepository;
```

Esse objeto não consulta banco. Ele só responde aquilo que o teste programar usando `when(...).thenReturn(...)`.

### `@InjectMocks`

Cria o Service real e injeta nele os repositories falsos.

```java
@InjectMocks
private EstoqueCentralService estoqueCentralService;
```

Portanto:

- `EstoqueCentralService` é real;
- os repositories são falsos;
- a regra de negócio é realmente executada;
- o banco não é acessado.

### `@BeforeEach`

Executa antes de cada método marcado com `@Test`.

Cada teste recebe objetos novos com o saldo inicial igual a 10. Isso impede que um teste altere os dados usados pelo próximo.

### Arrange, Act e Assert

Cada teste segue três momentos.

```text
Arrange → preparar objetos e respostas dos mocks
Act     → chamar o método do Service
Assert  → verificar resultado e interações
```

Exemplo:

```java
// Arrange
when(estoqueCentralRepository.findById(30L))
        .thenReturn(Optional.of(estoque));

// Act
EstoqueCentralDTO resultado = estoqueCentralService.saida(30L, dto);

// Assert
assertEquals(6, resultado.getQuantidadeAtual());
```

### `when(...).thenReturn(...)`

Programa o comportamento de um mock.

```java
when(estoqueCentralRepository.findById(30L))
        .thenReturn(Optional.of(estoque));
```

Significa:

```text
Quando o Service procurar o estoque 30,
retorne o objeto estoque criado no teste.
```

### `assertEquals`

Compara o valor esperado com o valor obtido.

```java
assertEquals(6, estoque.getQuantidadeAtual());
```

O teste falha caso o saldo não seja 6.

### `assertThrows`

Confirma que determinada operação lança a exceção esperada.

```java
BusinessRuleException exception = assertThrows(
        BusinessRuleException.class,
        () -> estoqueCentralService.saida(30L, dto)
);
```

O teste falha se:

- nenhuma exceção for lançada;
- for lançada uma exceção de outro tipo.

### `verify`

Confirma que um método do mock foi chamado.

```java
verify(estoqueCentralRepository).save(estoque);
```

Isso comprova que o Service tentou persistir o novo saldo.

### `never()`

Confirma que uma operação proibida não aconteceu.

```java
verify(movimentacaoEstoqueRepository, never()).save(any());
```

No cenário de estoque insuficiente, nenhuma movimentação deve ser registrada.

### `ArgumentCaptor`

Captura o objeto enviado ao repository para que seus campos possam ser verificados.

```java
ArgumentCaptor<MovimentacaoEstoque> captor =
        ArgumentCaptor.forClass(MovimentacaoEstoque.class);

verify(movimentacaoEstoqueRepository).save(captor.capture());

MovimentacaoEstoque movimentacao = captor.getValue();
```

Isso permite validar saldo anterior, saldo atual, tipo, origem, usuário e produto da movimentação criada internamente pelo Service.

---

## Como executar

Abra o terminal na pasta:

```text
backend/sgl-backend
```

No Windows com Maven Wrapper:

```bash
.\mvnw.cmd test
```

No Linux ou macOS:

```bash
./mvnw test
```

Com Maven instalado globalmente:

```bash
mvn test
```

Para executar apenas esta classe:

```bash
mvn -Dtest=EstoqueCentralServiceTest test
```

## Resultado esperado

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

# Próxima implementação

Depois que os quatro testes de estoque estiverem executando corretamente, a próxima classe será `PedidoServiceTest` com os seguintes cenários:

1. aprovar pedido pendente reduz o estoque;
2. aprovação registra movimentação `SAIDA`;
3. aprovação parcial usa a quantidade aprovada;
4. pedido fora de `PENDENTE` lança `BusinessRuleException`;
5. estoque insuficiente impede salvamento e movimentação;
6. falha em um item interrompe a aprovação.

O teste de rollback transacional real será deixado para a etapa de integração, pois um teste unitário com Mockito não abre uma transação nem usa banco de dados. Nesta primeira etapa, verificaremos que, diante da falha, o fluxo não executa as persistências posteriores.
