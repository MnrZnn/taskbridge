package com.taskbridge.repository;

import com.taskbridge.model.PerfilProfissional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PerfilProfissionalRepository extends JpaRepository<PerfilProfissional, Long> {

    Optional<PerfilProfissional> findByUsuarioId(Long usuarioId);

    /**
     * Candidatos ao matching de um projeto: perfis ativos que atendem a categoria ou que têm
     * ao menos uma habilidade pedida. O {@code Pageable} limita quantos são avaliados,
     * para que o cálculo em memória tenha custo previsível.
     */
    @Query("""
            select p from PerfilProfissional p join fetch p.usuario u
            where p.status = com.taskbridge.model.enums.PerfilStatus.ATIVO
              and p.disponibilidade <> com.taskbridge.model.enums.Disponibilidade.INDISPONIVEL
              and u.status = com.taskbridge.model.enums.UsuarioStatus.ATIVO
              and u.id <> :clienteId
              and (exists (select s.id from Servico s
                           where s.prestador = u
                             and s.status = com.taskbridge.model.enums.ServicoStatus.ATIVO
                             and s.categoria.id = :categoriaId)
                   or exists (select h from PerfilProfissional p2 join p2.habilidades h
                              where p2 = p and h in :habilidades))
            order by p.id
            """)
    List<PerfilProfissional> buscarCandidatos(@Param("clienteId") Long clienteId,
                                              @Param("categoriaId") Long categoriaId,
                                              @Param("habilidades") Collection<String> habilidades,
                                              Pageable limite);
}
