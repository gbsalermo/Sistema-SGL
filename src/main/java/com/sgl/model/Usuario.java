package com.sgl.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.sgl.exception.BusinessRuleException;
import com.sgl.model.enums.OrigemUsuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(nullable = false)
    private String nome;

    // Optional: corporate users authenticate against the corporate directory and have no local email/password.
    @Column(unique = true)
    private String email;

    @Column
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Perfil perfil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laboratorio_id")
    private Laboratorio laboratorio;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(unique = true)
    private String login;

    @Column(unique = true)
    private String matricula;

    @Column(unique = true)
    private String cpf;

    @Column
    private String cargo;

    @Column
    private String sublotacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemUsuario origem = OrigemUsuario.DEV;

    // Corporate users start as PENDENTE_VINCULACAO until an admin assigns their unit.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoUsuario situacao = SituacaoUsuario.ATIVO;

    @Column(name = "ultimo_login_em")
    private OffsetDateTime ultimoLoginEm;

    @Column(name = "sincronizado_em")
    private OffsetDateTime sincronizadoEm;

    // Keeps the pre-corporate-auth signature working for local (DEV) users created by seeders and tests.
    public Usuario(Long id, UUID publicId, String nome, String email, String senha, Perfil perfil,
            Unidade unidade, Laboratorio laboratorio, Boolean ativo) {
        this.id = id;
        this.publicId = publicId;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.perfil = perfil;
        this.unidade = unidade;
        this.laboratorio = laboratorio;
        this.ativo = ativo;
    }

    public void validateInternProfile() {
        if (perfil != Perfil.ESTAGIARIO) {
            throw new BusinessRuleException(
                    "Usuário deve ter perfil ESTAGIARIO para cadastro de estagiário."
            );
        }
    }

    public void validateActive() {
        if (!Boolean.TRUE.equals(ativo)) {
            throw new BusinessRuleException("O usuário está inativo.");
        }
    }

    @PrePersist
    private void generatePublicId() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}
