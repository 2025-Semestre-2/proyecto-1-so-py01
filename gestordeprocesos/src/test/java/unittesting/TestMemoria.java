package unittesting;


import almacenamiento.UnidadDeAlmacenamiento;
import instrucciones.Instruccion;
import instrucciones.InstructionParser;
import memoria.MemoriaPrincipal;
import procesos.BCP;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author dylan
 */
public class TestMemoria {
    public static void main(String[] args) {

        // Unidad de alamacenamiento
        UnidadDeAlmacenamiento ua = new UnidadDeAlmacenamiento(30, 10);

        List<String> lineas1 = Arrays.asList(
                "PARAM 10, 5",
                "MOV AX, 0",
                "MOV BX, 0",
                "POP AX",
                "POP BX",
                "ADD BX"
        );

        List<String> lineas2 = Arrays.asList(
                "MOV AX, 0",
                "MOV BX, 10",
                "MOV CX, 0",
                "INT 09H"
        );

        List<String> lineas3 = Arrays.asList(
                "PARAM 15, 25",
                "MOV AX, 0",
                "MOV BX, 0"
        );

        String[] nombres = {"Programa1", "Programa2", "Programa3"};
        List<String>[] programas = new List[]{lineas1, lineas2, lineas3};

        ua.cargarProgramas(nombres, programas);

        System.out.println("=== Unidad de Almacenamiento ===");
        ua.mostrarAlmacenamiento();
        
        // memoria principal
        MemoriaPrincipal memoria = new MemoriaPrincipal(100, 70);
        
        // Instrucciones y  BCP
        for (String nombre : nombres) {
            List<String> codigoDisco = ua.leerPrograma(nombre);
            if (codigoDisco == null) continue;

            // Parsear instrucciones
            List<Instruccion> instrucciones = InstructionParser.parseAll(codigoDisco);

            // Crear BCP
            BCP bcp = new BCP(nombre, memoria.getTamanioSO(), instrucciones.size(), 1);

            // Cargar BCP en memoria SO
            memoria.cargarBCP(bcp);

            // Cargar instrucciones en memoria usuario
            int dirUsuario = memoria.cargarProgramaUsuario(instrucciones);
            System.out.println("Programa '" + nombre + "' cargado en memoria usuario desde celda " + dirUsuario);
        }

        // Mostrar estado de memoria
        System.out.println("=== Memoria Principal (resumen) ===");
        Object[] mem = memoria.getMemoria();
        for (int i = 0; i < mem.length; i++) {
            Object celda = mem[i];
            if (celda != null) {
                System.out.println("[" + i + "] " + celda);
            } else {
                System.out.println("[" + i + "] 0");
            }
        }       
    }    
}
