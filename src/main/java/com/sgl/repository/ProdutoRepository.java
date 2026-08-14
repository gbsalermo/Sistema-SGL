package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.Produto;
import com.sgl.model.enums.NivelRisco;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {
	
	Optional<Produto> findByPublicId(UUID publicId);

    List<Produto> findByRisco(NivelRisco risco);

    List<Produto> findByPerecivelTrue();

    List<Produto> findByNomeContainingIgnoreCase(String nome);

    boolean existsByCodigoReferencia(String codigoReferencia);

    boolean existsByCodigoReferenciaAndIdNot(
            String codigoReferencia,
            Long id
    );
}
