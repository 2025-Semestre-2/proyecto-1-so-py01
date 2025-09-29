package instrucciones;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author dylan y gadyr
 */
public class Instruccion {

    private final Opcode opcode;
    private final List<String> operandos;

    public Instruccion(Opcode opcode, List<String> operandos) {
        this.opcode = opcode;
        this.operandos = new ArrayList<>(operandos);
        validarOperandos();
    }

    private void validarOperandos() {
        int n = operandos.size();
        int esperado = opcode.getNumOperandos();

        // Validación general
        if (esperado >= 0 && n != esperado) {
            throw new IllegalArgumentException(opcode.name() + " espera " + esperado + "operandos, recibió " + n);
        }
        if (opcode == Opcode.PARAM && (n < 1 || n > 3)) {
            throw new IllegalArgumentException("PARAM espera entre 1 y 3 operandos, recibió " + n);
        }
        if ((opcode == Opcode.INC || opcode == Opcode.DEC) && (n < 0 || n > 1)) {
            throw new IllegalArgumentException(opcode.name() + " espera 0 o 1 operando");
        }

        // Validaciones específicas
        for (String op : operandos) {
            if (opcode == Opcode.INT) {
                validarInterrupcion(op);
            } else if (opcode == Opcode.JMP || opcode == Opcode.JE || opcode == Opcode.JNE) {
                validarDesplazamiento(op);
            } else if (opcode == Opcode.MOV) {
                // MOV puede ser reg, reg  o  reg, valor
                validarRegistro(operandos.get(0)); // siempre primer operando es registro
                if (n == 2 && !esRegistro(operandos.get(1)) && !esNumero(operandos.get(1))) {
                    throw new IllegalArgumentException("MOV espera registro o constante, recibió " + operandos.get(1));
                }
            } else if (esRegistroEsperado(opcode)) {
                validarRegistro(op);
            }
        }
    }

    private boolean esRegistro(String token) {
        String reg = token.toUpperCase();
        return reg.equals("AC") || reg.equals("AX") || reg.equals("BX")
                || reg.equals("CX") || reg.equals("DX");
    }

    private boolean esNumero(String token) {
        try {
            Integer.valueOf(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean esRegistroEsperado(Opcode op) {
        return op == Opcode.LOAD || op == Opcode.STORE || op == Opcode.MOV
                || op == Opcode.ADD || op == Opcode.SUB || op == Opcode.INC
                || op == Opcode.DEC || op == Opcode.SWAP
                || op == Opcode.CMP || op == Opcode.PUSH || op == Opcode.POP;
    }

    private void validarRegistro(String token) {
        String reg = token.toUpperCase();
        if (!(reg.equals("AC") || reg.equals("AX") || reg.equals("BX")
                || reg.equals("CX") || reg.equals("DX"))) {
            throw new IllegalArgumentException("Registro inválido: " + token);
        }
    }

    private void validarInterrupcion(String token) {
        String val = token.toUpperCase();
        if (!(val.equals("20H") || val.equals("10H")
                || val.equals("09H") || val.equals("21H"))) {
            throw new IllegalArgumentException("Interrupción inválida: " + token);
        }
    }

    private void validarDesplazamiento(String token) {
        if (!(token.startsWith("+") || token.startsWith("-"))) {
            throw new IllegalArgumentException(
                    "Desplazamiento inválido: " + token + " (usar +n o -n)"
            );
        }
        try {
            Integer.valueOf(token);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Desplazamiento no numérico: " + token);
        }
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public List<String> getOperandos() {
        return new ArrayList<>(operandos);
    }

    @Override
    public String toString() {
        return opcode.name() + (operandos.isEmpty() ? "" : " " + String.join(", ", operandos));
    }
}
