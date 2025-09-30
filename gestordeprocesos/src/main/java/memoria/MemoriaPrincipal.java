package memoria;

import instrucciones.Instruccion;
import java.util.ArrayList;
import java.util.List;
import procesos.BCP;

/**
 *
 * @author dylan
 */
public class MemoriaPrincipal {

    private final Object[] memoria;
    private final int tamanioTotal;
    private final int tamanioSO;
    private final int tamanioUsuario;

    private final int maxProcesoUsuario = 5;

    private int siguienteLibreUsuario;
    
    private int siguienteLibreSO = 0;

    public MemoriaPrincipal(int tamanioTotal, int tamanioSO) {
        if (tamanioSO >= tamanioTotal) {
            throw new IllegalArgumentException("La sección SO supera el tamaño total de memoria");
        }

        this.tamanioTotal = tamanioTotal;
        this.tamanioSO = tamanioSO;
        this.tamanioUsuario = tamanioTotal - tamanioSO;
        this.memoria = new Object[tamanioTotal];
        this.siguienteLibreUsuario = tamanioSO;
    }

    /**
     * Carga un BCP en la sección SO. Cada BCP ocupa 22 celdas (aproximadamente,
     * según atributos guardados).
     *
     * @param bcp BCP a guardar
     * @param direccionInicio índice en memoria donde iniciar almacenamiento
     */
    public int cargarBCP(BCP bcp) {
        final int CELDAS_POR_BCP = 22; // celdas que ocupa un BCP
        if (siguienteLibreSO + CELDAS_POR_BCP > tamanioSO) {
            throw new RuntimeException("No hay espacio en la sección SO para guardar el BCP");
        }

        int direccionBase = siguienteLibreSO;
        int idx = direccionBase;

        memoria[idx++] = bcp.getEstado().ordinal();
        memoria[idx++] = bcp.getProgramCounter();
        memoria[idx++] = bcp.getAc();
        memoria[idx++] = bcp.getAx();
        memoria[idx++] = bcp.getBx();
        memoria[idx++] = bcp.getCx();
        memoria[idx++] = bcp.getDx();

        List<Integer> pila = new ArrayList<>(bcp.getPila());
        for (int i = 0; i < 5; i++) {
            memoria[idx++] = i < pila.size() ? pila.get(i) : 0;
        }

        memoria[idx++] = bcp.getCpuId();
        memoria[idx++] = (int) bcp.getTiempoInicio();
        memoria[idx++] = (int) bcp.getTiempoEmpleado();
        memoria[idx++] = (int) bcp.getTiempoFinalizacion();
        memoria[idx++] = bcp.isEsperandoEntrada() ? 1 : 0;
        memoria[idx++] = bcp.getSiguiente();
        memoria[idx++] = bcp.getDireccionBase(); // opcional: actualizar luego con dirección real
        memoria[idx++] = bcp.getTamanio();
        memoria[idx++] = bcp.getPrioridad();
        memoria[idx++] = bcp.getPid();

        siguienteLibreSO += CELDAS_POR_BCP;
        return direccionBase; // devuelve la dirección donde quedó el BCP
    }

    /**
     * Carga un programa en la sección Usuario.
     *
     * @param instrucciones lista de instrucciones parseadas
     * @return dirección base en memoria donde se cargó
     */
    public int cargarProgramaUsuario(List<Instruccion> instrucciones) {
        if (instrucciones.size() + siguienteLibreUsuario > tamanioTotal) {
            throw new RuntimeException("No hay espacio en memoria para cargar el programa");
        }

        int direccionBase = siguienteLibreUsuario;
        for (Instruccion instr : instrucciones) {
            memoria[siguienteLibreUsuario++] = instr;
        }
        return direccionBase;
    }

    /**
     * Obtiene la instrucción de usuario en una dirección específica
     *
     * @param direccion índice en memoria
     * @return instrucción
     */
    public Instruccion leerInstruccionUsuario(int direccion) {
        if (direccion < tamanioSO || direccion >= tamanioTotal) {
            throw new RuntimeException("Dirección fuera de la sección de usuario");
        }
        Object obj = memoria[direccion];
        if (!(obj instanceof Instruccion)) {
            throw new RuntimeException("No hay instrucción en la dirección solicitada");
        }
        return (Instruccion) obj;
    }

    /**
     * Actualiza un atributo de un BCP en memoria
     *
     * @param direccionInicio índice del BCP
     * @param offset desplazamiento del atributo dentro del BCP
     * @param valor nuevo valor
     */
    public void actualizarAtributoBCP(int direccionInicio, int offset, int valor) {
        if (direccionInicio + offset >= tamanioSO) {
            throw new RuntimeException("Fuera de la sección SO");
        }
        memoria[direccionInicio + offset] = valor;
    }

    /**
     * Devuelve una copia de la memoria (debug)
     */
    public Object[] getMemoria() {
        return memoria.clone();
    }

    public int getTamanioTotal() {
        return tamanioTotal;
    }

    public int getTamanioSO() {
        return tamanioSO;
    }

    public int getTamanioUsuario() {
        return tamanioUsuario;
    }
}
