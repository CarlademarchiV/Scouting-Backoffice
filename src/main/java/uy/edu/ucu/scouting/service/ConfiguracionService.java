package uy.edu.ucu.scouting.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uy.edu.ucu.scouting.domain.Configuracion;
import uy.edu.ucu.scouting.repository.ConfiguracionRepository;
import uy.edu.ucu.scouting.web.form.ConfiguracionForm;

@Service
@RequiredArgsConstructor
public class ConfiguracionService {

    private final ConfiguracionRepository repo;

    @Transactional(readOnly = true)
    public Configuracion obtener() {
        return repo.findById(Configuracion.ID)
                .orElseThrow(() -> new IllegalStateException("Falta la fila de configuración (id = 1)."));
    }

    @Transactional
    public void guardar(ConfiguracionForm f, String actor) {
        Configuracion c = obtener();
        c.setMaxParticipantes(f.getMaxParticipantes());
        c.setMaxExploracionesActivas(f.getMaxExploracionesActivas());
        c.setDominiosPermitidos(f.getDominiosPermitidos().trim());
        c.setMaxIntentos(f.getMaxIntentos());
        c.setFallasParaDesactivar(f.getFallasParaDesactivar());
        c.setCorreoAlertasTi(StringUtils.hasText(f.getCorreoAlertasTi()) ? f.getCorreoAlertasTi().trim() : null);
        c.setPieCorreo(StringUtils.hasText(f.getPieCorreo()) ? f.getPieCorreo().trim() : null);
        c.setModificadoEn(Instant.now());
        c.setModificadoPor(actor);
        repo.save(c);
    }
}
