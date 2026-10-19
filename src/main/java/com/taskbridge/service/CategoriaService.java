package com.taskbridge.service;

import com.taskbridge.model.Categoria;
import com.taskbridge.exception.RegraNegocioException;
import com.taskbridge.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository repository;

    public CategoriaService(CategoriaRepository repository) {
        this.repository = repository;
    }

    /** Lista pequena e fixa (vem de migration), por isso sem paginação. */
    @Transactional(readOnly = true)
    public List<Categoria> listar() {
        return repository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public Categoria buscar(Long categoriaId) {
        return repository.findById(categoriaId).orElseThrow(() -> new RegraNegocioException("Categoria inválida."));
    }
}
