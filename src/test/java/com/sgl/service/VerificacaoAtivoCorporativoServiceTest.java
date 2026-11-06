package com.sgl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.sgl.corporate.CorporateApiClient;
import com.sgl.corporate.CorporateApiProperties;
import com.sgl.model.Usuario;

class VerificacaoAtivoCorporativoServiceTest {

    // Dev/demo run without corporate credentials: creating a link must keep working there.
    @Test
    void integracaoDesligadaPulaConferencia() {
        CorporateApiClient client = new CorporateApiClient(RestClient.builder(),
                new CorporateApiProperties("http://localhost:0", "h", null, null, false));
        // Null on purpose: any attempt to deactivate someone would fail the test with an NPE.
        VerificacaoAtivoCorporativoService verificacao = new VerificacaoAtivoCorporativoService(client, null);

        assertThat(client.isEnabled()).isFalse();
        assertThatCode(() -> verificacao.exigirAtivos(pessoa("Estagiária", "12345678909", null),
                pessoa("Orientadora", null, "344136"))).doesNotThrowAnyException();
    }

    private static Usuario pessoa(String nome, String cpf, String matricula) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setCpf(cpf);
        usuario.setMatricula(matricula);
        usuario.setAtivo(true);
        return usuario;
    }
}
