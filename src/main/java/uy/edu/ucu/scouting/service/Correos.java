package uy.edu.ucu.scouting.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Utilidades para listas de correos pegadas por el usuario. */
public final class Correos {

    private static final Pattern VALIDO =
            Pattern.compile("^[a-z0-9._%+'-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,}$");

    private Correos() {
    }

    /** Separa por coma, punto y coma, espacios o saltos de línea; normaliza y quita repetidos. */
    public static List<String> parsear(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        return Arrays.stream(texto.split("[,;\\s]+"))
                .map(Correos::normalizar)
                .filter(c -> !c.isEmpty())
                .distinct()
                .toList();
    }

    public static String normalizar(String correo) {
        if (correo == null) {
            return "";
        }
        return correo.trim().replaceAll("^[<\"']+|[>\"']+$", "").toLowerCase(Locale.ROOT);
    }

    public static boolean esValido(String correo) {
        return correo != null && correo.length() <= 320 && VALIDO.matcher(correo).matches();
    }

    public static String dominio(String correo) {
        int i = correo.lastIndexOf('@');
        return i < 0 ? "" : correo.substring(i + 1);
    }
}
