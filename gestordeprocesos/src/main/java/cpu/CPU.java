/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cpu;

import almacenamiento.UnidadDeAlmacenamiento;
import instrucciones.IR;
import instrucciones.Instruccion;
import instrucciones.InstructionParser;
import instrucciones.Opcode;
import memoria.MemoriaPrincipal;
import procesos.BCP;
import procesos.Estado;
import procesos.Planificador;
import java.util.List;
import java.util.function.Consumer;
import sistema.GestorSistema;

/**
 * CPU único que ejecuta instrucciones de los procesos usando FCFS
 * Puede mantener hasta 5 BCPs pero ejecuta uno a la vez
 * @author gadyr
 */
public class CPU {
    
    private static final int MAX_PROCESOS_SIMULTANEOS = 5;
    
    private final MemoriaPrincipal memoria;
    private final Planificador planificador;
    private final UnidadDeAlmacenamiento almacenamiento;
    
    // 5 Registros IR, uno por cada proceso que puede estar cargado
    private IR[] registrosIR;
    
    // Callbacks para actualizar la interfaz
    private Consumer<String> consolaCallback;
    private Consumer<String> pantallaCallback;
    
    // Bandera de comparación (para JE/JNE)
    private boolean flagIgualdad = false;
    private GestorSistema gestorReferencia;
    
    public CPU(MemoriaPrincipal memoria, Planificador planificador, 
               UnidadDeAlmacenamiento almacenamiento, GestorSistema gestor) {
        this.memoria = memoria;
        this.planificador = planificador;
        this.almacenamiento = almacenamiento;
        this.gestorReferencia = gestor;
        this.registrosIR = new IR[MAX_PROCESOS_SIMULTANEOS];
    }
    
    /**
     * Ejecuta un ciclo de instrucción siguiendo FCFS
     * Solo ejecuta el primer proceso en la cola de listos
     * @return true si ejecutó algo, false si no hay procesos
     */
    public boolean ejecutarCiclo() {
        // Obtener el primer proceso en ejecución (FCFS)
        BCP proceso = obtenerProcesoActual();
        
        if (proceso == null) {
            return false; // No hay proceso para ejecutar
        }
        
        
        // Si el proceso no está cargado en memoria, cargarlo ahora
        if (proceso.getDireccionBase() == -1) {
            log("Cargando proceso " + proceso.getPid() + " del disco a memoria...");
            cargarProcesoEnMemoria(proceso);

            // Verificar si el proceso fue marcado como finalizado (error de sintaxis)
            if (proceso.getEstado() == Estado.FINALIZADO) {
                log("Proceso " + proceso.getPid() + " no se pudo cargar, pasando al siguiente");
                return false; // No ejecutar este proceso
            }
        }
        
        // Si está esperando entrada, no ejecutar
        if (proceso.isEsperandoEntrada()) {
            return false;
        }
        
        try {
            // Fetch: obtener instrucción
            int pc = proceso.getProgramCounter();
            Instruccion instruccion = memoria.leerInstruccionUsuario(pc);
            
            // Cargar en IR correspondiente
            int slot = obtenerSlotProceso(proceso);
            if (slot >= 0) {
                registrosIR[slot] = new IR(instruccion);
            }
            
            proceso.setInstruccionActual(instruccion.toString());
            
            // Decode & Execute: ejecutar instrucción
            ejecutarInstruccion(proceso, instruccion);
            
            // Incrementar PC (excepto si fue un salto)
            if (!esSalto(instruccion.getOpcode())) {
                proceso.incrementarPC();
            }
            
            return true;
            
        } catch (Exception e) {
            // Error en ejecución
            log("Error ejecutando proceso " + proceso.getPid() + ": " + e.getMessage());
            planificador.finalizarProceso(proceso.getCpuId());
            return false;
        }
    }
    
    
    /**
    * Carga un proceso del disco a la memoria principal
    */
    private void cargarProcesoEnMemoria(BCP proceso) {
        try {
            // Leer programa del disco
            List<String> codigoASM = almacenamiento.leerPrograma(proceso.getNombreArchivo());
            if (codigoASM == null) {
                throw new RuntimeException("Programa no encontrado en disco: " + proceso.getNombreArchivo());
            }

            // VALIDAR sintaxis ANTES de cargar
            List<Instruccion> instrucciones;
            try {
                instrucciones = InstructionParser.parseAll(codigoASM);
            } catch (Exception e) {
                // Error de sintaxis - reportar y NO cargar
                log("ERROR: Proceso " + proceso.getPid() + " (" + proceso.getNombreArchivo() + 
                    ") tiene error de sintaxis: " + e.getMessage());
                imprimirPantalla("✗ ERROR: Proceso " + proceso.getPid() + " (" + 
                               proceso.getNombreArchivo() + ") no se puede ejecutar.\n" +
                               "  Motivo: " + e.getMessage() + "\n");

                // Marcar proceso como finalizado con error
                proceso.cambiarEstado(Estado.FINALIZADO);
                planificador.finalizarProceso(proceso.getCpuId());
                return;
            }

            // Si pasa la validación, cargar en memoria
            int direccionBase = memoria.cargarProgramaUsuario(instrucciones);

            // Actualizar BCP con dirección base y PC
            proceso.setDireccionBase(direccionBase);
            proceso.setProgramCounter(direccionBase);

            // Cargar BCP en memoria SO
            int direccionBCP = memoria.cargarBCP(proceso);

            // Configurar el BCP para que actualice memoria automáticamente
            proceso.setDireccionBCPEnMemoria(direccionBCP);
            proceso.setMemoriaReferencia(memoria);
            log("Proceso " + proceso.getPid() + " cargado en memoria: Base=" + direccionBase + ", BCP@" + direccionBCP);

        } catch (Exception e) {
            log("ERROR al cargar proceso en memoria: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error cargando proceso en memoria", e);
        }
    }
    
    
    /**
     * Obtiene el proceso actual en ejecución (primer proceso de los 5 slots)
     */
    private BCP obtenerProcesoActual() {
        BCP[] procesos = planificador.getProcesosEnEjecucion();
        for (BCP proceso : procesos) {
            if (proceso != null && proceso.getEstado() == Estado.EJECUCION) {
                return proceso;
            }
        }
        return null;
    }
    
    /**
     * Obtiene el slot (0-4) donde está cargado un proceso
     */
    private int obtenerSlotProceso(BCP proceso) {
        BCP[] procesos = planificador.getProcesosEnEjecucion();
        for (int i = 0; i < procesos.length; i++) {
            if (procesos[i] != null && procesos[i].getPid() == proceso.getPid()) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Ejecuta una instrucción específica
     */
    private void ejecutarInstruccion(BCP bcp, Instruccion instr) {
        Opcode op = instr.getOpcode();
        List<String> operandos = instr.getOperandos();
        
        switch (op) {
            case LOAD:
                ejecutarLOAD(bcp, operandos.get(0));
                break;
            case STORE:
                ejecutarSTORE(bcp, operandos.get(0));
                break;
            case MOV:
                ejecutarMOV(bcp, operandos.get(0), operandos.get(1));
                break;
            case ADD:
                ejecutarADD(bcp, operandos.get(0));
                break;
            case SUB:
                ejecutarSUB(bcp, operandos.get(0));
                break;
            case INC:
                ejecutarINC(bcp, operandos.isEmpty() ? "AC" : operandos.get(0));
                break;
            case DEC:
                ejecutarDEC(bcp, operandos.isEmpty() ? "AC" : operandos.get(0));
                break;
            case SWAP:
                ejecutarSWAP(bcp, operandos.get(0), operandos.get(1));
                break;
            case INT:
                ejecutarINT(bcp, operandos.get(0));
                break;
            case JMP:
                ejecutarJMP(bcp, operandos.get(0));
                break;
            case CMP:
                ejecutarCMP(bcp, operandos.get(0), operandos.get(1));
                break;
            case JE:
                ejecutarJE(bcp, operandos.get(0));
                break;
            case JNE:
                ejecutarJNE(bcp, operandos.get(0));
                break;
            case PARAM:
                ejecutarPARAM(bcp, operandos);
                break;
            case PUSH:
                ejecutarPUSH(bcp, operandos.get(0));
                break;
            case POP:
                ejecutarPOP(bcp, operandos.get(0));
                break;
            default:
                throw new RuntimeException("Instrucción no implementada: " + op);
        }
    }
    
    // ========== IMPLEMENTACIÓN DE INSTRUCCIONES ==========
    
    private void ejecutarLOAD(BCP bcp, String reg) {
        int valor = leerRegistro(bcp, reg);
        bcp.setAc(valor);
    }
    
    private void ejecutarSTORE(BCP bcp, String reg) {
        escribirRegistro(bcp, reg, bcp.getAc());
    }
    
    private void ejecutarMOV(BCP bcp, String destino, String origen) {
        int valor;
        if (esNumero(origen)) {
            valor = Integer.parseInt(origen);
        } else {
            valor = leerRegistro(bcp, origen);
        }
        escribirRegistro(bcp, destino, valor);
    }
    
    private void ejecutarADD(BCP bcp, String reg) {
        int valor = leerRegistro(bcp, reg);
        bcp.setAc(bcp.getAc() + valor);
    }
    
    private void ejecutarSUB(BCP bcp, String reg) {
        int valor = leerRegistro(bcp, reg);
        bcp.setAc(bcp.getAc() - valor);
    }
    
    private void ejecutarINC(BCP bcp, String reg) {
        int valor = leerRegistro(bcp, reg);
        escribirRegistro(bcp, reg, valor + 1);
    }
    
    private void ejecutarDEC(BCP bcp, String reg) {
        int valor = leerRegistro(bcp, reg);
        escribirRegistro(bcp, reg, valor - 1);
    }
    
    private void ejecutarSWAP(BCP bcp, String reg1, String reg2) {
        int valor1 = leerRegistro(bcp, reg1);
        int valor2 = leerRegistro(bcp, reg2);
        escribirRegistro(bcp, reg1, valor2);
        escribirRegistro(bcp, reg2, valor1);
    }
    
    private void ejecutarINT(BCP bcp, String codigo) {
        switch (codigo.toUpperCase()) {
            case "20H":
                // Finalizar programa
                bcp.cambiarEstado(Estado.FINALIZADO);
                planificador.finalizarProceso(bcp.getCpuId());
                log("Proceso " + bcp.getPid() + " finalizado");

                // Registrar estadística
                if (gestorReferencia != null) {
                    gestorReferencia.registrarEstadisticaProceso(bcp);
                }
                break;
                
            case "10H":
                // Imprimir en pantalla el valor de DX
                int valorDX = bcp.getDx();
                imprimirPantalla(String.valueOf(valorDX));
                break;
                
            case "09H":
                // Entrada de teclado (guardar en DX)
                bcp.setEsperandoEntrada(true);
                planificador.agregarProcesoEspera(bcp);
//                planificador.liberarCPU(bcp.getCpuId());
                log("Proceso " + bcp.getPid() + " esperando entrada de teclado");
                break;
                
            case "21H":
                // Manejo de archivos (OPCIONAL)
                ejecutarManejoArchivos(bcp);
                break;
                
            default:
                throw new RuntimeException("Código de interrupción no reconocido: " + codigo);
        }
    }
    
    private void ejecutarJMP(BCP bcp, String desplazamiento) {
        int desp = Integer.parseInt(desplazamiento);
        int nuevoPC = bcp.getProgramCounter() + desp;
        
        if (!bcp.direccionValida(nuevoPC)) {
            throw new RuntimeException("Salto fuera de rango: " + nuevoPC);
        }
        
        bcp.setProgramCounter(nuevoPC);
    }
    
    private void ejecutarCMP(BCP bcp, String reg1, String reg2) {
        int valor1 = leerRegistro(bcp, reg1);
        int valor2 = leerRegistro(bcp, reg2);
        flagIgualdad = (valor1 == valor2);
    }
    
    private void ejecutarJE(BCP bcp, String desplazamiento) {
        if (flagIgualdad) {
            ejecutarJMP(bcp, desplazamiento);
        }
    }
    
    private void ejecutarJNE(BCP bcp, String desplazamiento) {
        if (!flagIgualdad) {
            ejecutarJMP(bcp, desplazamiento);
        }
    }
    
    private void ejecutarPARAM(BCP bcp, List<String> valores) {
        for (int i = valores.size() - 1; i >= 0; i--) {
            int valor = Integer.parseInt(valores.get(i));
            bcp.pushPila(valor);
        }
    }
    
    private void ejecutarPUSH(BCP bcp, String reg) {
        int valor = leerRegistro(bcp, reg);
        bcp.pushPila(valor);
    }
    
    private void ejecutarPOP(BCP bcp, String reg) {
        int valor = bcp.popPila();
        escribirRegistro(bcp, reg, valor);
    }
    
    private void ejecutarManejoArchivos(BCP bcp) {
        log("Manejo de archivos no implementado (INT 21H)");
    }
    
    // ========== UTILIDADES ==========
    
    private int leerRegistro(BCP bcp, String reg) {
        switch (reg.toUpperCase()) {
            case "AC": return bcp.getAc();
            case "AX": return bcp.getAx();
            case "BX": return bcp.getBx();
            case "CX": return bcp.getCx();
            case "DX": return bcp.getDx();
            default: throw new RuntimeException("Registro inválido: " + reg);
        }
    }
    
    private void escribirRegistro(BCP bcp, String reg, int valor) {
        switch (reg.toUpperCase()) {
            case "AC": bcp.setAc(valor); break;
            case "AX": bcp.setAx(valor); break;
            case "BX": bcp.setBx(valor); break;
            case "CX": bcp.setCx(valor); break;
            case "DX": bcp.setDx(valor); break;
            default: throw new RuntimeException("Registro inválido: " + reg);
        }
    }
    
    private boolean esNumero(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private boolean esSalto(Opcode op) {
        return op == Opcode.JMP || op == Opcode.JE || op == Opcode.JNE;
    }
    
    private void log(String mensaje) {
        if (consolaCallback != null) {
            consolaCallback.accept("[CPU] " + mensaje);
        }
        System.out.println("[CPU] " + mensaje);
    }
    
    private void imprimirPantalla(String texto) {
        if (pantallaCallback != null) {
            pantallaCallback.accept(texto + "\n");
        }
    }
    
    /**
     * Procesa la entrada del teclado para el proceso en espera
     */
    public void procesarEntradaTeclado(int cpuSlot, int valor) {
        // Buscar primero en el slot
        BCP proceso = planificador.getProcesoCPU(cpuSlot);

        // Si no está en el slot, buscar en cola de espera
        if (proceso == null) {
            for (BCP p : planificador.getColaEspera()) {
                if (p != null && p.isEsperandoEntrada() && p.getCpuId() == cpuSlot) {
                    proceso = p;
                    break;
                }
            }
        }

        if (proceso != null && proceso.isEsperandoEntrada()) {
            proceso.setDx(valor);
            proceso.setEsperandoEntrada(false);
            proceso.cambiarEstado(Estado.EJECUCION);
//            planificador.moverEsperaAListos(proceso);
            log("Proceso " + proceso.getPid() + " recibió entrada: " + valor);
        } else {
            log("ERROR: No se encontró proceso esperando entrada en slot " + cpuSlot);
        }
    }
    
    /**
     * Obtiene el IR de un slot específico
     */
    public IR getIR(int slot) {
        if (slot >= 0 && slot < MAX_PROCESOS_SIMULTANEOS) {
            return registrosIR[slot];
        }
        return null;
    }
    
    // ========== SETTERS PARA CALLBACKS ==========
    
    public void setConsolaCallback(Consumer<String> callback) {
        this.consolaCallback = callback;
    }
    
    public void setPantallaCallback(Consumer<String> callback) {
        this.pantallaCallback = callback;
    }
}