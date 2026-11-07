package com.sgl.model;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
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

    /*
     * The laboratories the person works in, all from the person's Unidade. Replaces the
     * single "laboratório-base": a person may operate in more than one laboratory without
     * needing an open-ended vínculo operacional for each extra one.
     */
    @ManyToMany
    @JoinTable(
            name = "usuario_laboratorios",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "laboratorio_id")
    )
    @Setter(AccessLevel.NONE)
    private Set<Laboratorio> laboratorios = new LinkedHashSet<>();

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
        // The single laboratory, when given, becomes a one-item list.
        this.id = id;
        this.publicId = publicId;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.perfil = perfil;
        this.unidade = unidade;
        if (laboratorio != null) {
            this.laboratorios.add(laboratorio);
        }
        this.ativo = ativo;
    }

    /*
     * Compares by id because the set may hold Hibernate proxies or detached
     * instances that are not the same object as the argument.
     */
    public boolean atuaNoLaboratorio(Laboratorio laboratorio) {
        if (laboratorio == null || laboratorio.getId() == null) {
            return false;
        }
        return laboratorios.stream()
                .anyMatch(item -> Objects.equals(item.getId(), laboratorio.getId()));
    }

    public List<Laboratorio> laboratoriosOrdenados() {
        return laboratorios.stream()
                .sorted(Comparator.comparing(Laboratorio::getNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    // Mutates the managed collection in place so Hibernate tracks the change as row diffs.
    public void substituirLaboratorios(Collection<Laboratorio> novos) {
        laboratorios.clear();
        laboratorios.addAll(novos);
    }

    public void limparLaboratorios() {
        laboratorios.clear();
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
