# 📋 Checklist - Estrutura Base Spring Boot

## ✅ Status: Em Andamento

---

## 1. Criar Projeto no Spring Initializr
- [x] Acessar https://start.spring.io/
- [x] Configurar Group: `com.sgl`
- [x] Configurar Artifact: `sgl-backend`
- [x] Selecionar Java 17
- [x] Selecionar Spring Boot 4.1.0
- [x] Adicionar dependências (ver lista abaixo)
- [x] Gerar e baixar arquivo .zip
- [x] Descompactar na pasta `backend/sgl-backend/`

## 2. Dependências Selecionadas
### Core
- [x] Spring Web
- [x] Spring Data JPA
- [x] Validation

### Banco de Dados
- [x] PostgreSQL Driver
- [x] H2 Database

### Segurança
- [x] Spring Security
- [x] OAuth2 Client

### Outros
- [x] Lombok
- [ ] MapStruct (opcional)
- [ ] SpringDoc OpenAPI

### Testes
- [x] Spring Boot Test
- [ ] Testcontainers (opcional)

## 3. Configurar Projeto no IDE
- [x] Abrir projeto no IDE
- [x] Importar como projeto Maven
- [x] Aguardar download de dependências
- [x] Verificar se compila sem erros (mvn clean compile)

## 4. Configurar application.properties
- [x] Criar arquivo `application.properties` (já existe)
- [x] Configurar servidor (porta 8080)
- [x] Configurar banco H2 (desenvolvimento)
- [x] Configurar JPA (hibernate.ddl-auto)
- [x] Habilitar H2 Console

## 5. Configurar SecurityConfig (para H2 Console)
- [x] Criar `SecurityConfig.java`
- [x] Desabilitar CSRF
- [x] Permitir acesso ao H2 Console
- [x] Desabilitar frames

## 6. Testar Aplicação
- [x] Executar `mvn clean compile`
- [x] Executar `mvn spring-boot:run`
- [x] Acessar http://localhost:8080
- [x] Verificar se app inicia sem erros
- [x] Acessar H2 Console: http://localhost:8080/h2-console
- [x] Login no H2 Console (JDBC URL: jdbc:h2:mem:sgldb, User: sa, Password: vazio)

## 7. Verificar Estrutura Criada
```
backend/sgl-backend/
├── src/
│   ├── main/
│   │   ├── java/com/sgl/
│   │   │   ├── SglApplication.java
│   │   │   └── config/
│   │   │       └── SecurityConfig.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── static/
│   │       └── templates/
│   └── test/
│       └── java/com/sgl/
│           └── SglApplicationTests.java
├── pom.xml
└── .mvn/wrapper/
```

---

## 📝 Observações
- **Responsável:** Você (estudante/desenvolvedor)
- **Orientador:** Mimo (documentação e checkups)
- **Objetivo:** Aprender a criar e configurar um projeto Spring Boot

## 8. Criar Entidade Unidade
- [x] Criar estrutura de pastas (model, repository, service, controller)
- [x] Criar `model/Unidade.java`
- [x] Criar `repository/UnidadeRepository.java`
- [x] Criar `service/UnidadeService.java`
- [x] Criar `controller/UnidadeController.java`

## 9. Testar CRUD da Unidade
- [ ] Reiniciar aplicação
- [ ] Verificar tabela `unidades` no H2 Console
- [ ] Testar POST (criar)
- [ ] Testar GET (listar/buscar)
- [ ] Testar PUT (atualizar)
- [ ] Testar DELETE (deletar)

## 10. Criar Entidades Restantes
- [x] Criar `model/Laboratorio.java`
- [x] Criar `repository/LaboratorioRepository.java`
- [x] Criar `service/LaboratorioService.java`
- [x] Criar `controller/LaboratorioController.java`
- [x] Criar `model/Estudante.java`
- [x] Criar `repository/EstudanteRepository.java`
- [x] Criar `service/EstudanteService.java`
- [x] Criar `controller/EstudanteController.java`
- [x] Criar `model/Produto.java`
- [x] Criar `repository/ProdutoRepository.java`
- [x] Criar `service/ProdutoService.java`
- [x] Criar `controller/ProdutoController.java`
- [x] Criar `model/Pedido.java`
- [x] Criar `repository/PedidoRepository.java`
- [x] Criar `service/PedidoService.java`
- [x] Criar `controller/PedidoController.java`
- [x] Criar `model/ItemPedido.java`
- [x] Criar `repository/ItemPedidoRepository.java`
- [x] Criar `service/ItemPedidoService.java`
- [x] Criar `controller/ItemPedidoController.java`
- [x] Criar `model/Projeto.java`
- [x] Criar `repository/ProjetoRepository.java`
- [x] Criar `service/ProjetoService.java`
- [x] Criar `controller/ProjetoController.java`

## 11. Verificar Estrutura Final
```
backend/sgl-backend/src/main/java/com/sgl/
├── SglApplication.java
├── config/
│   └── SecurityConfig.java
├── model/
│   ├── Unidade.java
│   ├── Laboratorio.java
│   ├── Estudante.java
│   ├── Produto.java
│   ├── Pedido.java
│   ├── ItemPedido.java
│   └── Projeto.java
├── repository/
│   ├── UnidadeRepository.java
│   ├── LaboratorioRepository.java
│   ├── EstudanteRepository.java
│   ├── ProdutoRepository.java
│   ├── PedidoRepository.java
│   ├── ItemPedidoRepository.java
│   └── ProjetoRepository.java
├── service/
│   ├── UnidadeService.java
│   ├── LaboratorioService.java
│   ├── EstudanteService.java
│   ├── ProdutoService.java
│   ├── PedidoService.java
│   ├── ItemPedidoService.java
│   └── ProjetoService.java
└── controller/
    ├── UnidadeController.java
    ├── LaboratorioController.java
    ├── EstudanteController.java
    ├── ProdutoController.java
    ├── PedidoController.java
    ├── ItemPedidoController.java
    └── ProjetoController.java
```

---

**DÚVIDAS?** Consulte o guia completo em `CONTINUIDADE.md` ou pergunte ao Mimo!