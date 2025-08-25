package co.com.pragma.model.usuario.gateways;

import co.com.pragma.model.usuario.Usuario;
import reactor.core.publisher.Mono;

public interface UsuarioRepository {
    Mono<Usuario> save(Usuario usuario);
    Mono<Boolean> existsByEmail(String email);
}
