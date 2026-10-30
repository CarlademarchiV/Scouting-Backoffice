package uy.edu.ucu.scouting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.ucu.scouting.domain.Configuracion;

public interface ConfiguracionRepository extends JpaRepository<Configuracion, Integer> {
}
