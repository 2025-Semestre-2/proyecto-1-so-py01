/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sistema;

import almacenamiento.UnidadDeAlmacenamiento;
import cpu.CPU;
import instrucciones.Instruccion;
import instrucciones.InstructionParser;
import memoria.MemoriaPrincipal;
import procesos.BCP;
import procesos.Estado;
import procesos.Planificador;
import sistema.EstadisticaProceso;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Gestor principal del sistema operativo simulado
 * Coordina CPUs, memoria, almacenamiento y planificador
 * @author gadyr
 */
public class GestorSistema {
    
    private static final int NUM_CPUS = 5;
    
    // Componentes del sistema
    private UnidadDeAlmacenamiento almacenamiento;
    private MemoriaPrincipal memoria;
    private Planificador planificador;
    private CPU[] cpus;
    
    // Callbacks para actualizar GUI
    private Consumer<String> consolaCallback;
    private Consumer<String> pantallaCallback;
    private Runnable actualizarGUICallback;
    
    // Control de ejecución
    private boolean ejecutando = false;
    private Thread hiloEjecucion;
    
    // Estadísticas
    private List<EstadisticaProceso> estadisticas;
    
    /**
     * Constructor con configuración por defecto
     */
    public GestorSistema() {
        this(512, 412, 512, 64);
    }
    
    /**
     * Constructor con configuración personalizada
     */
    public GestorSistema(int tamanioMemoria, int tamanioSO, 
                         int tamanioAlmacenamiento, int memoriaVirtual) {
        inicializarSistema(tamanioMemoria, tamanioSO, tamanioAlmacenamiento, memoriaVirtual);
    }
    
    private void inicializarSistema(int tamanioMemoria, int tamanioSO,
                                    int tamanioAlmacenamiento, int memoriaVirtual) {
        // Crear componentes
        this.almacenamiento = new UnidadDeAlmacenamiento(tamanioAlmacenamiento, memoriaVirtual);
        this.memoria = new MemoriaPrincipal(tamanioMemoria, tamanioSO);
        this.planificador = new Planificador();
        this.estadisticas = new ArrayList<>();
        
        // Crear CPUs
        this.cpus = new CPU[NUM_CPUS];
        for (int i = 0; i < NUM_CPUS; i++) {
            cpus[i] = new CPU(i, memoria, planificador, almacenamiento);
        }
        
        log("Sistema inicializado correctamente");
    }
    
    /**
     * Reconfigura el tamaño de la memoria
     */
    public void reconfigurarMemoria(int tamanioTotal, int tamanioSO) {
        if (ejecutando) {
            log("No se puede reconfigurar memoria mientras se ejecuta");
            return;
        }
        
        // Reiniciar sistema con nueva configuración
        int tamanioAlm = almacenamiento.getTamañoTotal();
        int memVirtual = almacenamiento.getMemoriaVirtual();
        inicializarSistema(tamanioTotal, tamanioSO, tamanioAlm, memVirtual);
        
        log("Memoria reconfigurada: Total=" + tamanioTotal + " KB, SO=" + tamanioSO + " KB");
    }
    
    /**
     * Carga programas desde archivos .asm
     */
    public void cargarProgramas(File[] archivos) {
        if (archivos == null || archivos.length == 0) {
            log("No se seleccionaron archivos");
            return;
        }
        
        try {
            // Leer archivos
            String[] nombres = new String[archivos.length];
            List<String>[] programas = new List[archivos.length];
            
            for (int i = 0; i < archivos.length; i++) {
                nombres[i] = archivos[i].getName().replace(".asm", "");
                programas[i] = leerArchivo(archivos[i]);
                
                // Validar sintaxis
                try {
                    InstructionParser.parseAll(programas[i]);
                    log("✓ Archivo validado: " + nombres[i]);
                } catch (Exception e) {
                    log("✗ Error de sintaxis en " + nombres[i] + ": " + e.getMessage());
                    return;
                }
            }
            
            // Cargar en almacenamiento
            almacenamiento.cargarProgramas(nombres, programas);
            log("Programas cargados en disco");
            
            // Crear procesos y cargar en memoria
            for (String nombre : nombres) {
                cargarProcesoEnMemoria(nombre);
            }
            
            // Despachar procesos a CPUs
            planificador.despacharProcesos();
            
            log("Todos los procesos listos para ejecutar");
            
        } catch (Exception e) {
            log("Error cargando programas: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Carga un proceso individual en memoria
     */
    private void cargarProcesoEnMemoria(String nombrePrograma) {
        // Leer programa del disco
        List<String> codigoASM = almacenamiento.leerPrograma(nombrePrograma);
        if (codigoASM == null) {
            throw new RuntimeException("Programa no encontrado: " + nombrePrograma);
        }
        
        // Parsear instrucciones
        List<Instruccion> instrucciones = InstructionParser.parseAll(codigoASM);
        
        // Cargar instrucciones en memoria usuario
        int direccionBase = memoria.cargarProgramaUsuario(instrucciones);
        
        // Crear BCP
        BCP bcp = new BCP(nombrePrograma, direccionBase, instrucciones.size(), 1);
        
        // Cargar BCP en memoria SO
        int direccionBCP = memoria.cargarBCP(bcp);
        
        // Agregar a cola de listos
        planificador.agregarProcesoListo(bcp);
        
        log("Proceso cargado: " + nombrePrograma + " (PID:" + bcp.getPid() + 
            ") Base:" + direccionBase + " Tamaño:" + instrucciones.size());
    }
    
    /**
     * Ejecuta UN ciclo de reloj (1 segundo simulado)
     * Cada CPU con proceso asignado ejecuta una instrucción
     */
    public void ejecutarPasoAPaso() {
        if (todosProcesosFinalizado()) {
            log("Todos los procesos han finalizado");
            mostrarEstadisticas();
            return;
        }
        
        log("=== Ciclo de ejecución ===");
        
        // Intentar despachar procesos a CPUs libres
        planificador.despacharProcesos();
        
        // Cada CPU ejecuta un ciclo
        for (CPU cpu : cpus) {
            cpu.ejecutarCiclo();
        }
        
        // Actualizar GUI si hay callback
        if (actualizarGUICallback != null) {
            actualizarGUICallback.run();
        }
    }
    
    /**
     * Ejecuta automáticamente hasta que todos los procesos terminen
     */
    public void ejecutarAutomatico() {
        if (ejecutando) {
            log("Ya se está ejecutando");
            return;
        }
        
        ejecutando = true;
        
        hiloEjecucion = new Thread(() -> {
            log("Iniciando ejecución automática...");
            
            while (ejecutando && !todosProcesosFinalizado()) {
                ejecutarPasoAPaso();
                
                try {
                    Thread.sleep(1000); // 1 segundo por ciclo
                } catch (InterruptedException e) {
                    break;
                }
            }
            
            ejecutando = false;
            log("Ejecución automática finalizada");
            mostrarEstadisticas();
        });
        
        hiloEjecucion.start();
    }
    
    /**
     * Detiene la ejecución automática
     */
    public void detenerEjecucion() {
        if (ejecutando) {
            ejecutando = false;
            if (hiloEjecucion != null) {
                hiloEjecucion.interrupt();
            }
            log("Ejecución detenida");
        }
    }
    
    /**
     * Procesa entrada de teclado para un proceso en espera
     */
    public void procesarEntradaTeclado(int cpuID, int valor) {
        if (cpuID >= 0 && cpuID < NUM_CPUS) {
            BCP proceso = planificador.getProcesoCPU(cpuID);
            if (proceso != null && proceso.isEsperandoEntrada()) {
                proceso.setDx(valor);
                proceso.setEsperandoEntrada(false);
                planificador.moverEsperaAListos(proceso);
                log("CPU" + cpuID + " - Proceso " + proceso.getPid() + " recibió entrada: " + valor);
            }
        }
    }
    
    /**
     * Limpia todo el sistema
     */
    public void limpiarSistema() {
        detenerEjecucion();
        
        // Reinicializar con valores por defecto
        int tamanioMem = memoria.getTamanioTotal();
        int tamanioSO = memoria.getTamanioSO();
        int tamanioAlm = almacenamiento.getTamañoTotal();
        int memVirtual = almacenamiento.getMemoriaVirtual();
        
        inicializarSistema(tamanioMem, tamanioSO, tamanioAlm, memVirtual);
        BCP.reiniciarContadorPID();
        
        log("Sistema limpiado");
    }
    
    /**
     * Verifica si todos los procesos han finalizado
     */
    private boolean todosProcesosFinalizado() {
        // Verificar CPUs
        for (CPU cpu : cpus) {
            if (planificador.getProcesoCPU(cpu.getCpuID()) != null) {
                return false;
            }
        }
        
        // Verificar colas
        return planificador.getColaListos().isEmpty() && 
               planificador.getColaEspera().isEmpty();
    }
    
    /**
     * Muestra estadísticas finales
     */
    private void mostrarEstadisticas() {
        log("\n========== ESTADÍSTICAS FINALES ==========");
        
        estadisticas.clear();
        
        // Recolectar estadísticas de todos los procesos finalizados
        for (int i = 0; i < NUM_CPUS; i++) {
            BCP proceso = planificador.getProcesoCPU(i);
            if (proceso != null && proceso.getEstado() == Estado.FINALIZADO) {
                EstadisticaProceso est = new EstadisticaProceso(
                    proceso.getNombreArchivo(),
                    proceso.getPid(),
                    proceso.getTiempoInicio(),
                    proceso.getTiempoFinalizacion(),
                    proceso.getTiempoEmpleado(),
                    proceso.getCpuId()
                );
                estadisticas.add(est);
            }
        }
        
        // Mostrar en formato tabla
        if (!estadisticas.isEmpty()) {
            log(EstadisticaProceso.getEncabezadoTabla());
            log(EstadisticaProceso.getLineaSeparadora());
            
            for (EstadisticaProceso est : estadisticas) {
                log(est.toStringTabla());
            }
            
            log(EstadisticaProceso.getLineaSeparadora());
        } else {
            log("No hay procesos finalizados para mostrar estadísticas");
        }
        
        log("==========================================\n");
    }
    
    /**
     * Genera estadísticas de todos los procesos (incluyendo finalizados previamente)
     */
    public List<EstadisticaProceso> generarEstadisticasCompletas() {
        List<EstadisticaProceso> stats = new ArrayList<>();
        
        // Buscar todos los BCPs en memoria (sección SO)
        // Nota: esto es una aproximación, idealmente deberías mantener una lista
        // de todos los procesos creados
        
        return new ArrayList<>(estadisticas);
    }
    
    /**
     * Lee un archivo y devuelve sus líneas
     */
    private List<String> leerArchivo(File archivo) throws IOException {
        List<String> lineas = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (!linea.isEmpty() && !linea.startsWith(";")) {
                    lineas.add(linea);
                }
            }
        }
        return lineas;
    }
    
    private void log(String mensaje) {
        if (consolaCallback != null) {
            consolaCallback.accept(mensaje + "\n");
        }
        System.out.println(mensaje);
    }
    
    // ========== GETTERS PARA LA GUI ==========
    
    public Object[] getContenidoMemoria() {
        return memoria.getMemoria();
    }
    
    public List<String> getContenidoAlmacenamiento() {
        return almacenamiento.getAlmacenamiento();
    }
    
    public BCP[] getProcesosEnEjecucion() {
        return planificador.getProcesosEnEjecucion();
    }
    
    public List<BCP> getColaListos() {
        return new ArrayList<>(planificador.getColaListos());
    }
    
    public List<BCP> getColaEspera() {
        return new ArrayList<>(planificador.getColaEspera());
    }
    
    public BCP getProcesoCPU(int cpuID) {
        return planificador.getProcesoCPU(cpuID);
    }
    
    public List<EstadisticaProceso> getEstadisticas() {
        return new ArrayList<>(estadisticas);
    }
    
    public MemoriaPrincipal getMemoria() {
        return memoria;
    }
    
    public UnidadDeAlmacenamiento getAlmacenamiento() {
        return almacenamiento;
    }
    
    public Planificador getPlanificador() {
        return planificador;
    }
    
    public boolean isEjecutando() {
        return ejecutando;
    }
    
    public CPU[] getCPUs() {
        return cpus;
    }
    
    // ========== SETTERS PARA CALLBACKS ==========
    
    public void setConsolaCallback(Consumer<String> callback) {
        this.consolaCallback = callback;
        // Propagar a todos los CPUs
        for (CPU cpu : cpus) {
            cpu.setConsolaCallback(callback);
        }
    }
    
    public void setPantallaCallback(Consumer<String> callback) {
        this.pantallaCallback = callback;
        // Propagar a todos los CPUs
        for (CPU cpu : cpus) {
            cpu.setPantallaCallback(callback);
        }
    }
    
    public void setActualizarGUICallback(Runnable callback) {
        this.actualizarGUICallback = callback;
    }
}