package com.sgl.test;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UnidadeRepository unidadeRepository;
    private final LaboratorioRepository laboratorioRepository;

    @Override
    public void run(String... args) throws Exception {
        Unidade u1 = unidadeRepository.save(new Unidade(null, "Instituto de Biologia", "IB", null));
        Unidade u2 = unidadeRepository.save(new Unidade(null, "Instituto de Fisica", "IF", null));
        Unidade u3 = unidadeRepository.save(new Unidade(null, "Instituto de Quimica", "IQ", null));

        laboratorioRepository.save(new Laboratorio(null, u1, "Laboratorio de Microbiologia", "Lab de estudo de microrganismos", "Dr. Carlos", true));
        laboratorioRepository.save(new Laboratorio(null, u1, "Laboratorio de Genetica", "Lab de analise genetica", "Dra. Ana", true));
        laboratorioRepository.save(new Laboratorio(null, u2, "Laboratorio de Optica", "Lab de estudo da luz", "Dr. Pedro", true));
        laboratorioRepository.save(new Laboratorio(null, u3, "Laboratorio de Quimica Organica", "Lab de sintese organica", "Dra. Maria", true));
        laboratorioRepository.save(new Laboratorio(null, u3, "Laboratorio de Analise Instrumental", "Lab de instrumentacao analitica", "Dr. Joao", false));

        System.out.println("=== Dados de teste injetados com sucesso! ===");
    }
}
