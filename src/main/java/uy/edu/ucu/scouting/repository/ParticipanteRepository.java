package uy.edu.ucu.scouting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.ucu.scouting.domain.Participante;

public interface ParticipanteRepository extends JpaRepository<Participante, Long> {

    long countByExploracionIdAndActivoTrue(Long exploracionId);

    boolean existsByExploracionIdAndCorreoIgnoreCase(Long exploracionId, String correo);
}
