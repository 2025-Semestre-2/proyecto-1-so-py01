/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package view;

import java.io.File;
import java.util.List;
import procesos.BCP;
import sistema.EstadisticaProceso;
import sistema.GestorSistema;
import procesos.Estado;
import java.util.Set;
import java.util.HashSet;

/**
 *
 * @author gadyr
 */
public class App extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(App.class.getName());
    
    private GestorSistema gestor;
    private javax.swing.Timer timerActualizacion;

    /**
     * Creates new form App
     */
    public App() {
        initComponents();
        inicializarSistema();
        
    }
    
    /**
     * Inicializa el sistema
     */
    private void inicializarSistema(){
        
        gestor = new GestorSistema();
        
        gestor.setConsolaCallback(mensaje -> {
            System.out.print(mensaje);
        });

        gestor.setPantallaCallback(texto -> {
            pantalla.append(texto);
            pantalla.setCaretPosition(pantalla.getDocument().getLength());
        });
        
        
        if (inputTeclado != null) {
            inputTeclado.setEnabled(false);
            inputTeclado.addActionListener(evt -> enviarEntrada());
        }

        if (Enviar != null) {
            Enviar.setEnabled(false); // Deshabilitado hasta que se necesite
        }
    
        gestor.setActualizarGUICallback(() -> {
            actualizarTablas();
        });


        
        actualizarTablas();
        
        
        
    }
    
   /**
    * Actualiza todas las tablas de la GUI con datos del gestor
    */
   private void actualizarTablas() {
       actualizarTablaMemoria();
       actualizarTablaDisco();
       actualizarTablaProcesos();
       actualizarBCP();
   }

   /**
    * Actualiza la tabla de memoria principal
    */
   private void actualizarTablaMemoria() { javax.swing.table.DefaultTableModel modelo = (javax.swing.table.DefaultTableModel) memoryTable1.getModel();

       // Limpiar tabla
       modelo.setRowCount(0);

       // Obtener memoria del gestor
       Object[] memoria = gestor.getContenidoMemoria();

       for (int i = 0; i < memoria.length; i++) {
           Object celda = memoria[i];
           String valor = (celda != null) ? celda.toString() : "NULL";
           modelo.addRow(new Object[]{i, valor});
       }
   }

   /**
    * Actualiza la tabla de disco (almacenamiento)
    */
   private void actualizarTablaDisco() { javax.swing.table.DefaultTableModel modelo = (javax.swing.table.DefaultTableModel) discTable.getModel();

       // Limpiar tabla
       modelo.setRowCount(0);

       // Obtener almacenamiento
       List<String> disco = gestor.getContenidoAlmacenamiento();

       for (int i = 0; i < disco.size(); i++) {
           String celda = disco.get(i);
           String valor = (celda != null) ? celda : "0";
           modelo.addRow(new Object[]{i, valor});
       }
   }

   /**
    * Actualiza la tabla de estados de procesos
    */
    private void actualizarTablaProcesos() {
        javax.swing.table.DefaultTableModel modelo = 
            (javax.swing.table.DefaultTableModel) ProcessStatesTable1.getModel();

        // Limpiar tabla
        modelo.setRowCount(0);

        // Lista para evitar duplicados
        java.util.Set<Integer> pidsYaMostrados = new java.util.HashSet<>();

        // Procesos en slots (solo si NO están en cola de espera)
        BCP[] enEjecucion = gestor.getProcesosEnEjecucion();
        for (int i = 0; i < enEjecucion.length; i++) {
            if (enEjecucion[i] != null) {
                BCP bcp = enEjecucion[i];

                // Solo mostrar si NO está en estado ESPERA (esos se muestran después)
                if (bcp.getEstado() != procesos.Estado.ESPERA) {
                    String estado = bcp.getEstado() == procesos.Estado.EJECUCION ? 
                        "EJECUCION (CPU)" : "CARGADO - Slot " + i;
                    modelo.addRow(new Object[]{
                        bcp.getNombreArchivo() + " (PID:" + bcp.getPid() + ")",
                        estado
                    });
                    pidsYaMostrados.add(bcp.getPid());
                }
            }
        }

        // Cola de listos
        List<BCP> listos = gestor.getColaListos();
        for (BCP bcp : listos) {
            if (!pidsYaMostrados.contains(bcp.getPid())) {
                modelo.addRow(new Object[]{
                    bcp.getNombreArchivo() + " (PID:" + bcp.getPid() + ")",
                    "PREPARADO"
                });
                pidsYaMostrados.add(bcp.getPid());
            }
        }

        // Cola de espera
        List<BCP> espera = gestor.getColaEspera();
        for (BCP bcp : espera) {
            if (!pidsYaMostrados.contains(bcp.getPid())) {
                modelo.addRow(new Object[]{
                    bcp.getNombreArchivo() + " (PID:" + bcp.getPid() + ")",
                    "ESPERA (E/S)"
                });
                pidsYaMostrados.add(bcp.getPid());
            }
        }
    }

   /**
    * Actualiza el área de texto del BCP actual
    */
    private void actualizarBCP() {
        StringBuilder sb = new StringBuilder();
        sb.append("═══ ESTADO DEL CPU (FCFS) ═══\n\n");

        BCP[] slots = gestor.getProcesosEnEjecucion();
        boolean hayProcesos = false;
        BCP procesoActual = null;

        // Encontrar el proceso en ejecución
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null) {
                hayProcesos = true;
                BCP bcp = slots[i];

                if (bcp.getEstado() == procesos.Estado.EJECUCION) {
                    procesoActual = bcp;
                    sb.append(">>> EJECUTANDO AHORA <<<\n");
                    sb.append("─────────────────────────\n");
                    sb.append("Slot: ").append(i).append("\n");
                    sb.append("Proceso: ").append(bcp.getNombreArchivo()).append("\n");
                    sb.append("PID: ").append(bcp.getPid()).append("\n");
                    sb.append("Estado: ").append(bcp.getEstado()).append("\n");
                    sb.append("PC: ").append(bcp.getProgramCounter()).append("\n");
                    sb.append("Instrucción: ").append(bcp.getInstruccionActual()).append("\n\n");

                    sb.append("REGISTROS:\n");
                    sb.append("  AC = ").append(bcp.getAc()).append("\n");
                    sb.append("  AX = ").append(bcp.getAx()).append("\n");
                    sb.append("  BX = ").append(bcp.getBx()).append("\n");
                    sb.append("  CX = ").append(bcp.getCx()).append("\n");
                    sb.append("  DX = ").append(bcp.getDx()).append("\n\n");

//                    sb.append("PILA: ").append(bcp.getPila()).append("\n");
//                    sb.append("Tamaño: ").append(bcp.getPila().size()).append("/5\n\n");
//
//                    sb.append("MEMORIA:\n");
//                    sb.append("  Base: ").append(bcp.getDireccionBase()).append("\n");
//                    sb.append("  Tamaño: ").append(bcp.getTamanio()).append("\n");
//                    sb.append("  Prioridad: ").append(bcp.getPrioridad()).append("\n\n");
                }
            }
        }

        // Mostrar otros procesos cargados
        if (hayProcesos) {
            sb.append("─── Otros procesos en memoria ───\n");
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] != null && slots[i] != procesoActual) {
                    BCP bcp = slots[i];
                    sb.append("Slot ").append(i).append(": ")
                      .append(bcp.getNombreArchivo())
                      .append(" (PID:").append(bcp.getPid()).append(")")
                      .append(" - ").append(bcp.getEstado()).append("\n");
                }
            }
        }

        if (!hayProcesos) {
            sb.append("No hay procesos cargados\n");
        }

        BCP.setText(sb.toString());
    }
   
   /**
    * Muestra una ventana con las estadísticas en formato tabla
    */
   private void mostrarVentanaEstadisticas(List<EstadisticaProceso> estadisticas) {
        // Crear diálogo
        javax.swing.JDialog dialogoEstadisticas = new javax.swing.JDialog(this, "Estadísticas de Ejecución", true);
        dialogoEstadisticas.setSize(700, 400);
        dialogoEstadisticas.setLocationRelativeTo(this);

        // Crear tabla
        String[] columnas = {"Proceso", "PID", "Hora Inicio", "Hora Fin", "Duración", "CPU"};
        Object[][] datos = new Object[estadisticas.size()][6];

        for (int i = 0; i < estadisticas.size(); i++) {
            EstadisticaProceso est = estadisticas.get(i);
            datos[i][0] = est.getNombreProceso();
            datos[i][1] = est.getPid();
            datos[i][2] = est.getHoraInicioFormateada();
            datos[i][3] = est.getHoraFinFormateada();
            datos[i][4] = est.getDuracionFormateada();
            datos[i][5] = est.getCpuAsignado() >= 0 ? "CPU" + est.getCpuAsignado() : "N/A";
        }

        javax.swing.JTable tablaEstadisticas = new javax.swing.JTable(datos, columnas);
        tablaEstadisticas.setEnabled(false); // Solo lectura

        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(tablaEstadisticas);

        // Panel con texto adicional
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout());

        javax.swing.JLabel titulo = new javax.swing.JLabel(
            "Estadísticas de " + estadisticas.size() + " proceso(s) finalizado(s)",
            javax.swing.SwingConstants.CENTER
        );
        titulo.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        titulo.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(titulo, java.awt.BorderLayout.NORTH);
        panel.add(scrollPane, java.awt.BorderLayout.CENTER);

        javax.swing.JButton btnCerrar = new javax.swing.JButton("Cerrar");
        btnCerrar.addActionListener(e -> dialogoEstadisticas.dispose());

        javax.swing.JPanel panelBoton = new javax.swing.JPanel();
        panelBoton.add(btnCerrar);
        panel.add(panelBoton, java.awt.BorderLayout.SOUTH);

        dialogoEstadisticas.add(panel);
        dialogoEstadisticas.setVisible(true);
    }

   
   /**
    * Verifica si hay procesos esperando entrada y solicita automáticamente
    */
    private void verificarEntradaPendiente() {
        System.out.println(" pendiente");

        // Buscar en procesos cargados en slots
        BCP[] procesos = gestor.getProcesosEnEjecucion();
        for (int cpuID = 0; cpuID < procesos.length; cpuID++) {
            BCP proceso = procesos[cpuID];
            if (proceso != null && proceso.isEsperandoEntrada()) {
                System.out.println("Encontrado en slot " + cpuID);
                mostrarPromptEntrada(cpuID, proceso);
                return;
            }
        }

        // Buscar en cola de espera
        List<BCP> colaEspera = gestor.getColaEspera();
        for (BCP proceso : colaEspera) {
            if (proceso != null && proceso.isEsperandoEntrada()) {
                int cpuID = proceso.getCpuId();
                System.out.println("en cola de espera, CPU: " + cpuID);
                mostrarPromptEntrada(cpuID, proceso);
                return;
            }
        }

    }
    
    
    private void mostrarPromptEntrada(int cpuID, BCP proceso) {

        String prompt = String.format(
            "\n[CPU%d] Proceso %s (PID:%d) requiere entrada (0-255): ",
            cpuID, proceso.getNombreArchivo(), proceso.getPid()
        );
        pantalla.append(prompt);
        pantalla.setCaretPosition(pantalla.getDocument().getLength());

        if (inputTeclado != null) {
            inputTeclado.setEnabled(true);
            inputTeclado.requestFocus();
            inputTeclado.setText("");
            System.out.println(" inputTeclado habilitado");
        } else {
            System.out.println(" ERROR - inputTeclado es NULL");
        }

        if (Enviar != null) {
            Enviar.setEnabled(true);
            System.out.println("Enviar habilitado");
        } else {
            System.out.println(" - Botón Enviar es NULL");
        }
    }
    
    /**
    * Procesa la entrada del JTextField
    */
    private void enviarEntrada() {
        // Buscar primero en slots de ejecución
        BCP[] procesos = gestor.getProcesosEnEjecucion();
        for (int cpuID = 0; cpuID < procesos.length; cpuID++) {
            BCP proceso = procesos[cpuID];
            if (proceso != null && proceso.isEsperandoEntrada()) {
                procesarEntrada(cpuID, proceso);
                return;
            }
        }

        // Buscar en cola de espera
        List<BCP> colaEspera = gestor.getColaEspera();
        for (BCP proceso : colaEspera) {
            if (proceso != null && proceso.isEsperandoEntrada()) {
                int cpuID = proceso.getCpuId();
                procesarEntrada(cpuID, proceso);
                return;
            }
        }

        // No encontró proceso esperando
        pantalla.append("\nError: No hay proceso esperando entrada\n");
    }    
    
    
    private void procesarEntrada(int cpuID, BCP proceso) {
        String input = inputTeclado.getText().trim();
        System.out.println(" Procesando entrada: " + input);

        if (input.isEmpty()) {
            pantalla.append("\nError: Debe ingresar un valor\n");
            return;
        }

        try {
            int valor = Integer.parseInt(input);

            if (valor < 0 || valor > 255) {
                pantalla.append("\nError: El valor debe estar entre 0 y 255\n");
                inputTeclado.setText("");
                inputTeclado.requestFocus();
                return;
            }

            // Mostrar valor ingresado
            pantalla.append(valor + "\n");

            // Procesar entrada
            gestor.procesarEntradaTeclado(cpuID, valor);

            // Deshabilitar entrada
            inputTeclado.setEnabled(false);
            inputTeclado.setText("");
            Enviar.setEnabled(false);

            // Actualizar GUI
            actualizarTablas();

            System.out.println("DEBUG: Entrada procesada correctamente");

        } catch (NumberFormatException e) {
            pantalla.append("\nError: Debe ingresar un número válido\n");
            inputTeclado.setText("");
            inputTeclado.requestFocus();
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        execute = new javax.swing.JButton();
        nextStep = new javax.swing.JButton();
        setNewMemory = new javax.swing.JButton();
        stadistics = new javax.swing.JButton();
        loadFile = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        discTable = new javax.swing.JTable();
        jScrollPane1 = new javax.swing.JScrollPane();
        BCP = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        newMemorySize = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        userMemory = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        pantalla = new javax.swing.JTextArea();
        jScrollPane5 = new javax.swing.JScrollPane();
        memoryTable1 = new javax.swing.JTable();
        jScrollPane6 = new javax.swing.JScrollPane();
        ProcessStatesTable1 = new javax.swing.JTable();
        clean = new javax.swing.JButton();
        Enviar = new javax.swing.JButton();
        inputTeclado = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        execute.setText("start");
        execute.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                executeActionPerformed(evt);
            }
        });

        nextStep.setText("next step");
        nextStep.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextStepActionPerformed(evt);
            }
        });

        setNewMemory.setText("set");
        setNewMemory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setNewMemoryActionPerformed(evt);
            }
        });

        stadistics.setText("stadistics");
        stadistics.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stadisticsActionPerformed(evt);
            }
        });

        loadFile.setText("Load file");
        loadFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadFileActionPerformed(evt);
            }
        });

        discTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Position", "Value in Disc"
            }
        ));
        jScrollPane4.setViewportView(discTable);

        BCP.setColumns(20);
        BCP.setRows(5);
        BCP.setEnabled(false);
        jScrollPane1.setViewportView(BCP);

        jLabel1.setText("BCP");

        jLabel2.setText("Memory size");

        newMemorySize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newMemorySizeActionPerformed(evt);
            }
        });

        jLabel3.setText("S.O memory");

        userMemory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                userMemoryActionPerformed(evt);
            }
        });

        jLabel4.setText("Pantalla");

        pantalla.setColumns(20);
        pantalla.setRows(5);
        pantalla.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                pantallaKeyPressed(evt);
            }
        });
        jScrollPane2.setViewportView(pantalla);

        memoryTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Position", "Value in memory"
            }
        ));
        jScrollPane5.setViewportView(memoryTable1);

        ProcessStatesTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Process", "States"
            }
        ));
        jScrollPane6.setViewportView(ProcessStatesTable1);

        clean.setText("Clean");
        clean.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cleanActionPerformed(evt);
            }
        });

        Enviar.setText("Enter");
        Enviar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EnviarActionPerformed(evt);
            }
        });

        jLabel5.setText("Teclado");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(execute)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(nextStep)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(clean, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(stadistics)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(newMemorySize, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(userMemory, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(loadFile, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(setNewMemory)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGap(571, 571, 571))
            .addGroup(layout.createSequentialGroup()
                .addGap(34, 34, 34)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 297, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 297, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(33, 33, 33)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 297, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(253, 253, 253)
                                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(Enviar)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel5)
                                .addComponent(inputTeclado, javax.swing.GroupLayout.PREFERRED_SIZE, 266, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 252, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(38, 38, 38)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 395, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addGap(365, 365, 365))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(loadFile, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(execute)
                            .addComponent(nextStep)
                            .addComponent(stadistics)
                            .addComponent(jLabel2)
                            .addComponent(newMemorySize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(clean))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(userMemory, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(setNewMemory)
                .addGap(29, 29, 29)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(22, 22, 22)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(13, 13, 13)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(inputTeclado, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Enviar)
                .addContainerGap(75, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void executeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_executeActionPerformed
        // TODO add your handling code here:
        if (gestor.isEjecutando()) {
            // Si ya está ejecutando, detener
            gestor.detenerEjecucion();
            execute.setText("start");
            nextStep.setEnabled(true);
            loadFile.setEnabled(true);
        } else {
            // Iniciar ejecución automática
            gestor.ejecutarAutomatico();
            execute.setText("stop");
            nextStep.setEnabled(false);  // Deshabilitar next step durante ejecución automática
            loadFile.setEnabled(false);   // No permitir cargar archivos mientras ejecuta
        }
    }//GEN-LAST:event_executeActionPerformed

    private void setNewMemoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setNewMemoryActionPerformed
        // TODO add your handling code here:
        try {
            // Leer valores de los campos de texto
            String memoriaStr = newMemorySize.getText().trim();
            String soStr = userMemory.getText().trim();

            if (memoriaStr.isEmpty() || soStr.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(this,
                    "Por favor ingrese ambos valores",
                    "Advertencia",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }

            int tamanioTotal = Integer.parseInt(memoriaStr);
            int tamanioSO = Integer.parseInt(soStr);

            // Validaciones
            if (tamanioTotal <= 0 || tamanioSO <= 0) {
                javax.swing.JOptionPane.showMessageDialog(this,
                    "Los valores deben ser positivos",
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (tamanioSO >= tamanioTotal) {
                javax.swing.JOptionPane.showMessageDialog(this,
                    "La memoria del SO no puede ser mayor o igual a la memoria total",
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Reconfigurar memoria
            gestor.reconfigurarMemoria(tamanioTotal, tamanioSO);

            // Actualizar tablas
            actualizarTablas();

            javax.swing.JOptionPane.showMessageDialog(this,
                "Memoria reconfigurada correctamente\nTotal: " + tamanioTotal + 
                " KB\nSO: " + tamanioSO + " KB\nUsuario: " + (tamanioTotal - tamanioSO) + " KB",
                "Éxito",
                javax.swing.JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Por favor ingrese valores numéricos válidos",
                "Error",
                javax.swing.JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Error al reconfigurar memoria:\n" + e.getMessage(),
                "Error",
                javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_setNewMemoryActionPerformed

    private void stadisticsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stadisticsActionPerformed
        // TODO add your handling code here:
        List<EstadisticaProceso> estadisticas = gestor.getEstadisticas();
        if (estadisticas.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "No hay estadísticas disponibles.\nEjecute programas primero.",
                "Sin estadísticas",
                javax.swing.JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Crear ventana de estadísticas
        mostrarVentanaEstadisticas(estadisticas);
    }//GEN-LAST:event_stadisticsActionPerformed

    private void newMemorySizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newMemorySizeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_newMemorySizeActionPerformed

    private void userMemoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_userMemoryActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_userMemoryActionPerformed

    private void cleanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cleanActionPerformed
        // TODO add your handling code here:
        int respuesta = javax.swing.JOptionPane.showConfirmDialog(this,"¿Está seguro de limpiar todo el sistema?\nSe perderán todos los procesos cargados.","Confirmar limpieza",javax.swing.JOptionPane.YES_NO_OPTION,javax.swing.JOptionPane.QUESTION_MESSAGE);

        if (respuesta == javax.swing.JOptionPane.YES_OPTION) {
            // Limpiar sistema
            gestor.limpiarSistema();

            // Limpiar pantalla
            pantalla.setText("");

            // Actualizar tablas
            actualizarTablas();

            // Resetear botón execute
            execute.setText("start");
            nextStep.setEnabled(true);
            loadFile.setEnabled(true);

            javax.swing.JOptionPane.showMessageDialog(this,"Sistema limpiado correctamente","Información",javax.swing.JOptionPane.INFORMATION_MESSAGE);
        }
    }//GEN-LAST:event_cleanActionPerformed

    private void loadFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadFileActionPerformed
        // Crear selector de archivos
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
        fileChooser.setMultiSelectionEnabled(true); // Permitir múltiples archivos
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".asm");
            }

            @Override
            public String getDescription() {
                return "Archivos ASM (*.asm)";
            }
        });

        // Mostrar diálogo
        int resultado = fileChooser.showOpenDialog(this);

        if (resultado == javax.swing.JFileChooser.APPROVE_OPTION) {
            File[] archivos = fileChooser.getSelectedFiles();

            if (archivos.length == 0) {
                javax.swing.JOptionPane.showMessageDialog(this,
                    "No se seleccionaron archivos",
                    "Advertencia",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                // Cargar programas en el gestor
                gestor.cargarProgramas(archivos);

                // Actualizar tablas
                actualizarTablas();

                // Mostrar mensaje de éxito
                javax.swing.JOptionPane.showMessageDialog(this,
                    archivos.length + " archivo(s) cargado(s) correctamente",
                    "Éxito",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al cargar archivos:\n" + e.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }//GEN-LAST:event_loadFileActionPerformed

    private void nextStepActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextStepActionPerformed
        // TODO add your handling code here:
        try {
            // Ejecutar un ciclo
            gestor.ejecutarPasoAPaso();

            actualizarTablas();
            verificarEntradaPendiente();

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Error durante la ejecución:\n" + e.getMessage(), "Error",javax.swing.JOptionPane.ERROR_MESSAGE);e.printStackTrace();
        }
    }//GEN-LAST:event_nextStepActionPerformed

    private void pantallaKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_pantallaKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_pantallaKeyPressed

    private void EnviarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EnviarActionPerformed
        // TODO add your handling code here:
        enviarEntrada();
    }//GEN-LAST:event_EnviarActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new App().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JTextArea BCP;
    private javax.swing.JButton Enviar;
    public javax.swing.JTable ProcessStatesTable1;
    public javax.swing.JButton clean;
    public javax.swing.JTable discTable;
    public javax.swing.JButton execute;
    private javax.swing.JTextField inputTeclado;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    public javax.swing.JButton loadFile;
    public javax.swing.JTable memoryTable1;
    public javax.swing.JTextField newMemorySize;
    public javax.swing.JButton nextStep;
    public javax.swing.JTextArea pantalla;
    public javax.swing.JButton setNewMemory;
    public javax.swing.JButton stadistics;
    public javax.swing.JTextField userMemory;
    // End of variables declaration//GEN-END:variables
}
