package almacenamiento;

import java.util.ArrayList;
import java.util.List;

/**
 * Unidad de Almacenamiento (simula el disco)
 * Guarda programas como texto ASM, sin parsear.
 *
 * @author dylan y gadyr
 */
public class UnidadDeAlmacenamiento {

    private final int tamañoTotal;        
    private final int memoriaVirtual;     
    private final List<String> almacenamiento; 

    public UnidadDeAlmacenamiento(int tamañoTotal, int memoriaVirtual) {
        if (memoriaVirtual >= tamañoTotal) {
            throw new IllegalArgumentException("Memoria virtual no puede ser mayor o igual al tamaño total.");
        }
        this.tamañoTotal = tamañoTotal;
        this.memoriaVirtual = memoriaVirtual;
        this.almacenamiento = new ArrayList<>(tamañoTotal);

        // Inicializar almacenamiento vacío
        for (int i = 0; i < tamañoTotal; i++) {
            almacenamiento.add(null);
        }
    }

    public UnidadDeAlmacenamiento() {
        this(512, 64);
    }

    /**
     * Carga programas en el almacenamiento como texto (ASM).
     * 
     * @param nombres nombres de programas
     * @param programas array con listas de líneas ASM
     */
    public void cargarProgramas(String[] nombres, List<String>[] programas) {
        if (nombres.length != programas.length) {
            throw new IllegalArgumentException("Cantidad de nombres y programas no coincide.");
        }

        // Resetear almacenamiento
        for (int i = 0; i < tamañoTotal; i++) {
            almacenamiento.set(i, null);
        }

        int memoriaLimite = tamañoTotal - memoriaVirtual;
        int indiceInicio = 0;
        int longitudPrograma = 0;
        List<String> nombresValidos = new ArrayList<>();
        List<List<String>> programasValidos = new ArrayList<>();

        // Validación de espacio
        int espacioDisponible = memoriaLimite;
        for (int i = 0; i < nombres.length; i++) {
            List<String> lineas = programas[i];
            if (lineas.size() <= espacioDisponible) {
                nombresValidos.add(nombres[i]);
                programasValidos.add(lineas);
                espacioDisponible -= lineas.size();
            } else {
                System.out.println("No hay suficiente espacio para el programa " + nombres[i] + ", se omite.");
            }
        }
        
        // Cargar programas válidos
        indiceInicio = nombresValidos.size();
        for (int i = 0; i < nombresValidos.size(); i++) {
            String progNombre = nombresValidos.get(i);
            List<String> lineas = programasValidos.get(i);

            // Guardar en índice: "nombre;posicionInicio"
            almacenamiento.set(i, progNombre + ";" + indiceInicio + ";" + lineas.size());

            for (String linea : lineas) {
                almacenamiento.set(indiceInicio, linea); // guarda ASM puro
                indiceInicio++;
            }
        }
    }

    /**
     * Leer un programa desde el almacenamiento, buscándolo por nombre.
     * 
     * @param nombre nombre del programa
     * @return lista de instrucciones ASM, o null si no existe
     */
    public List<String> leerPrograma(String nombre) {
        for (int i = 0; i < tamañoTotal - memoriaVirtual; i++) {
            String celda = almacenamiento.get(i);
            if (celda != null && celda.startsWith(nombre + ";")) {
                String[] parts = celda.split(";");   
                int lineaIni = Integer.parseInt(parts[1]);
                int inicio = Integer.parseInt(parts[1]);
                int longitud = Integer.parseInt(parts[2]);
                
                List<String> programa = new ArrayList<>();

                while (inicio < almacenamiento.size() && almacenamiento.get(inicio) != null && inicio < (lineaIni + longitud)) {
                    programa.add(almacenamiento.get(inicio));
                    inicio++;
                }
                return programa;
            }
        }
        return null;
    }    
    
    /**
     * Muestra contenido de la unidad (debug).
     */
    public void mostrarAlmacenamiento() {
        for (int i = 0; i < almacenamiento.size(); i++) {
            String valor = almacenamiento.get(i);
            if (valor != null) {
                System.out.println("[" + i + "] " + valor);
            } else {
                System.out.println("[" + i + "] 0");
            }
        }
    }

    public List<String> getAlmacenamiento() {
        return almacenamiento;
    }

    public int getTamañoTotal() {
        return tamañoTotal;
    }

    public int getMemoriaVirtual() {
        return memoriaVirtual;
    }
}
