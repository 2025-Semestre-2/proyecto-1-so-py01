package procesos;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * 
 * @author dylan
 */
public class BCP {

    // Estado y PC
    private Estado estado;
    private int programCounter;

    // Registros
    private int ac, ax, bx, cx, dx;

    // Pila (estado)
    private Stack<Integer> pila;
    private static final int TAMANIO_MAXIMO_PILA = 5;

    // Contables
    private int cpuID;
    private long tiempoInicio;
    private long tiempoEmpleado;
    private long tiempoFinalizacion;

    // Información de E/S
    private List<String> archivosAbiertos;
    private boolean esperandoEntrada;

    // Dirección en memoria del siguiente BCP (para listas/colas)
    private int dirSiguienteBCP;

    // Memoria
    private int direccionBase;
    private int tamanio;

    // Prioridad
    private int prioridad;

    // Info adicional
    private String nombreArchivo;
    private int pid;
    private String instruccionActual;

    private static int siguientePID = 1;

    public BCP(String nombreArchivo, int direccionBase, int tamaño, int prioridad) {
        this.nombreArchivo = nombreArchivo;
        this.pid = siguientePID++;
        this.direccionBase = direccionBase;
        this.tamanio = tamaño;
        this.prioridad = prioridad;

        this.estado = Estado.NUEVO;
        this.programCounter = direccionBase;

        // Inicializar registros
        this.ac = 0;
        this.ax = 0;
        this.bx = 0;
        this.cx = 0;
        this.dx = 0;

        // Pila inicial vacía
        this.pila = new Stack<>();

        // Contables
        this.cpuID = -1; // no asignado
        this.tiempoInicio = 0;
        this.tiempoEmpleado = 0;
        this.tiempoFinalizacion = 0;

        // E/S
        this.archivosAbiertos = new ArrayList<>();
        this.esperandoEntrada = false;

        // Enlace
        this.dirSiguienteBCP = -1;
        this.instruccionActual = "";
    }

    // Estados
    public void cambiarEstado(Estado nuevoEstado) {
        long tiempoActual = System.currentTimeMillis();

        if (this.estado == Estado.EJECUCION && nuevoEstado != Estado.EJECUCION) {
            tiempoEmpleado += tiempoActual - tiempoInicio;
        }
        if (nuevoEstado == Estado.EJECUCION) {
            tiempoInicio = tiempoActual;
        }
        if (nuevoEstado == Estado.FINALIZADO) {
            tiempoFinalizacion = tiempoActual;
        }

        this.estado = nuevoEstado;
    }

    // Validaciones memoria/PC
    public void incrementarPC() {
        programCounter++;
    }

    public void setProgramCounter(int nuevaDireccion) {
        if (!direccionValida(nuevaDireccion)) {
            throw new RuntimeException("Dirección inválida");
        }
        this.programCounter = nuevaDireccion;
    }

    public boolean direccionValida(int direccion) {
        return direccion >= direccionBase && direccion < direccionBase + tamanio;
    }

    // to string
    @Override
    public String toString() {
        return String.format(
                "BCP[%s] %s | PID:%d | PC:%d | AC:%d | AX:%d | BX:%d | CX:%d | DX:%d | Pila:%d/%d",
                nombreArchivo, estado, pid, programCounter,
                ac, ax, bx, cx, dx,
                pila.size(), TAMANIO_MAXIMO_PILA);
    }

    public String toStringCompleto() {
        return String.format(
                "BCP[%s] PID:%d Estado:%s PC:%d AC:%d Registros: AX=%d BX=%d CX=%d DX=%d "
                + "Pila:%s Tamaño:%d/%d Mem:@%d Pri:%d Archivos:%s EsperaE/S:%b",
                nombreArchivo, pid, estado, programCounter,
                ac, ax, bx, cx, dx,
                pila, pila.size(), TAMANIO_MAXIMO_PILA,
                direccionBase, prioridad,
                archivosAbiertos, esperandoEntrada);
    }

    // getters - setters
    public Estado getEstado() { return estado; }
    public int getProgramCounter() { return programCounter; }
    public int getAc() { return ac; }
    public int getAx() { return ax; }
    public int getBx() { return bx; }
    public int getCx() { return cx; }
    public int getDx() { return dx; }
    public int getCpuId() { return cpuID; }
    public long getTiempoInicio() { return tiempoInicio; }
    public long getTiempoEmpleado() { return tiempoEmpleado; }
    public long getTiempoFinalizacion() { return tiempoFinalizacion; }
    public List<String> getArchivosAbiertos() { return new ArrayList<>(archivosAbiertos); }
    public boolean isEsperandoEntrada() { return esperandoEntrada; }
    public int getSiguiente() { return dirSiguienteBCP; }
    public int getDireccionBase() { return direccionBase; }
    public int getTamanio() { return tamanio; }
    public int getPrioridad() { return prioridad; }
    public String getNombreArchivo() { return nombreArchivo; }
    public int getPid() { return pid; }
    public String getInstruccionActual() { return instruccionActual; }
    public Stack<Integer> getPila() { return (Stack<Integer>) pila.clone(); }

    public void setEstado(Estado estado) { this.estado = estado; }
    public void setAc(int ac) { this.ac = ac; }
    public void setAx(int ax) { this.ax = ax; }
    public void setBx(int bx) { this.bx = bx; }
    public void setCx(int cx) { this.cx = cx; }
    public void setDx(int dx) { this.dx = dx; }
    public void setCpuID(int cpuID) { this.cpuID = cpuID; }
    public void setSiguiente(int dirSiguienteBCP) { this.dirSiguienteBCP = dirSiguienteBCP; }
    public void setInstruccionActual(String instruccionActual) { this.instruccionActual = instruccionActual; }
    public void setEsperandoEntrada(boolean esperandoEntrada) { this.esperandoEntrada = esperandoEntrada; }
    public void setArchivosAbiertos(List<String> archivos) { this.archivosAbiertos = archivos; }
    public void setTiempoEmpleado(long tiempoEmpleado) { this.tiempoEmpleado = tiempoEmpleado; }

    public static void reiniciarContadorPID() { siguientePID = 1; }
}
