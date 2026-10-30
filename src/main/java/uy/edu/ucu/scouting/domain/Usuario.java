package uy.edu.ucu.scouting.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entraOid;
    private String upn;
    private String nombre;
    private String email;
    private boolean activo = true;
    private Instant ultimoAcceso;
    private Instant creadoEn = Instant.now();
}
