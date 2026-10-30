package uy.edu.ucu.scouting.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "grupo")
@Getter
@Setter
public class Grupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String descripcion;
    private boolean esAdmin;
    /** null = rige el valor global. */
    private Integer maxParticipantes;
    /** null = rige el valor global. */
    private Integer maxExploracionesActivas;
    private boolean activo = true;

    @ManyToMany
    @JoinTable(name = "grupo_miembro",
            joinColumns = @JoinColumn(name = "grupo_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id"))
    @OrderBy("nombre")
    private Set<Usuario> miembros = new LinkedHashSet<>();

    private Instant creadoEn = Instant.now();
    private String creadoPor;
    private Instant modificadoEn;
    private String modificadoPor;
}
