package unittesting;

import instrucciones.*;

/**
 *
 * @author dylan y gadyr 
 */
public class TestParser {
    public static void main(String[] args) {
        String[] ejemplos = {
            "MOV AX, 5",
            "ADD BX",
            "INC",
            "DEC CX",
            "SWAP AX, BX",
            "JMP +3",
            "CMP AX, BX",
            "JE -2",
            "INT 20H",
            "PARAM 10, 20, 30",
            "PUSH AX",
            "POP BX ;abc",
            "; Esto es un comentario"
        };

        for (String linea : ejemplos) {
            try {
                Instruccion inst = InstructionParser.parse(linea);
                IR ir = new IR(inst);
                System.out.println(inst + " -> IR=" + ir.getBinario());
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}
