package instrucciones;

/**
 *
 * @author dylan y gadyr
 */
public enum Opcode {
    LOAD(2, 1),    // 1 operando: reg
    STORE(2, 1),   // 1 operando: reg
    MOV(1, 2),     // 2 operandos: reg, reg|const
    ADD(3, 1),     // 1 operando: reg
    SUB(3, 1),     // 1 operando: reg
    INC(1, -1),    // 0 o 1 operando
    DEC(1, -1),    // 0 o 1 operando
    SWAP(1, 2),    // 2 operandos: reg, reg
    INT(2, 1),     // 1 operando: código int
    JMP(2, 1),     // 1 operando: desplazamiento
    CMP(2, 2),     // 2 operandos: reg, reg
    JE(2, 1),      // 1 operando: desplazamiento
    JNE(2, 1),     // 1 operando: desplazamiento
    PARAM(3, -1),  // 1..3 operandos: valores
    PUSH(1, 1),    // 1 operando: reg
    POP(1, 1);     // 1 operando: reg
    
    private final int peso;             // tiempo en sengundos
    private final int numOperandos;     // -1 = variable
    
    Opcode(int peso, int numOperandos) {
        this.peso = peso;
        this.numOperandos = numOperandos;
    }
    
    public int getPeso() { return peso; }
    
    public int getNumOperandos() { return numOperandos; }
}
