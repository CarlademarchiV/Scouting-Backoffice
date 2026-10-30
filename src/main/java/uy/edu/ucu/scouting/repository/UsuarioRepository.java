package uy.edu.ucu.scouting.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.ucu.scouting.domain.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEntraOid(String entraOid);

    Optional<Usuario> findByUpnIgnoreCase(String upn);
}
