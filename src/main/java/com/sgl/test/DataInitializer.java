package com.sgl.test;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.sgl.model.Laboratorio;
import com.sgl.model.Perfil;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UnidadeRepository unidadeRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    public void run(String... args) throws Exception {
        // Unidades
        Unidade u1 = unidadeRepository.save(new Unidade(null, "Instituto de Biologia", "IB", null));
        Unidade u2 = unidadeRepository.save(new Unidade(null, "Instituto de Fisica", "IF", null));
        Unidade u3 = unidadeRepository.save(new Unidade(null, "Instituto de Quimica", "IQ", null));

        // Laboratórios (sem responsável ainda)
        Laboratorio lab1 = laboratorioRepository.save(new Laboratorio(null, u1, "Laboratorio de Microbiologia", "Lab de estudo de microrganismos", null, true));
        Laboratorio lab2 = laboratorioRepository.save(new Laboratorio(null, u1, "Laboratorio de Genetica", "Lab de analise genetica", null, true));
        Laboratorio lab3 = laboratorioRepository.save(new Laboratorio(null, u2, "Laboratorio de Optica", "Lab de estudo da luz", null, true));
        Laboratorio lab4 = laboratorioRepository.save(new Laboratorio(null, u3, "Laboratorio de Quimica Organica", "Lab de sintese organica", null, true));
        Laboratorio lab5 = laboratorioRepository.save(new Laboratorio(null, u3, "Laboratorio de Analise Instrumental", "Lab de instrumentacao analitica", null, false));

        // Usuários
        Usuario admin = usuarioRepository.save(new Usuario(null, "Admin Sistema", "admin@sgl.com", "123456", Perfil.ADMINISTRADOR, null, true));
        Usuario carlos = usuarioRepository.save(new Usuario(null, "Dr. Carlos Silva", "carlos@ib.com", "123456", Perfil.GESTOR, lab1, true));
        Usuario ana = usuarioRepository.save(new Usuario(null, "Dra. Ana Santos", "ana@ib.com", "123456", Perfil.TECNICO, lab2, true));
        Usuario joao = usuarioRepository.save(new Usuario(null, "Joao Pereira", "joao@if.com", "123456", Perfil.PESQUISADOR, lab3, true));
        Usuario maria = usuarioRepository.save(new Usuario(null, "Maria Oliveira", "maria@iq.com", "123456", Perfil.ESTAGIARIO, lab4, true));

        // Atualizar laboratórios com os responsáveis
        lab1.setResponsavel(carlos);
        lab2.setResponsavel(ana);
        lab3.setResponsavel(joao);
        lab4.setResponsavel(maria);
        lab5.setResponsavel(admin);

        laboratorioRepository.save(lab1);
        laboratorioRepository.save(lab2);
        laboratorioRepository.save(lab3);
        laboratorioRepository.save(lab4);
        laboratorioRepository.save(lab5);

        System.out.println("=== Dados de teste injetados com sucesso! ===");
        System.out.println("=== 3 Unidades, 5 Laboratórios, 5 Usuários ===");
    }
}
