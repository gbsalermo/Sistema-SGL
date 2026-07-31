package com.sgl.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>{
	
	Optional<Usuario> findByEmail(String email); //encontrar usuario com esse email
	
	List<Usuario> findByLaboratorioId(Long laboratorioId); //Listar os usuarios por laboratorio
	
	boolean existsByEmail(String email); //Verificar se existe usuario com esse email
	
	boolean existsByEmailAndIdNot(String email,Long id); //Papel parecido Com existsByEmail, mas aplicado para o metodo atualizar
}
