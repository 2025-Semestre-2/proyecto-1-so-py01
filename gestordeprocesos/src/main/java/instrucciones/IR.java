package instrucciones;

/**
 *
 * @author dylan y Gadyr
 * 
 */
public class IR {
    private final Instruccion instruccion;

    public IR(Instruccion instruccion) {
        this.instruccion = instruccion;
    }

    public String getBinario() {
        // Representación simple: 5 bits para opcode, 11 para operandos hash
        String opBits = String.format("%5s", Integer.toBinaryString(instruccion.getOpcode().ordinal()))
                            .replace(' ', '0');
        int hashOps = instruccion.getOperandos().toString().hashCode() & 0x7FF;
        String opsBits = String.format("%11s", Integer.toBinaryString(hashOps))
                             .replace(' ', '0');
        return opBits + opsBits;
    }

    @Override
    public String toString() {
        return "[IR] " + instruccion.toString() + " -> " + getBinario();
    }
}
