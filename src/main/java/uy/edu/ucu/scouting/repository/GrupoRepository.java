package uy.edu.ucu.scouting.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.edu.ucu.scouting.domain.Grupo;

public interface GrupoRepository extends JpaRepository<Grupo, Long> {

    List<Grupo> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    @Query("select distinct g from Grupo g left join fetch g.miembros where g.id = :id")
    Optional<Grupo> conMiembros(@Param("id") Long id);

    @Query("select count(g) > 0 from Grupo g join g.miembros m where m.id = :usuarioId and g.activo = true")
    boolean tieneGrupoActivo(@Param("usuarioId") Long usuarioId);

    @Query("select count(g) > 0 from Grupo g join g.miembros m "
            + "where m.id = :usuarioId and g.activo = true and g.esAdmin = true")
    boolean esAdmin(@Param("usuarioId") Long usuarioId);
}
