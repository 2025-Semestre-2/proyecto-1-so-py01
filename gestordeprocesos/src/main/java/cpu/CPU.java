/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cpu;

import almacenamiento.UnidadDeAlmacenamiento;
import instrucciones.Instruccion;
import instrucciones.Opcode;
import memoria.MemoriaPrincipal;
import procesos.BCP;
import procesos.Estado;
import procesos.Planificador;
import java.util.List;
import java.util.function.Consumer;

/**
 * CPU que ejecuta instrucciones de los procesos
 * @author gadyr
 */
public class CPU {
    
    private final int cpuID;
    private final MemoriaPrincipal memoria;
    private final Planificador planificador;
    private final UnidadDeAlmacenamiento almacenamiento;
    
    private Consumer<String> consolaCallback;
    private Consumer<String> pantallaCallback;
    
    private boolean esperandoEntrada = false;
    private int valorEntrada = 0;
    
    private boolean flagIgualdad = false;
    
    public CPU(int cpuID, MemoriaPrincipal memoria, Planificador planificador, 
               UnidadDeAlmacenamiento almacenamiento) {
        this.cpuID = cpuID;
        this.memoria = memoria;
        this.planificador = planificador;
        this.almacenamiento = almacenamiento;
    }
    
    /**
     * Ejecuta un ciclo de instrucción para el proceso asignado a este CPU
     * @return true si ejecutó algo, false si el CPU está libre o proceso terminado
     */
    public boolean ejecutarCiclo() {
        BCP proceso = planificador.getProcesoCPU(cpuID);
        
        if (proceso == null) {
            return false; // CPU libre
        }
        
        // Si está esperando entrada, no ejecutar
        if (proceso.isEsperandoEntrada()) {
            return false;
        }
        
        try {
            // Fetch: obtener instrucción
            int pc = proceso.getProgramCounter();
            Instruccion instruccion = memoria.leerInstruccionUsuario(pc);
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
            planificador.finalizarProceso(cpuID);
            return false;
        }
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
    
    ////////////Instrucciones////////////////
    
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
                planificador.finalizarProceso(cpuID);
                bcp.cambiarEstado(Estado.FINALIZADO);
                log("Proceso " + bcp.getPid() + " finalizado");
                break;
                
            case "10H":
                // Imprimir en pantalla el valor de DX
                int valorDX = bcp.getDx();
                imprimirPantalla(String.valueOf(valorDX));
                break;
                
            case "09H":
                // Entrada de teclado (guardar en DX)
                bcp.setEsperandoEntrada(true);
                planificador.liberarCPU(cpuID);
                log("Proceso " + bcp.getPid() + " esperando entrada de teclado");
                break;
                
            case "21H":
                // opcional no implentada aun
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
            bcp.pushPila(valor);  // <-- Cambio aquí
        }
    }
    
    private void ejecutarPUSH(BCP bcp, String reg) {
        int valor = leerRegistro(bcp, reg);
        bcp.pushPila(valor);  // <-- Cambio aquí
    }
    
    private void ejecutarPOP(BCP bcp, String reg) {
        int valor = bcp.popPila();  // <-- Cambio aquí
        escribirRegistro(bcp, reg, valor);
    }
    
    private void ejecutarManejoArchivos(BCP bcp) {
        // OPCIONAL: implementar si quieren los 5 puntos extra
        log("Manejo de archivos no implementado (INT 21H)");
    }
    
    
    
    //////////////////// leer, escribir//////////////////////    
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
            consolaCallback.accept("[CPU" + cpuID + "] " + mensaje);
        }
        System.out.println("[CPU" + cpuID + "] " + mensaje);
    }
    
    private void imprimirPantalla(String texto) {
        if (pantallaCallback != null) {
            pantallaCallback.accept(texto + "\n");
        }
    }
    
    /**
     * Procesa la entrada del teclado para un proceso en espera
     */
    public void procesarEntradaTeclado(int valor) {
        BCP proceso = planificador.getProcesoCPU(cpuID);
        if (proceso != null && proceso.isEsperandoEntrada()) {
            proceso.setDx(valor);
            proceso.setEsperandoEntrada(false);
            planificador.moverEsperaAListos(proceso);
            log("Proceso " + proceso.getPid() + " recibió entrada: " + valor);
        }
    }
    
    // setters de callBacks
    
    public void setConsolaCallback(Consumer<String> callback) {
        this.consolaCallback = callback;
    }
    
    public void setPantallaCallback(Consumer<String> callback) {
        this.pantallaCallback = callback;
    }
    
    public int getCpuID() {
        return cpuID;
    }
}
