package procesos;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Planificador para 5 CPUs simultáneas (FIFO)
 * @author dylan
 */
public class Planificador {

    private static final int NUM_CPUS = 5;

    private Queue<BCP> colaListos;
    private Queue<BCP> colaEspera;
    private BCP[] procesosEnEjecucion; // procesos ejecutándose en cada CPU

    public Planificador() {
        this.colaListos = new LinkedList<>();
        this.colaEspera = new LinkedList<>();
        this.procesosEnEjecucion = new BCP[NUM_CPUS];
    }

    /** Agrega un proceso a la cola de listos */
    public void agregarProcesoListo(BCP proceso) {
        proceso.setEstado(Estado.PREPARADO);
        colaListos.add(proceso);
    }

    /** Agrega un proceso a la cola de espera (ej: por E/S) */
    public void agregarProcesoEspera(BCP proceso) {
        proceso.setEstado(Estado.ESPERA);
        colaEspera.add(proceso);
    }

    /** Mueve un proceso de espera a la cola de listos */
    public void moverEsperaAListos(BCP proceso) {
        if (colaEspera.remove(proceso)) {
            agregarProcesoListo(proceso);
        }
    }

    /** Asigna procesos listos a CPUs libres */
    public void despacharProcesos() {
        // FCFS: Solo un proceso a la vez en ejecución
        if (procesosEnEjecucion[0] == null && !colaListos.isEmpty()) {
            BCP proceso = colaListos.poll();
            procesosEnEjecucion[0] = proceso;
            proceso.setEstado(Estado.EJECUCION);
            proceso.setCpuID(0);
        }
    }

    /** Finaliza el proceso que está corriendo en un CPU específico */
    public void finalizarProceso(int cpuID) {
        if (cpuID < 0 || cpuID >= NUM_CPUS) return;
        BCP proceso = procesosEnEjecucion[cpuID];
        if (proceso != null) {
            proceso.setEstado(Estado.FINALIZADO);
            procesosEnEjecucion[cpuID] = null;

            colaListos.remove(proceso);
            colaEspera.remove(proceso);
        }
    }

    /** Libera un CPU sin finalizar el proceso (por ejemplo, espera de E/S) */
    public void liberarCPU(int cpuID) {
        if (cpuID < 0 || cpuID >= NUM_CPUS) return;
        BCP proceso = procesosEnEjecucion[cpuID];
        if (proceso != null) {
            proceso.setEstado(Estado.ESPERA);
            agregarProcesoEspera(proceso);
            procesosEnEjecucion[cpuID] = null;
        }
    }

    /** Devuelve el proceso que está corriendo en un CPU */
    public BCP getProcesoCPU(int cpuID) {
        if (cpuID < 0 || cpuID >= NUM_CPUS) return null;
        return procesosEnEjecucion[cpuID];
    }

    /** Devuelve todos los procesos en ejecución */
    public BCP[] getProcesosEnEjecucion() {
        return procesosEnEjecucion.clone();
    }

    public Queue<BCP> getColaListos() {
        return colaListos;
    }

    public Queue<BCP> getColaEspera() {
        return colaEspera;
    }

    /** Muestra un resumen del estado de CPUs y colas */
    public void imprimirEstadoColas() {
        System.out.println("=== Estado del Planificador ===");

        for (int cpu = 0; cpu < NUM_CPUS; cpu++) {
            BCP bcp = procesosEnEjecucion[cpu];
            System.out.println("CPU " + cpu + ": " + (bcp != null ? bcp.getPid() : "Libre"));
        }

        System.out.print("Cola de listos: ");
        for (BCP b : colaListos) {
            System.out.print(b.getPid() + " ");
        }
        System.out.println();

        System.out.print("Cola de espera: ");
        for (BCP b : colaEspera) {
            System.out.print(b.getPid() + " ");
        }
        System.out.println();
    }
}
