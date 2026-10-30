package uy.edu.ucu.scouting.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.edu.ucu.scouting.domain.Ejecucion;
import uy.edu.ucu.scouting.domain.EstadoEjecucion;

public interface EjecucionRepository extends JpaRepository<Ejecucion, Long> {

    @Query("select x from Ejecucion x join fetch x.exploracion e join fetch e.propietario where x.id = :id")
    Optional<Ejecucion> conDetalle(@Param("id") Long id);

    boolean existsByExploracionIdAndModoAndEstadoIn(Long exploracionId, uy.edu.ucu.scouting.domain.ModoEjecucion modo,
                                                    Collection<EstadoEjecucion> estados);

    List<Ejecucion> findByExploracionIdOrderByCreadaEnDesc(Long exploracionId, Pageable pageable);

    @Query("select x from Ejecucion x join fetch x.exploracion e join fetch e.propietario p "
            + "where (:estado is null or x.estado = :estado) "
            + "and (:propietario is null or lower(p.upn) like lower(concat('%', :propietario, '%'))) "
            + "and (:desde is null or x.creadaEn >= :desde) "
            + "and (:hasta is null or x.creadaEn < :hasta) "
            + "order by x.creadaEn desc")
    List<Ejecucion> monitor(@Param("estado") EstadoEjecucion estado,
                            @Param("propietario") String propietario,
                            @Param("desde") Instant desde,
                            @Param("hasta") Instant hasta,
                            Pageable pageable);
}
