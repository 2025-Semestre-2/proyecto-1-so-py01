package sistema;

import almacenamiento.UnidadDeAlmacenamiento;
import cpu.CPU;
import instrucciones.Instruccion;
import instrucciones.InstructionParser;
import memoria.MemoriaPrincipal;
import procesos.BCP;
import procesos.Estado;
import procesos.Planificador;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Gestor principal del sistema operativo simulado
 * Coordina CPU, memoria, almacenamiento y planificador
 * @author gadyr
 */
public class GestorSistema {
    
    private static final int MAX_PROCESOS_CARGADOS = 5;
    
    // Componentes del sistema
    private UnidadDeAlmacenamiento almacenamiento;
    private MemoriaPrincipal memoria;
    private Planificador planificador;
    private CPU cpu; // CPU único que maneja hasta 5 procesos (FCFS)
    
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
        int tamanioMemoria = 512;
        int tamanioSO = 150;
        int tamanioAlmacenamiento = 512;
        int memoriaVirtual = 45;

        inicializarSistema(tamanioMemoria, tamanioSO, tamanioAlmacenamiento, memoriaVirtual);
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
        
        // Crear CPU único
        this.cpu = new CPU(memoria, planificador, almacenamiento, this);
        
        log("Sistema inicializado: 1 CPU con capacidad para " + MAX_PROCESOS_CARGADOS + " procesos (FCFS)");
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
//                try {
//                    InstructionParser.parseAll(programas[i]);
//                    log("✓ Archivo validado: " + nombres[i]);
//                } catch (Exception e) {
//                    log("✗ Error de sintaxis en " + nombres[i] + ": " + e.getMessage());
//                    return;
//                }
            }
            
            // Cargar en almacenamiento
            almacenamiento.cargarProgramas(nombres, programas);
            log("Programas cargados en disco");
            
            // Solo crear BCPs, NO cargar en memoria todavía
            for (String nombre : nombres) {
                crearBCPSinCargarMemoria(nombre);
            }
            
            // Despachar procesos
//            planificador.despacharProcesos();
            
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
     * Crea un BCP sin cargar el programa en memoria principal
     * El programa permanece en disco hasta que se ejecute
     */
    private void crearBCPSinCargarMemoria(String nombrePrograma) {
        // Leer programa del disco solo para obtener el tamaño
        List<String> codigoASM = almacenamiento.leerPrograma(nombrePrograma);
        if (codigoASM == null) {
            throw new RuntimeException("Programa no encontrado: " + nombrePrograma);
        }

        // NO parsear aquí - solo contar líneas
        int tamanio = codigoASM.size();

        // Crear BCP sin dirección base todavía (se asignará al cargar en memoria)
        BCP bcp = new BCP(nombrePrograma, -1, tamanio, 1);

        // Agregar a cola de listos
        planificador.agregarProcesoListo(bcp);

        log("Proceso creado: " + nombrePrograma + " (PID:" + bcp.getPid() + 
            ") - En disco, no cargado en memoria");
    }
    
    
    /**
     * Ejecuta UN ciclo de reloj (1 segundo simulado)
     * El CPU ejecuta el primer proceso (FCFS)
     */
    public void ejecutarPasoAPaso() {
        if (todosProcesosFinalizado()) {
            log("Todos los procesos han finalizado");
            mostrarEstadisticas();
            return;
        }
        
        log("=== Ciclo de ejecución (FCFS) ===");
        
        // Intentar despachar procesos a slots libres
        planificador.despacharProcesos();
        
        // El CPU ejecuta SOLO el primer proceso (FCFS)
        cpu.ejecutarCiclo();
        
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
            log("Iniciando/Reanudando ejecución automática...");

            while (ejecutando && !todosProcesosFinalizado()) {
                ejecutarPasoAPaso();

                // Verificar si hay procesos esperando entrada
                if (hayProcesoEsperandoEntrada()) {
                    log("Ejecución pausada: esperando entrada de usuario");
                    ejecutando = false;

                    // Notificar a la GUI para que verifique entrada pendiente
                    if (actualizarGUICallback != null) {
                        actualizarGUICallback.run();
                    }
                    break;
                }

                try {
                    Thread.sleep(1000); // 1 segundo por ciclo
                } catch (InterruptedException e) {
                    break;
                }
            }

            ejecutando = false;
            if (todosProcesosFinalizado()) {
                log("Ejecución automática finalizada - todos los procesos terminaron");
                mostrarEstadisticas();
            }
        });

        hiloEjecucion.start();
    }
    
    
    /**
    * Verifica si hay algún proceso esperando entrada de teclado
    */
    private boolean hayProcesoEsperandoEntrada() {
        BCP[] procesos = planificador.getProcesosEnEjecucion();
        for (BCP proceso : procesos) {
            if (proceso != null && proceso.isEsperandoEntrada()) {
                return true;
            }
        }
        return false;
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
    public void procesarEntradaTeclado(int cpuSlot, int valor) {
        cpu.procesarEntradaTeclado(cpuSlot, valor);
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
        // Verificar slots del CPU
        BCP[] procesosEnEjecucion = planificador.getProcesosEnEjecucion();
        for (BCP proceso : procesosEnEjecucion) {
            if (proceso != null) {
                return false;
            }
        }
        
        // Verificar colas
        return planificador.getColaListos().isEmpty() && 
               planificador.getColaEspera().isEmpty();
    }
    
    /**
    * Verifica si la ejecución se detuvo por espera de entrada
    */
    public boolean seDetuvoPorEntrada() {
        return !ejecutando && hayProcesoEsperandoEntrada();
    }
    
    
    /**
     * Muestra estadísticas finales
     */
    private void mostrarEstadisticas() {
        log("\n========== ESTADÍSTICAS FINALES ==========");

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
    * Registra estadística de un proceso finalizado
    */
    public void registrarEstadisticaProceso(BCP proceso) {
        if (proceso.getEstado() == Estado.FINALIZADO) {
            EstadisticaProceso est = new EstadisticaProceso(
                proceso.getNombreArchivo(),
                proceso.getPid(),
                proceso.getTiempoInicio(),
                proceso.getTiempoFinalizacion(),
                proceso.getTiempoEmpleado(),
                proceso.getCpuId()
            );
            estadisticas.add(est);
            log("Estadística registrada: " + proceso.getNombreArchivo() + " (PID:" + proceso.getPid() + ")");
        }
    }
    
    /**
     * Genera estadísticas de todos los procesos (incluyendo finalizados previamente)
     */
    public List<EstadisticaProceso> generarEstadisticasCompletas() {
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
    
    public BCP getProcesoCPU(int slot) {
        return planificador.getProcesoCPU(slot);
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
    
    public CPU getCPU() {
        return cpu;
    }
    

    
    // ========== SETTERS PARA CALLBACKS ==========
    
    public void setConsolaCallback(Consumer<String> callback) {
        this.consolaCallback = callback;
        // Propagar al CPU
        cpu.setConsolaCallback(callback);
    }
    
    public void setPantallaCallback(Consumer<String> callback) {
        this.pantallaCallback = callback;
        // Propagar al CPU
        cpu.setPantallaCallback(callback);
    }
    
    public void setActualizarGUICallback(Runnable callback) {
        this.actualizarGUICallback = callback;
    }
}