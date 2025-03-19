import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class HackAssembler {
    // Symbol tables for predefined values, computation, destination, and jump codes
    private static final Map<String, Integer> SYMBOLS = new HashMap<>();
    private static final Map<String, String> COMP_CODES = new HashMap<>();
    private static final Map<String, String> DEST_CODES = new HashMap<>();
    private static final Map<String, String> JUMP_CODES = new HashMap<>();
    private static int variableAddress = 16; // Starting address for new variables

    static {
        // Initialize predefined symbols
        String[] predefinedSymbols = {"SP", "LCL", "ARG", "THIS", "THAT", "R0", "R1", "R2", "R3", "R4", "R5", "R6", "R7", "R8", "R9", "R10", "R11", "R12", "R13", "R14", "R15"};
        for (int i = 0; i < predefinedSymbols.length; i++) {
            SYMBOLS.put(predefinedSymbols[i], i);
        }
        SYMBOLS.put("SCREEN", 16384);
        SYMBOLS.put("KBD", 24576);

        // Initialize computation codes
        COMP_CODES.put("0", "101010"); COMP_CODES.put("1", "111111"); COMP_CODES.put("-1", "111010"); COMP_CODES.put("D", "001100"); 
        COMP_CODES.put("A", "110000"); COMP_CODES.put("!D", "001101"); COMP_CODES.put("!A", "110001"); COMP_CODES.put("-D", "001111");
        COMP_CODES.put("-A", "110011"); COMP_CODES.put("D+1", "011111"); COMP_CODES.put("A+1", "110111"); COMP_CODES.put("D-1", "001110");
        COMP_CODES.put("A-1", "110010"); COMP_CODES.put("D+A", "000010"); COMP_CODES.put("D-A", "010011"); COMP_CODES.put("A-D", "000111"); 
        COMP_CODES.put("D&A", "000000"); COMP_CODES.put("D|A", "010101"); COMP_CODES.put("M", "110000"); COMP_CODES.put("!M", "110001");
        COMP_CODES.put("-M", "110011"); COMP_CODES.put("M+1", "110111"); COMP_CODES.put("M-1", "110010"); COMP_CODES.put("D+M", "000010");
        COMP_CODES.put("D-M", "010011"); COMP_CODES.put("M-D", "000111"); COMP_CODES.put("D&M", "000000"); COMP_CODES.put("D|M", "010101");

        // Destination codes
        DEST_CODES.put("", "000"); DEST_CODES.put("M", "001"); DEST_CODES.put("D", "010"); DEST_CODES.put("MD", "011");
        DEST_CODES.put("A", "100"); DEST_CODES.put("AM", "101"); DEST_CODES.put("AD", "110"); DEST_CODES.put("AMD", "111");

        // Jump codes
        JUMP_CODES.put("", "000"); JUMP_CODES.put("JGT", "001"); JUMP_CODES.put("JEQ", "010"); JUMP_CODES.put("JGE", "011");
        JUMP_CODES.put("JLT", "100"); JUMP_CODES.put("JNE", "101"); JUMP_CODES.put("JLE", "110"); JUMP_CODES.put("JMP", "111");
    }

    public static void main(String[] args) throws IOException {
        File inputFile = new File(args[0]);
        List<String> lines = Files.readAllLines(inputFile.toPath());

        // First pass: Identify label definitions and store their addresses
        Map<String, Integer> labelAddresses = new HashMap<>();
        int instructionAddress = 0;
        for (String line : lines) {
            String processedLine = removeSpaces(removeComments(line));
            if (processedLine.isEmpty()) continue;
            if (processedLine.startsWith("(") && processedLine.endsWith(")")) {
                String label = processedLine.substring(1, processedLine.length() - 1);
                labelAddresses.put(label, instructionAddress);
            } else {
                instructionAddress++;
            }
        }

        // Second pass: Translate assembly to binary instructions
        List<String> binaryInstructions = new ArrayList<>();
        for (String line : lines) {
            String processedLine = removeSpaces(removeComments(line));
            if (processedLine.isEmpty() || (processedLine.startsWith("(") && processedLine.endsWith(")"))) continue;
            if (processedLine.startsWith("@")) {
                binaryInstructions.add(translateAInstruction(processedLine, labelAddresses));
            } else {
                binaryInstructions.add(translateCInstruction(processedLine));
            }
        }

        // Write output file
        File outputFile = new File(inputFile.getParent(), inputFile.getName().replace(".asm", ".hack"));
        Files.write(outputFile.toPath(), String.join("\n", binaryInstructions).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // Translates A-instruction to binary
    private static String translateAInstruction(String line, Map<String, Integer> labelAddresses) {
        String symbol = line.substring(1);
        int address;
        if (symbol.matches("\\d+")) {
            address = Integer.parseInt(symbol);
        } else if (SYMBOLS.containsKey(symbol)) {
            address = SYMBOLS.get(symbol);
        } else if (labelAddresses.containsKey(symbol)) {
            address = labelAddresses.get(symbol);
        } else {
            address = variableAddress;
            SYMBOLS.put(symbol, variableAddress++);
        }
        return "0" + String.format("%15s", Integer.toBinaryString(address)).replace(' ', '0');
    }

    // Translates C-instruction to binary
    private static String translateCInstruction(String line) {
        String dest = "", comp = line, jump = "";
        if (line.contains("=")) {
            String[] parts = line.split("=");
            dest = parts[0];
            comp = parts[1];
        }
        if (line.contains(";")) {
            String[] parts = line.split(";");
            comp = parts[0];
            jump = parts[1];
        }
        String aBit = "0";
        if (comp.contains("M")) {
            aBit = "1";
            comp = comp.replace("M", "A");
        }
        String compCode = COMP_CODES.getOrDefault(comp, "000000");
        String destCode = DEST_CODES.getOrDefault(dest, "000");
        String jumpCode = JUMP_CODES.getOrDefault(jump, "000");
        return "111" + aBit + compCode + destCode + jumpCode;
    }

    // Removes comments from a line
    private static String removeComments(String line) {
        int index = line.indexOf("//");
        if (index != -1) {
            return line.substring(0, index).trim();
        }
        return line.trim();
    }

    // Removes spaces from a line
    private static String removeSpaces(String line) {
        return line.replace(" ", "").trim();
    }
}