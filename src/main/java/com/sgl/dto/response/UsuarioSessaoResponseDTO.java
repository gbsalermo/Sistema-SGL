package com.sgl.dto.response;

import java.util.List;
import java.util.UUID;

import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** "Who am I". Deliberately without CPF and matrícula: personal identifiers never reach the browser. */
@Schema(description = "Dados do usuário da sessão atual.")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioSessaoResponseDTO {

    @Schema(description = "Identificador público UUID do usuário.")
    private UUID id;
    @Schema(description = "Nome do usuário.")
    private String nome;
    @Schema(description = "E-mail do usuário, quando houver.")
    private String email;
    @Schema(description = "Perfil do usuário. Para PENDENTE_VINCULACAO é apenas provisório.")
    private Perfil perfil;
    @Schema(description = "Situação do cadastro.")
    private SituacaoUsuario situacao;
    @Schema(description = "UUID da Unidade do usuário.")
    private UUID unidadeId;
    @Schema(description = "Nome da Unidade do usuário.")
    private String unidadeNome;
    @Schema(description = "Sigla da Unidade do usuário.")
    private String unidadeSigla;
    @Schema(description = "Laboratórios em que a pessoa atua, ordenados por nome.")
    private List<LaboratorioResumoDTO> laboratorios;
    @Schema(description = "Indica se o usuário está ativo.")
    private Boolean ativo;

    /** Expects unidade and laboratorios already loaded (they are lazy). */
    public UsuarioSessaoResponseDTO(Usuario usuario) {
        this.id = usuario.getPublicId();
        this.nome = usuario.getNome();
        this.email = usuario.getEmail();
        this.perfil = usuario.getPerfil();
        this.situacao = usuario.getSituacao();
        this.ativo = usuario.getAtivo();
        Unidade unidade = usuario.getUnidade();
        if (unidade != null) {
            this.unidadeId = unidade.getPublicId();
            this.unidadeNome = unidade.getNome();
            this.unidadeSigla = unidade.getSigla();
        }
        this.laboratorios = LaboratorioResumoDTO.de(usuario);
    }
}
