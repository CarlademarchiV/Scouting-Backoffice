package uy.edu.ucu.scouting.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.edu.ucu.scouting.domain.Exploracion;

public interface ExploracionRepository extends JpaRepository<Exploracion, Long> {

    @Query("select e from Exploracion e join fetch e.propietario where e.propietario.id = :usuarioId order by e.titulo")
    List<Exploracion> delPropietario(@Param("usuarioId") Long usuarioId);

    @Query("select e from Exploracion e join fetch e.propietario order by e.titulo")
    List<Exploracion> todas();

    @Query("select distinct e from Exploracion e join fetch e.propietario left join fetch e.participantes where e.id = :id")
    Optional<Exploracion> conDetalle(@Param("id") Long id);

    @Query("select count(e) from Exploracion e where e.propietario.id = :usuarioId and e.activa = true "
            + "and (:excluirId is null or e.id <> :excluirId)")
    long contarActivas(@Param("usuarioId") Long usuarioId, @Param("excluirId") Long excluirId);
}
