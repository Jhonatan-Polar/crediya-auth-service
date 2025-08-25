package co.com.pragma.usecase.registrarusuario;

import co.com.pragma.model.exception.DuplicateEmailException;
import co.com.pragma.model.exception.ValidationException;
import co.com.pragma.model.usuario.Usuario;
import co.com.pragma.model.usuario.gateways.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class RegistrarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;
    private static final BigDecimal SALARIO_MIN = BigDecimal.ZERO;
    private static final BigDecimal SALARIO_MAX = new BigDecimal("15000000");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public Mono<Usuario> ejecutar(Usuario usuario) {
        return Mono.justOrEmpty(usuario)
                .switchIfEmpty(Mono.error(new ValidationException("El objeto Usuario es requerido")))
                .map(this::normalizarEntradas)
                .flatMap(this::validarCamposObligatorios)
                .flatMap(this::validarFormatos)
                .flatMap(this::validarRangos)
                .flatMap(this::validarEmailUnico)
                .flatMap(usuarioRepository::save)
                .doOnSuccess(u -> log.info("Usuario registrado exitosamente: {}", u.getCorreoElectronico()))
                .doOnError(e -> log.error("Error registrando usuario [{}]: {}",
                        safeEmail(usuario), e.getMessage()));
    }

    private String safeEmail(Usuario u) {
        return u != null ? u.getCorreoElectronico() : "desconocido";
    }

    /** Normaliza espacios y correo en mayuscula */
    private Usuario normalizarEntradas(Usuario u) {
        if (u.getNombres() != null) u.setNombres(u.getNombres().trim());
        if (u.getApellidos() != null) u.setApellidos(u.getApellidos().trim());
        if (u.getDireccion() != null) u.setDireccion(u.getDireccion().trim());
        if (u.getTelefono() != null) u.setTelefono(u.getTelefono().trim());
        if (u.getCorreoElectronico() != null) u.setCorreoElectronico(u.getCorreoElectronico().trim().toUpperCase());
        return u;
    }

    /** Valida requeridos: nombres, apellidos, correo y salario */
    private Mono<Usuario> validarCamposObligatorios(Usuario u) {
        if (isBlank(u.getNombres())) {
            return Mono.error(new ValidationException("El campo 'nombres' es requerido"));
        }
        if (isBlank(u.getApellidos())) {
            return Mono.error(new ValidationException("El campo 'apellidos' es requerido"));
        }
        if (isBlank(u.getCorreoElectronico())) {
            return Mono.error(new ValidationException("El campo 'correo_electronico' es requerido"));
        }
        if (Objects.isNull(u.getSalarioBase())) {
            return Mono.error(new ValidationException("El campo 'salario_base' es requerido"));
        }
        return Mono.just(u);
    }

    /** Valida correo */
    private Mono<Usuario> validarFormatos(Usuario u) {
        if (!EMAIL_PATTERN.matcher(u.getCorreoElectronico()).matches()) {
            return Mono.error(new ValidationException("El 'correo_electronico' no tiene un formato válido"));
        }
        return Mono.just(u);
    }

    /** Valida rangos: salario entre 0 y 15,000,000 */
    private Mono<Usuario> validarRangos(Usuario u) {
        if (u.getSalarioBase().compareTo(SALARIO_MIN) < 0 ||
                u.getSalarioBase().compareTo(SALARIO_MAX) > 0) {
            return Mono.error(new ValidationException("El 'salario_base' debe estar entre 0 y 15000000"));
        }
        return Mono.just(u);
    }

    /** Verifica duplicidad del correo */
    private Mono<Usuario> validarEmailUnico(Usuario u) {
        return usuarioRepository.existsByEmail(u.getCorreoElectronico())
                .flatMap(existe -> existe
                        ? Mono.error(new DuplicateEmailException("El correo ya se encuentra registrado"))
                        : Mono.just(u));
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
