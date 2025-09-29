package instrucciones;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author dylan y gadyr
 */
public class InstructionParser {

    /**
     * Parsea una línea ASM y devuelve la Instruccion correspondiente.
     *
     * @param linea Línea de código (ej: "MOV AX, 5")
     * @return Instruccion validada
     * @throws IllegalArgumentException si la línea es inválida
     */
    public static Instruccion parse(String linea) {
        if (linea == null || linea.trim().isEmpty()) {
            throw new IllegalArgumentException("Línea vacía");
        }

        // Elimina comentarios ";"
        String sinComentario = linea.split(";", 2)[0].trim();
        if (sinComentario.isEmpty()) {
            throw new IllegalArgumentException("Línea vacía tras comentario");
        }

        // Tokenizar
        String[] partes = sinComentario.split("\\s+", 2);
        String opStr = partes[0].toUpperCase();

        Opcode opcode;
        try {
            opcode = Opcode.valueOf(opStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Instrucción desconocida: " + opStr);
        }

        List<String> operandos = new ArrayList<>();
        if (partes.length > 1) {
            String resto = partes[1].trim();
            // Separar por comas
            operandos = new ArrayList<>(
                Arrays.asList(resto.split("\\s*,\\s*"))
            );
        }

        return new Instruccion(opcode, operandos);
    }

    /**
     * Parsea múltiples líneas ASM.
     *
     * @param lineas Lista de líneas de un archivo ASM
     * @return Lista de Instrucciones validadas
     */
    public static List<Instruccion> parseAll(List<String> lineas) {
        List<Instruccion> instrucciones = new ArrayList<>();
        for (String linea : lineas) {
            try {
                instrucciones.add(parse(linea));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    "Error en línea: \"" + linea + "\" -> " + e.getMessage()
                );
            }
        }
        return instrucciones;
    }
}