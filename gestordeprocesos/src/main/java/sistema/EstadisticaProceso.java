/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sistema;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Clase para almacenar estadísticas de ejecución de un proceso
 * @author gadyr
 */
public class EstadisticaProceso {
    
    private String nombreProceso;
    private int pid;
    private long tiempoInicio;      
    private long tiempoFin;         
    private long duracionMs;        
    private int cpuAsignado;
    
    /**
     * Constructor completo
     */
    public EstadisticaProceso(String nombre, int pid, long inicio, long fin, long duracion) {
        this.nombreProceso = nombre;
        this.pid = pid;
        this.tiempoInicio = inicio;
        this.tiempoFin = fin;
        this.duracionMs = duracion;
        this.cpuAsignado = -1;
    }
    
    /**
     * Constructor con CPU asignado
     */
    public EstadisticaProceso(String nombre, int pid, long inicio, long fin, long duracion, int cpu) {
        this(nombre, pid, inicio, fin, duracion);
        this.cpuAsignado = cpu;
    }
    
    /**
     * Obtiene la hora de inicio formateada
     */
    public String getHoraInicioFormateada() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date(tiempoInicio));
    }
    
    /**
     * Obtiene la hora de fin formateada
     */
    public String getHoraFinFormateada() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date(tiempoFin));
    }
    
    /**
     * Obtiene la duracion en segundos
     */
    public double getDuracionSegundos() {
        return duracionMs / 1000.0;
    }
    
    /**
     * Obtiene la duracion 
     */
    public String getDuracionFormateada() {
        long minutos = duracionMs / 60000;
        long segundos = (duracionMs % 60000) / 1000;
        long milisegundos = duracionMs % 1000;
        return String.format("%02d:%02d.%03d", minutos, segundos, milisegundos);
    }
    
    /**
     * Representacion en formato de tabla
     */
    public String toStringTabla() {
        return String.format("%-20s | %4d | %10s | %10s | %12s | CPU%d",
            nombreProceso,
            pid,
            getHoraInicioFormateada(),
            getHoraFinFormateada(),
            getDuracionFormateada(),
            cpuAsignado >= 0 ? cpuAsignado : -1
        );
    }
    
    /**
     * Representacion detallada
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Proceso: ").append(nombreProceso).append(" (PID: ").append(pid).append(")\n");
        sb.append("  Hora inicio: ").append(getHoraInicioFormateada()).append("\n");
        sb.append("  Hora fin: ").append(getHoraFinFormateada()).append("\n");
        sb.append("  Duración: ").append(getDuracionFormateada()).append(" (").append(getDuracionSegundos()).append(" segundos)\n");
        if (cpuAsignado >= 0) {
            sb.append("  CPU asignado: ").append(cpuAsignado).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * Encabezado para tabla de estadisticas
     */
    public static String getEncabezadoTabla() {
        return String.format("%-20s | %4s | %10s | %10s | %12s | %s",
            "Proceso", "PID", "Inicio", "Fin", "Duración", "CPU"
        );
    }
    
    /**
     * Linea separadora para tabla
     */
    public static String getLineaSeparadora() {
        return "-".repeat(80);
    }
    
    //////////setters y getters////////////////////
    
    public String getNombreProceso() {
        return nombreProceso;
    }
    
    public void setNombreProceso(String nombreProceso) {
        this.nombreProceso = nombreProceso;
    }
    
    public int getPid() {
        return pid;
    }
    
    public void setPid(int pid) {
        this.pid = pid;
    }
    
    public long getTiempoInicio() {
        return tiempoInicio;
    }
    
    public void setTiempoInicio(long tiempoInicio) {
        this.tiempoInicio = tiempoInicio;
    }
    
    public long getTiempoFin() {
        return tiempoFin;
    }
    
    public void setTiempoFin(long tiempoFin) {
        this.tiempoFin = tiempoFin;
    }
    
    public long getDuracionMs() {
        return duracionMs;
    }
    
    public void setDuracionMs(long duracionMs) {
        this.duracionMs = duracionMs;
    }
    
    public int getCpuAsignado() {
        return cpuAsignado;
    }
    
    public void setCpuAsignado(int cpuAsignado) {
        this.cpuAsignado = cpuAsignado;
    }
}