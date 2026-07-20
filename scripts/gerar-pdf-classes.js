const PDFDocument = require('pdfkit');
const fs = require('fs');
const path = require('path');

const doc = new PDFDocument({
    size: 'A4',
    margins: { top: 50, bottom: 50, left: 50, right: 50 },
    info: {
        Title: 'SGL - Relação Completa de Classes',
        Author: 'SGL Project',
        Subject: 'Documentação Técnica - Classes, Enums e Componentes',
        Keywords: 'SGL, Java, Spring Boot, Classes'
    }
});

const outputPath = path.join(__dirname, '..', 'docs', 'SGL_Relacao_Completa_Classes.pdf');
const stream = fs.createWriteStream(outputPath);
doc.pipe(stream);

// Colors
const primaryColor = '#1a365d';
const secondaryColor = '#2d3748';
const accentColor = '#3182ce';
const lightBg = '#f7fafc';
const greenStatus = '#38a169';
const yellowStatus = '#d69e2e';
const redStatus = '#e53e3e';

// Helper functions
function drawHeader() {
    doc.rect(0, 0, doc.page.width, 80).fill(primaryColor);
    doc.fill('white')
       .font('Helvetica-Bold')
       .fontSize(18)
       .text('SGL - Sistema de Gestão de Laboratórios', 50, 25, { align: 'center' });
    doc.font('Helvetica')
       .fontSize(11)
       .text('Relação Completa de Classes, Enums e Componentes', 50, 50, { align: 'center' });
}

function drawFooter() {
    doc.rect(0, doc.page.height - 40, doc.page.width, 40).fill(primaryColor);
    doc.fill('white')
       .font('Helvetica')
       .fontSize(8)
       .text('SGL - Documentação Técnica | Gerado em: ' + new Date().toLocaleDateString('pt-BR'), 50, doc.page.height - 25, { align: 'center' });
}

function drawSectionTitle(title, y) {
    doc.rect(50, y, doc.page.width - 100, 25).fill(accentColor);
    doc.fill('white')
       .font('Helvetica-Bold')
       .fontSize(12)
       .text(title, 60, y + 6);
    return y + 35;
}

function drawSubsectionTitle(title, y) {
    doc.fill(primaryColor)
       .font('Helvetica-Bold')
       .fontSize(11)
       .text(title, 50, y);
    doc.moveTo(50, y + 14)
       .lineTo(doc.page.width - 50, y + 14)
       .strokeColor(accentColor)
       .lineWidth(0.5)
       .stroke();
    return y + 22;
}

function drawClassBox(className, status, y) {
    const statusColors = {
        '✅ CRIADO': greenStatus,
        '⚠️ PARCIAL': yellowStatus,
        '⬜ PENDENTE': redStatus
    };
    
    doc.roundedRect(50, y, doc.page.width - 100, 20, 3)
        .fill(statusColors[status] || lightBg);
    
    doc.fill(status === '⬜ PENDENTE' ? 'white' : 'white')
       .font('Helvetica-Bold')
       .fontSize(10)
       .text(className, 60, y + 5);
    
    doc.font('Helvetica')
       .fontSize(9)
       .text(status, doc.page.width - 150, y + 5, { align: 'right', width: 90 });
    
    return y + 25;
}

function drawField(fieldName, fieldType, y) {
    doc.fill(secondaryColor)
       .font('Helvetica')
       .fontSize(9)
       .text(`• ${fieldName}: ${fieldType}`, 70, y, { width: doc.page.width - 140 });
    return y + 14;
}

function drawCodeBlock(lines, y) {
    doc.rect(60, y, doc.page.width - 120, lines.length * 12 + 10)
       .fill('#edf2f7');
    
    doc.font('Courier')
       .fontSize(8)
       .fill(secondaryColor);
    
    lines.forEach((line, i) => {
        doc.text(line, 70, y + 5 + (i * 12), { width: doc.page.width - 140 });
    });
    
    return y + lines.length * 12 + 20;
}

// Start building the PDF
drawHeader();

let y = 100;

// Title page content
doc.fill(primaryColor)
   .font('Helvetica-Bold')
   .fontSize(24)
   .text('Relação Completa de Classes', 50, y, { align: 'center' });
y += 35;

doc.fill(secondaryColor)
   .font('Helvetica')
   .fontSize(12)
   .text('Projeto SGL - Sistema de Gestão de Laboratórios', 50, y, { align: 'center' });
y += 25;

doc.fill(secondaryColor)
   .font('Helvetica')
   .fontSize(10)
   .text('Backend: Java Spring Boot | Frontend: Vue.js | Database: PostgreSQL', 50, y, { align: 'center' });
y += 20;

doc.fill(secondaryColor)
   .font('Helvetica')
   .fontSize(10)
   .text('Data: ' + new Date().toLocaleDateString('pt-BR'), 50, y, { align: 'center' });
y += 40;

// Summary table
doc.fill(primaryColor)
   .font('Helvetica-Bold')
   .fontSize(14)
   .text('Resumo Geral', 50, y, { align: 'center' });
y += 25;

const summaryData = [
    ['Status', 'Quantidade', 'Descrição'],
    ['✅ Criado/Funcional', '10', 'Unidade, Laboratório, Usuario, Perfil, DTOs, Repositories, Config'],
    ['⚠️ Parcial', '2', 'UsuarioDTO (correções), DataInitializer (adicionar users)'],
    ['⬜ Pendente', '33', 'Produto, Pedido, ItemPedido, Projeto, Documento, Movimentacao, Lote + Enums + DTOs + Services + Controllers'],
    ['Total', '45', 'Todos os componentes planejados']
];

const tableWidth = doc.page.width - 100;
const colWidths = [tableWidth * 0.3, tableWidth * 0.2, tableWidth * 0.5];

summaryData.forEach((row, i) => {
    const bgColor = i === 0 ? primaryColor : (i % 2 === 0 ? '#edf2f7' : 'white');
    doc.rect(50, y, tableWidth, 20).fill(bgColor);
    
    doc.fill(i === 0 ? 'white' : secondaryColor)
       .font(i === 0 ? 'Helvetica-Bold' : 'Helvetica')
       .fontSize(9);
    
    let xPos = 55;
    row.forEach((cell, j) => {
        doc.text(cell, xPos, y + 5, { width: colWidths[j] - 5 });
        xPos += colWidths[j];
    });
    y += 20;
});

doc.addPage();
y = 50;

// ==================== SECTION 1: ENTITIES ====================
y = drawSectionTitle('1. ENTIDADES (Entities)', y);

// 1.1 Unidade
y = drawClassBox('1.1. Unidade (Tenant)', '✅ CRIADO', y);
doc.fill(secondaryColor).font('Courier').fontSize(8);
doc.text('Tabela: unidades', 60, y);
y += 14;
doc.text('Campos: id (Long, PK) | nome (String) | sigla (String, único) | laboratorios (List<Laboratorio>)', 60, y, { width: doc.page.width - 120 });
y += 25;

// 1.2 Laboratório
y = drawClassBox('1.2. Laboratório', '✅ CRIADO', y);
doc.text('Tabela: laboratorios', 60, y);
y += 14;
doc.text('Campos: id (Long, PK) | unidade (Unidade, FK) | nome (String) | descricao (String) | responsavel (String) | ativo (Boolean)', 60, y, { width: doc.page.width - 120 });
y += 25;

// 1.3 Usuario
y = drawClassBox('1.3. Usuario', '✅ CRIADO', y);
doc.text('Tabela: usuario', 60, y);
y += 14;
doc.text('Campos: id (Long, PK) | nome (String) | email (String, único) | senha (String, BCrypt) | perfil (Perfil, enum) | laboratorio (Laboratorio, FK) | ativo (Boolean)', 60, y, { width: doc.page.width - 120 });
y += 14;
doc.fill('#d69e2e').text('⚠️ OBS: Entidade não tem unidadeId direto (unidade via laboratorio.unidade)', 70, y, { width: doc.page.width - 140 });
y += 20;

// 1.4 Produto
y = drawClassBox('1.4. Produto', '⬜ PENDENTE', y);
doc.fill(secondaryColor).text('Tabela: produtos', 60, y);
y += 14;
doc.text('Campos: id | laboratorio (FK) | nome | descricao | codigo_referencia | quantidade_atual | quantidade_minima | unidade_medida | localizacao_fisica | ativo', 60, y, { width: doc.page.width - 120 });
y += 14;
doc.text('Risco: risco (Risco enum) | tipo_risco (TipoRisco enum) | descricao_risco | perecivel (Boolean) | dias_validade | tipo_perecivel (TipoPerecivel enum) | condicoes_armazenamento', 60, y, { width: doc.page.width - 120 });
y += 25;

// 1.5 Pedido
y = drawClassBox('1.5. Pedido', '⬜ PENDENTE', y);
doc.text('Tabela: pedidos', 60, y);
y += 14;
doc.text('Campos: id | usuario (Usuario, FK) | laboratorio (Laboratorio, FK) | projeto (Projeto, FK, opcional) | data_solicitacao | status (StatusPedido enum) | observacao | arquivo_documento', 60, y, { width: doc.page.width - 120 });
y += 25;

// 1.6 ItemPedido
y = drawClassBox('1.6. ItemPedido', '⬜ PENDENTE', y);
doc.text('Tabela: itens_pedido', 60, y);
y += 14;
doc.text('Campos: id | pedido (Pedido, FK) | produto (Produto, FK) | quantidade_solicitada | quantidade_aprovada', 60, y, { width: doc.page.width - 120 });
y += 25;

// 1.7 Projeto
y = drawClassBox('1.7. Projeto', '⬜ PENDENTE', y);
doc.text('Tabela: projetos', 60, y);
y += 14;
doc.text('Campos: id | laboratorio (Laboratorio, FK) | nome | descricao | data_inicio | data_fim (opcional) | responsavel | ativo', 60, y, { width: doc.page.width - 120 });
y += 25;

// 1.8-1.10 Future entities
y = drawClassBox('1.8. Documento', '⬜ PENDENTE', y);
doc.fill('#718096').font('Helvetica-Oblique').fontSize(9)
   .text('Entidade futura - detalhes a serem definidos', 70, y, { width: doc.page.width - 140 });
y += 20;

y = drawClassBox('1.9. Movimentacao', '⬜ PENDENTE', y);
doc.fill('#718096').font('Helvetica-Oblique').fontSize(9)
   .text('Entidade futura - detalhes a serem definidos', 70, y, { width: doc.page.width - 140 });
y += 20;

y = drawClassBox('1.10. Lote', '⬜ PENDENTE', y);
doc.fill('#718096').font('Helvetica-Oblique').fontSize(9)
   .text('Entidade futura - detalhes a serem definidos', 70, y, { width: doc.page.width - 140 });

doc.addPage();
y = 50;

// ==================== SECTION 2: ENUMS ====================
y = drawSectionTitle('2. ENUMS', y);

// 2.1 Perfil
y = drawClassBox('2.1. Perfil', '✅ CRIADO', y);
doc.fill(secondaryColor).font('Courier').fontSize(8);
doc.text('ADMINISTRADOR | GESTOR | TECNICO | PESQUISADOR | ESTAGIARIO', 60, y);
y += 25;

// 2.2 StatusPedido
y = drawClassBox('2.2. StatusPedido', '⬜ PENDENTE', y);
doc.text('PENDENTE | APROVADO | REJEITADO | ENTREGUE | CANCELADO', 60, y);
y += 25;

// 2.3 Risco
y = drawClassBox('2.3. Risco', '⬜ PENDENTE', y);
doc.text('NENHUM | BAIXO | MEDIO | ALTO', 60, y);
y += 25;

// 2.4 TipoRisco
y = drawClassBox('2.4. TipoRisco', '⬜ PENDENTE', y);
doc.text('NENHUM | INFLAMAVEL | RADIOATIVO | TOXICO | CORROSIVO | BIOLOGICO', 60, y);
y += 25;

// 2.5 TipoPerecivel
y = drawClassBox('2.5. TipoPerecivel', '⬜ PENDENTE', y);
doc.text('NENHUM | VEGETAL | ANIMAL | MICROBIANO | QUIMICO', 60, y);
y += 25;

// 2.6 TipoMovimentacao
y = drawClassBox('2.6. TipoMovimentacao', '⬜ PENDENTE', y);
doc.text('ENTRADA | SAIDA | TRANSFERENCIA | AJUSTE', 60, y);
y += 35;

// ==================== SECTION 3: DTOs ====================
y = drawSectionTitle('3. DTOs (Data Transfer Objects)', y);

// 3.1 UnidadeDTO
y = drawClassBox('3.1. UnidadeDTO', '✅ CRIADO', y);
doc.fill(secondaryColor).font('Courier').fontSize(8);
doc.text('Campos: id | nome | sigla', 60, y);
y += 14;
doc.text('Construtor Entity→DTO: completo', 60, y);
y += 25;

// 3.2 LaboratorioDTO
y = drawClassBox('3.2. LaboratorioDTO', '✅ CRIADO', y);
doc.text('Campos: id | unidadeId | nome | descricao | responsavel | ativo', 60, y);
y += 14;
doc.text('Validações: @NotNull unidadeId, @NotBlank nome', 60, y);
y += 25;

// 3.3 UsuarioDTO
y = drawClassBox('3.3. UsuarioDTO', '⚠️ PARCIAL', y);
doc.fill(secondaryColor).font('Courier').fontSize(8);
doc.text('Campos: id | nome | email | senha | perfil | unidadeId | laboratorioId | ativo', 60, y);
y += 14;
doc.fill('#d69e2e').text('CORREÇÕES: Remover unidadeId | Remover senha do construtor | Adicionar perfil/laboratorioId', 70, y, { width: doc.page.width - 140 });
y += 25;

// 3.4-3.7 Pending DTOs
['3.4. ProdutoDTO', '3.5. PedidoDTO', '3.6. ItemPedidoDTO', '3.7. ProjetoDTO'].forEach(dto => {
    y = drawClassBox(dto, '⬜ PENDENTE', y);
    y += 5;
});

y += 15;

// ==================== SECTION 4: REPOSITORIES ====================
y = drawSectionTitle('4. REPOSITORIES (Acesso a Dados)', y);

const repositories = [
    ['4.1. UnidadeRepository', '✅ CRIADO', 'findAll, findBySigla'],
    ['4.2. LaboratorioRepository', '✅ CRIADO', 'findByUnidadeId'],
    ['4.3. UsuarioRepository', '⬜ PENDENTE', 'findByEmail, findByLaboratorioId'],
    ['4.4. ProdutoRepository', '⬜ PENDENTE', 'findByLaboratorioId, findByRisco, findPereciveis, findEstoqueBaixo'],
    ['4.5. PedidoRepository', '⬜ PENDENTE', 'findByUsuarioId, findByLaboratorioId, findByStatus'],
    ['4.6. ItemPedidoRepository', '⬜ PENDENTE', 'findByPedidoId'],
    ['4.7. ProjetoRepository', '⬜ PENDENTE', 'findByLaboratorioId']
];

repositories.forEach(([name, status, methods]) => {
    y = drawClassBox(name, status, y);
    doc.fill(secondaryColor).font('Helvetica').fontSize(8);
    doc.text(`Métodos: ${methods}`, 70, y, { width: doc.page.width - 140 });
    y += 18;
});

y += 10;

// ==================== SECTION 5: SERVICES ====================
y = drawSectionTitle('5. SERVICES (Lógica de Negócio)', y);

const services = [
    ['5.1. UnidadeService', '✅ CRIADO', 'listarTodos, buscarPorId, salvar, atualizar, deletar'],
    ['5.2. LaboratorioService', '✅ CRIADO', 'listarTodos, listarPorUnidade, buscarPorId, salvar, atualizar, deletar'],
    ['5.3. UsuarioService', '⬜ PENDENTE', 'listarTodos, buscarPorId, salvar, atualizar, deletar'],
    ['5.4. ProdutoService', '⬜ PENDENTE', 'listarTodos, buscarPorId, salvar, atualizar, deletar, listarEstoqueBaixo, listarPereciveis'],
    ['5.5. PedidoService', '⬜ PENDENTE', 'listarTodos, buscarPorId, criar, aprovar, rejeitar, entregar'],
    ['5.6. ProjetoService', '⬜ PENDENTE', 'listarTodos, buscarPorId, salvar, atualizar, deletar']
];

services.forEach(([name, status, methods]) => {
    y = drawClassBox(name, status, y);
    doc.fill(secondaryColor).font('Helvetica').fontSize(8);
    doc.text(`Métodos: ${methods}`, 70, y, { width: doc.page.width - 140 });
    y += 18;
});

doc.addPage();
y = 50;

// ==================== SECTION 6: CONTROLLERS ====================
y = drawSectionTitle('6. CONTROLLERS (Endpoints REST)', y);

const controllers = [
    ['6.1. UnidadeController', '✅ CRIADO', [
        'GET    /api/v1/unidades',
        'POST   /api/v1/unidades',
        'GET    /api/v1/unidades/{id}',
        'PUT    /api/v1/unidades/{id}',
        'DELETE /api/v1/unidades/{id}'
    ]],
    ['6.2. LaboratorioController', '✅ CRIADO', [
        'GET    /api/v1/laboratorios',
        'POST   /api/v1/laboratorios',
        'GET    /api/v1/laboratorios/{id}',
        'PUT    /api/v1/laboratorios/{id}',
        'GET    /api/v1/laboratorios/por-unidade?unidadeId=X'
    ]],
    ['6.3. UsuarioController', '⬜ PENDENTE', [
        'GET    /api/v1/usuarios',
        'POST   /api/v1/usuarios',
        'GET    /api/v1/usuarios/{id}',
        'PUT    /api/v1/usuarios/{id}',
        'DELETE /api/v1/usuarios/{id}',
        'GET    /api/v1/laboratorios/{id}/usuarios'
    ]],
    ['6.4. ProdutoController', '⬜ PENDENTE', [
        'GET    /api/v1/produtos',
        'POST   /api/v1/produtos',
        'GET    /api/v1/produtos/{id}',
        'PUT    /api/v1/produtos/{id}',
        'DELETE /api/v1/produtos/{id}',
        'GET    /api/v1/laboratorios/{id}/produtos',
        'GET    /api/v1/produtos/estoque-baixo',
        'GET    /api/v1/produtos/risco/{nivel}',
        'GET    /api/v1/produtos/pereciveis',
        'GET    /api/v1/produtos/validade-proxima'
    ]],
    ['6.5. PedidoController', '⬜ PENDENTE', [
        'GET    /api/v1/pedidos',
        'POST   /api/v1/pedidos',
        'GET    /api/v1/pedidos/{id}',
        'PUT    /api/v1/pedidos/{id}/aprovar',
        'PUT    /api/v1/pedidos/{id}/rejeitar',
        'PUT    /api/v1/pedidos/{id}/entregar'
    ]],
    ['6.6. ProjetoController', '⬜ PENDENTE', [
        'GET    /api/v1/projetos',
        'POST   /api/v1/projetos',
        'GET    /api/v1/projetos/{id}',
        'PUT    /api/v1/projetos/{id}',
        'DELETE /api/v1/projetos/{id}',
        'GET    /api/v1/laboratorios/{id}/projetos'
    ]]
];

controllers.forEach(([name, status, endpoints]) => {
    y = drawClassBox(name, status, y);
    doc.fill(secondaryColor).font('Courier').fontSize(7);
    endpoints.forEach(ep => {
        doc.text(ep, 70, y, { width: doc.page.width - 140 });
        y += 10;
    });
    y += 10;
});

y += 10;

// ==================== SECTION 7: CONFIG & INFRASTRUCTURE ====================
y = drawSectionTitle('7. CONFIGURAÇÃO E INFRAESTRUTURA', y);

y = drawClassBox('7.1. SecurityConfig', '✅ CRIADO', y);
doc.fill(secondaryColor).font('Courier').fontSize(8);
doc.text('Desabilita CSRF, frames, form login, http basic | Permite /h2-console/**', 60, y);
y += 25;

y = drawClassBox('7.2. CorsConfig', '⬜ PENDENTE', y);
doc.fill(secondaryColor).font('Courier').fontSize(8);
doc.text('Configuração de CORS para frontend Vue.js', 60, y);
y += 30;

// ==================== SECTION 8: EXCEPTIONS ====================
y = drawSectionTitle('8. EXCEÇÕES', y);

y = drawClassBox('8.1. RestExceptionHandler', '✅ CRIADO', y);
doc.fill(secondaryColor).font('Courier').fontSize(8);
doc.text('Mapeamento: EntityNotFoundException → 404', 60, y);
y += 14;
doc.fill('#d69e2e').text('Pendência: Tratar DataIntegrityViolationException → 409', 70, y, { width: doc.page.width - 140 });
y += 25;

y = drawClassBox('8.2. RecursoNaoEncontradoException', '⬜ PENDENTE', y);
doc.fill(secondaryColor).font('Courier').fontSize(8);
doc.text('Exception customizada para quando entidade não é encontrada', 60, y);
y += 30;

// ==================== SECTION 9: UTILITIES & TESTS ====================
y = drawSectionTitle('9. UTILITÁRIOS E TESTES', y);

y = drawClassBox('9.1. DataInitializer', '✅ CRIADO', y);
doc.fill(secondaryColor).font('Courier').fontSize(8);
doc.text('CommandLineRunner que injeta dados de teste (3 unidades, 5 laboratórios)', 60, y);
y += 14;
doc.fill('#d69e2e').text('Pendência: Adicionar usuários de teste', 70, y, { width: doc.page.width - 140 });

doc.addPage();
y = 50;

// ==================== SECTION 10: PROJECT DIAGRAM ====================
y = drawSectionTitle('10. DIAGRAMA DO PROJETO', y);

y += 10;

// Draw the UML diagram as text
doc.fill(primaryColor)
   .font('Helvetica-Bold')
   .fontSize(11)
   .text('Diagrama de Classes - Visão Geral', 50, y);
y += 20;

// Diagram boxes
const diagramData = [
    { name: 'UNIDADE', x: 300, y: y, color: '#3182ce' },
    { name: 'LABORATORIO', x: 300, y: y + 60, color: '#38a169' },
    { name: 'USUARIO', x: 100, y: y + 120, color: '#d69e2e' },
    { name: 'PRODUTO', x: 400, y: y + 120, color: '#e53e3e' },
    { name: 'PEDIDO', x: 250, y: y + 180, color: '#805ad5' },
    { name: 'ITEM_PEDIDO', x: 400, y: y + 180, color: '#dd6b20' },
    { name: 'PROJETO', x: 100, y: y + 180, color: '#319795' }
];

diagramData.forEach(box => {
    doc.roundedRect(box.x, box.y, 120, 30, 5)
        .fill(box.color);
    doc.fill('white')
       .font('Helvetica-Bold')
       .fontSize(8)
       .text(box.name, box.x + 10, box.y + 10, { width: 100, align: 'center' });
});

// Draw connection lines
doc.strokeColor('#4a5568')
   .lineWidth(1);

// Unidade → Laboratorio
doc.moveTo(360, y + 30).lineTo(360, y + 60).stroke();

// Laboratorio → Usuario
doc.moveTo(300, y + 90).lineTo(160, y + 120).stroke();

// Laboratorio → Produto
doc.moveTo(420, y + 90).lineTo(460, y + 120).stroke();

// Usuario → Pedido
doc.moveTo(160, y + 150).lineTo(310, y + 180).stroke();

// Produto → ItemPedido
doc.moveTo(460, y + 150).lineTo(460, y + 180).stroke();

// Pedido → ItemPedido
doc.moveTo(370, y + 210).lineTo(400, y + 210).stroke();

// Projeto → Pedido
doc.moveTo(160, y + 210).lineTo(250, y + 210).stroke();

y += 240;

// Legend
doc.fill(primaryColor)
   .font('Helvetica-Bold')
   .fontSize(10)
   .text('Legenda:', 50, y);
y += 15;

const legend = [
    ['■ Unidade', '#3182ce', 'Tenant/Instituição'],
    ['■ Laboratorio', '#38a169', 'Laboratório de pesquisa'],
    ['■ Usuario', '#d69e2e', 'Usuário do sistema'],
    ['■ Produto', '#e53e3e', 'Material em estoque'],
    ['■ Pedido', '#805ad5', 'Solicitação de material'],
    ['■ ItemPedido', '#dd6b20', 'Item dentro do pedido'],
    ['■ Projeto', '#319795', 'Projeto de pesquisa']
];

legend.forEach(([name, color, desc]) => {
    doc.fill(color)
       .font('Helvetica')
       .fontSize(9)
       .text(name, 60, y, { width: 100 });
    doc.fill('#4a5568')
       .text(`- ${desc}`, 170, y, { width: 200 });
    y += 14;
});

y += 20;

// Relationships
doc.fill(primaryColor)
   .font('Helvetica-Bold')
   .fontSize(10)
   .text('Relacionamentos Principais:', 50, y);
y += 15;

const relationships = [
    'Unidade "1" → "*" Laboratorio',
    'Unidade "1" → "*" Usuario',
    'Unidade "1" → "*" Produto',
    'Laboratorio "1" → "*" Usuario',
    'Laboratorio "1" → "*" Pedido',
    'Usuario "1" → "*" Pedido',
    'Produto "1" → "*" ItemPedido',
    'Pedido "1" → "*" ItemPedido',
    'Projeto "1" → "*" Pedido'
];

relationships.forEach(rel => {
    doc.fill(secondaryColor)
       .font('Helvetica')
       .fontSize(9)
       .text(`• ${rel}`, 60, y, { width: doc.page.width - 120 });
    y += 14;
});

// Finish the PDF
doc.end();

stream.on('finish', () => {
    console.log(`✅ PDF gerado com sucesso: ${outputPath}`);
});

stream.on('error', (err) => {
    console.error('❌ Erro ao gerar PDF:', err);
});
