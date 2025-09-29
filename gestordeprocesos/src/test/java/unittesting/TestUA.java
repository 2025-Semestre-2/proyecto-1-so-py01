package unittesting;

import almacenamiento.UnidadDeAlmacenamiento;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author dylan
 */
public class TestUA {
    public static void main(String[] args) {
        UnidadDeAlmacenamiento ua = new UnidadDeAlmacenamiento();

        // Simular programas en "disco" (no parseados aún)
        List<String> lineas1 = Arrays.asList(
            "PARAM 10, 5",
            "MOV AX, 0",
            "MOV BX, 0",
            "POP AX",
            "POP BX",
            "ADD BX",
            "MOV DX, AC",
            "INT 10H",
            "MOV AX, DX",
            "SUB BX",
            "MOV DX, AC",
            "INT 10H",
            "INT 20H"
        );

        List<String> lineas2 = Arrays.asList(
            "MOV AX, 0",
            "MOV BX, 10",
            "MOV CX, 0",
            "INT 09H",
            "MOV AX, DX",
            "MOV CX, AX",
            "LOAD AX",
            "ADD BX",
            "MOV DX, AC",
            "INT 10H",
            "INC CX",
            "MOV DX, CX",
            "INT 10H",
            "DEC CX",
            "MOV DX, CX",
            "INT 10H",
            "INT 20H"
        );

        List<String> lineas3 = Arrays.asList(
            "PARAM 15, 25",
            "MOV AX, 0",
            "MOV BX, 0",
            "POP AX",
            "POP BX",
            "MOV DX, AX",
            "INT 10H",
            "MOV DX, BX",
            "INT 10H",
            "SWAP AX, BX",
            "MOV DX, AX",
            "INT 10H",
            "MOV DX, BX",
            "INT 10H",
            "INT 20H"
        );

        // Preparar arrays para UnidadDeAlmacenamiento
        String[] nombres = {"Programa1", "Programa2", "Programa3"};
        List<String>[] programas = new List[]{lineas1, lineas2, lineas3};

        // Cargar programas (solo strings, como si fueran en disco)
        ua.cargarProgramas(nombres, programas);

        // Mostrar estado del almacenamiento
        ua.mostrarAlmacenamiento();
    }
}
