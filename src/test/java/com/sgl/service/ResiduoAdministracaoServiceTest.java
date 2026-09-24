package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.request.AdministrarResiduoRequestDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.HistoricoResiduo;
import com.sgl.model.Laboratorio;
import com.sgl.model.Residuo;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.AcaoAdministrativaResiduo;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.StatusResiduo;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.repository.ClasseResiduoRepository;
import com.sgl.repository.HistoricoResiduoRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.LocalArmazenamentoResiduoRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.ResiduoRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
class ResiduoAdministracaoServiceTest {

    private static final UUID UNIDADE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final UUID RESIDUO_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000402");
    private static final UUID ADMIN_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000403");
    private static final UUID GESTOR_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000404");
    private static final UUID GERADOR_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000405");
    private static final UUID LAB_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000406");

    @Mock
    private ResiduoRepository residuoRepository;
    @Mock
    private HistoricoResiduoRepository historicoResiduoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private LaboratorioRepository laboratorioRepository;
    @Mock
    private ProjetoRepository projetoRepository;
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private ClasseResiduoRepository classeResiduoRepository;
    @Mock
    private LocalArmazenamentoResiduoRepository localArmazenamentoResiduoRepository;

    @InjectMocks
    private ResiduoService service;

    private Unidade unidade;
    private Laboratorio laboratorio;
    private Usuario gerador;
    private Usuario administrador;
    private Usuario gestor;

    @BeforeEach
    void preparar() {
        TenantContext.definir(UNIDADE_ID);

        unidade = new Unidade();
        unidade.setId(1L);
        unidade.setPublicId(UNIDADE_ID);
        unidade.setNome("CNPMF");
        unidade.setSigla("CNPMF");

        laboratorio = Laboratorio.builder()
                .id(10L)
                .publicId(LAB_ID)
                .unidade(unidade)
                .nome("Laboratório")
                .ativo(true)
                .build();

        gerador = usuario(GERADOR_ID, Perfil.PESQUISADOR, "Gerador");
        administrador = usuario(ADMIN_ID, Perfil.ADMINISTRADOR, "Administrador");
        gestor = usuario(GESTOR_ID, Perfil.GESTOR, "Gestor");
    }

    @AfterEach
    void limparTenant() {
        TenantContext.limpar();
    }

    @Test
    void deveCancelarResiduoComAdministradorDaMesmaUnidadeERegistrarHistorico() {
        Residuo residuo = residuo(StatusResiduo.EM_ANALISE);

        when(residuoRepository.findByPublicIdAndLaboratorioUnidadePublicId(
                RESIDUO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(residuo));

        when(usuarioRepository.findByPublicIdAndUnidadePublicId(
                ADMIN_ID, UNIDADE_ID))
                .thenReturn(Optional.of(administrador));

        when(residuoRepository.save(residuo))
                .thenReturn(residuo);

        AdministrarResiduoRequestDTO dto =
                new AdministrarResiduoRequestDTO(
                        ADMIN_ID,
                        AcaoAdministrativaResiduo.CANCELAR,
                        "Cadastro realizado por engano"
                );

        var resposta = service.administrarCiclo(RESIDUO_ID, dto);

        assertEquals(StatusResiduo.CANCELADO, resposta.getStatus());

        ArgumentCaptor<HistoricoResiduo> captor =
                ArgumentCaptor.forClass(HistoricoResiduo.class);

        verify(historicoResiduoRepository).save(captor.capture());

        assertEquals(
                "RESIDUO_CANCELADO_ADMINISTRATIVAMENTE",
                captor.getValue().getAcao()
        );
        assertEquals(
                StatusResiduo.CANCELADO,
                captor.getValue().getStatus()
        );
    }

    @Test
    void deveRetornarUmaEtapaERegistrarHistorico() {
        Residuo residuo = residuo(
                StatusResiduo.LIBERADO_PARA_ARMAZENAMENTO
        );
        residuo.setDataLiberacao(LocalDateTime.now());

        when(residuoRepository.findByPublicIdAndLaboratorioUnidadePublicId(
                RESIDUO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(residuo));

        when(usuarioRepository.findByPublicIdAndUnidadePublicId(
                ADMIN_ID, UNIDADE_ID))
                .thenReturn(Optional.of(administrador));

        when(residuoRepository.save(residuo))
                .thenReturn(residuo);

        AdministrarResiduoRequestDTO dto =
                new AdministrarResiduoRequestDTO(
                        ADMIN_ID,
                        AcaoAdministrativaResiduo.RETORNAR_ETAPA,
                        "Necessária nova conferência"
                );

        var resposta = service.administrarCiclo(RESIDUO_ID, dto);

        assertEquals(StatusResiduo.EM_ANALISE, resposta.getStatus());

        ArgumentCaptor<HistoricoResiduo> captor =
                ArgumentCaptor.forClass(HistoricoResiduo.class);

        verify(historicoResiduoRepository).save(captor.capture());

        assertEquals(
                "RETORNO_ADMINISTRATIVO_DE_ETAPA",
                captor.getValue().getAcao()
        );
        assertEquals(
                "Retorno administrativo: Liberado para armazenamento → Em análise. Justificativa: Necessária nova conferência",
                captor.getValue().getObservacao()
        );
    }

    @Test
    void deveBuscarHistoricoDoGeradorDaUnidade() {
        Residuo residuo = residuo(StatusResiduo.CANCELADO);

        HistoricoResiduo evento = HistoricoResiduo.builder()
                .id(200L)
                .publicId(UUID.fromString("00000000-0000-0000-0000-000000000420"))
                .residuo(residuo)
                .usuario(administrador)
                .status(StatusResiduo.CANCELADO)
                .acao("RESIDUO_CANCELADO_ADMINISTRATIVAMENTE")
                .observacao("Justificativa: cadastro indevido")
                .dataHora(LocalDateTime.now())
                .build();

        when(usuarioRepository.findByPublicIdAndUnidadePublicId(
                GERADOR_ID, UNIDADE_ID))
                .thenReturn(Optional.of(gerador));

        when(historicoResiduoRepository
                .findByResiduoGeradorPublicIdAndResiduoLaboratorioUnidadePublicIdOrderByDataHoraDesc(
                        GERADOR_ID,
                        UNIDADE_ID))
                .thenReturn(List.of(evento));

        var resposta = service.buscarHistoricoPorGerador(GERADOR_ID);

        assertEquals(1, resposta.size());
        assertEquals(RESIDUO_ID, resposta.get(0).getResiduoId());
        assertEquals(
                "RESIDUO_CANCELADO_ADMINISTRATIVAMENTE",
                resposta.get(0).getAcao()
        );
    }

    @Test
    void deveRejeitarGestorSemPerfilAdministrador() {
        Residuo residuo = residuo(StatusResiduo.EM_ANALISE);

        when(residuoRepository.findByPublicIdAndLaboratorioUnidadePublicId(
                RESIDUO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(residuo));

        when(usuarioRepository.findByPublicIdAndUnidadePublicId(
                GESTOR_ID, UNIDADE_ID))
                .thenReturn(Optional.of(gestor));

        AdministrarResiduoRequestDTO dto =
                new AdministrarResiduoRequestDTO(
                        GESTOR_ID,
                        AcaoAdministrativaResiduo.CANCELAR,
                        "Teste"
                );

        assertThrows(
                BusinessRuleException.class,
                () -> service.administrarCiclo(RESIDUO_ID, dto)
        );

        verify(residuoRepository, never())
                .save(any(Residuo.class));
    }

    @Test
    void deveExigirTenantAtivo() {
        TenantContext.limpar();

        AdministrarResiduoRequestDTO dto =
                new AdministrarResiduoRequestDTO(
                        ADMIN_ID,
                        AcaoAdministrativaResiduo.CANCELAR,
                        "Teste"
                );

        assertThrows(
                BusinessRuleException.class,
                () -> service.administrarCiclo(RESIDUO_ID, dto)
        );

        verify(residuoRepository, never())
                .save(any(Residuo.class));
    }

    @Test
    void residuoDeOutraUnidadeDeveParecerInexistente() {
        when(residuoRepository.findByPublicIdAndLaboratorioUnidadePublicId(
                RESIDUO_ID, UNIDADE_ID))
                .thenReturn(Optional.empty());

        AdministrarResiduoRequestDTO dto =
                new AdministrarResiduoRequestDTO(
                        ADMIN_ID,
                        AcaoAdministrativaResiduo.CANCELAR,
                        "Teste"
                );

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.administrarCiclo(RESIDUO_ID, dto)
        );

        verify(usuarioRepository, never())
                .findByPublicIdAndUnidadePublicId(any(), any());
    }

    private Usuario usuario(UUID id, Perfil perfil, String nome) {
        Usuario usuario = new Usuario();
        usuario.setId((long) nome.hashCode());
        usuario.setPublicId(id);
        usuario.setNome(nome);
        usuario.setEmail(nome.toLowerCase() + "@teste.local");
        usuario.setSenha("senha");
        usuario.setPerfil(perfil);
        usuario.setUnidade(unidade);
        usuario.setAtivo(true);
        return usuario;
    }

    private Residuo residuo(StatusResiduo status) {
        return Residuo.builder()
                .id(100L)
                .publicId(RESIDUO_ID)
                .laboratorio(laboratorio)
                .gerador(gerador)
                .descricao("Resíduo teste")
                .processoOrigem("Teste")
                .recipiente("Frasco")
                .quantidade(BigDecimal.ONE)
                .unidadeMedida(UnidadeMedida.ML)
                .nivelRiscoInformado(NivelRisco.BAIXO)
                .status(status)
                .dataInformacao(LocalDateTime.now())
                .build();
    }
}
