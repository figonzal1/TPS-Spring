package com.tps.msusuario.servicio.impl;

import com.tps.msusuario.repositorio.UsuarioJpaRepository;
import com.tps.msusuario.repositorio.entidad.Usuario;
import com.tps.msusuario.servicio.UsuarioService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("usuarioService")
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioJpaRepository usuarioJpaRepository;

    public UsuarioServiceImpl(@Qualifier("usuarioJpaRespository") UsuarioJpaRepository usuarioJpaRepository) {
        this.usuarioJpaRepository = usuarioJpaRepository;
    }

    @Override
    public List<Usuario> getUsuarios() {
        return usuarioJpaRepository.findAll();
    }

    @Override
    public Usuario agregarUsuario(Usuario usuario) {
        return usuarioJpaRepository.save(usuario);
    }

    @Override
    public int eliminarUsuario(Usuario usuario) {
        usuarioJpaRepository.delete(usuario);
        return 0;
    }

    @Override
    public Usuario editarUsuario(Usuario usuario) {
        return null;
    }
}
