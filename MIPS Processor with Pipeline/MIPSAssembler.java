import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

//Compile with JDK 9+: javac --release 8 MIPSAssembler.java
//Compile with JDK 8:  javac MIPSAssembler.java
//       java MIPSAssembler .\program.asm program.hex

public class MIPSAssembler {

    private static final Map<String, Integer> OPCODE = new HashMap<>();
    private static final Map<String, Integer> REGISTER = new HashMap<>();

    static {
        OPCODE.put("sll",  0x0);
        OPCODE.put("andi", 0x1);
        OPCODE.put("sw",   0x2);
        OPCODE.put("srl",  0x3);
        OPCODE.put("ori",  0x4);
        OPCODE.put("lw",   0x5);
        OPCODE.put("beq",  0x6);
        OPCODE.put("or",   0x7);
        OPCODE.put("subi", 0x8);
        OPCODE.put("add",  0x9);
        OPCODE.put("addi", 0xA);
        OPCODE.put("and",  0xB);
        OPCODE.put("sub",  0xC);
        OPCODE.put("bneq", 0xD);
        OPCODE.put("nor",  0xE);
        OPCODE.put("j",    0xF);

       

        REGISTER.put("$zero", 0x0);
        REGISTER.put("$t0",   0x1);
        REGISTER.put("$t1",   0x2);
        REGISTER.put("$t2",   0x3);
        REGISTER.put("$t3",   0x4);
        REGISTER.put("$t4",   0x5);
        REGISTER.put("$sp",   0x6);
    }

    public static void main(String[] args) {
        
        String inputFile = "";
        String outputFile = "program.hex";

        if (args.length >= 1)
            inputFile = args[0];

        if (args.length >= 2)
            outputFile = args[1];

        try {

            assemble(inputFile, outputFile);

            System.out.println();
            System.out.println("Assembly completed successfully.");
            System.out.println("Input file : " + inputFile);
            System.out.println("Output file: " + outputFile);
            System.out.println();
            System.out.println("Load " + outputFile
                    + " into the Logisim ROM using Load Image.");

        } catch (Exception e) {

            System.err.println();
            System.err.println("ASSEMBLER ERROR");
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }




    private static void assemble(
            String inputFile,
            String outputFile
    ) throws IOException {

        String source = new String(Files.readAllBytes(Paths.get(inputFile)), StandardCharsets.UTF_8);
        String hex = assemble(source);
        Files.write(Paths.get(outputFile), hex.getBytes(StandardCharsets.UTF_8));
    }

    /** Assembles the GUI's instruction set without inserting extra instructions. */
    public static String assemble(String source) {
        Objects.requireNonNull(source, "Source must not be null");
        List<String> lines = Arrays.asList(source.replace("\uFEFF", "").split("\\R", -1));

        Map<String, Integer> labels = findLabels(lines);

        List<Integer> machineCode = new ArrayList<>();

        int pc = 0;

        System.out.println();
        System.out.println("ADDRESS    MACHINE CODE    ASSEMBLY");
        System.out.println("-----------------------------------------------");

        for (int lineNumber = 0;
             lineNumber < lines.size();
             lineNumber++) {

            String line = cleanLine(lines.get(lineNumber));

            if (line.isEmpty())
                continue;

            /*
             * Remove label if line contains:
             */

            line = removeLabels(line);

            if (line.isEmpty())
                continue;

            try {

                int instruction =
                        assembleInstruction(line, labels, pc);

                machineCode.add(instruction);

                System.out.printf(
                        "%02X         %04X            %s%n",
                        pc,
                        instruction & 0xFFFF,
                        line
                );

                pc++;

            } catch (RuntimeException e) {

                throw new RuntimeException(
                        "Line " + (lineNumber + 1)
                                + ": "
                                + lines.get(lineNumber)
                                + "\n"
                                + e.getMessage()
                );
            }
        }

        if (machineCode.isEmpty()) {
            throw new IllegalArgumentException("Enter at least one instruction before assembling.");
        }
        StringBuilder hex = new StringBuilder("v2.0 raw\n");
        for (int instruction : machineCode) {
            hex.append(String.format(Locale.ROOT, "%04X%n", instruction & 0xFFFF));
        }
        return hex.toString();
    }

    // FIRST PASS - FIND LABELS


    private static Map<String, Integer> findLabels(
            List<String> lines
    ) {

        Map<String, Integer> labels = new HashMap<>();

        int pc = 0;

        for (int i = 0; i < lines.size(); i++) {

            String line = cleanLine(lines.get(i));

            if (line.isEmpty())
                continue;

            while (line.contains(":")) {

                int colon = line.indexOf(':');

                String label =
                        line.substring(0, colon).trim();

                if (label.isEmpty()) {

                    throw new RuntimeException(
                            "Invalid label at line "
                                    + (i + 1)
                    );
                }

                if (labels.containsKey(label)) {

                    throw new RuntimeException(
                            "Duplicate label \""
                                    + label
                                    + "\""
                    );
                }

                labels.put(label, pc);

                line =
                        line.substring(colon + 1).trim();

                if (line.isEmpty())
                    break;
            }

            if (!line.isEmpty())
                pc++;
        }

        return labels;
    }


    // ASSEMBLE ONE INSTRUCTION

    private static int assembleInstruction(
            String line,
            Map<String, Integer> labels,
            int pc
    ) {

        /*
         * Remove commas so both work:
         *
         * add $t0, $t1, $t2
         * add $t0 $t1 $t2
         */

        line = line
                .replace(",", " ")
                .replaceAll("\\s+", " ")
                .trim();

        String[] token = line.split(" ");

        String instruction =
                token[0].toLowerCase();

        Integer opcode = OPCODE.get(instruction);

        if (opcode == null) {

            throw new RuntimeException(
                    "Unknown instruction: "
                            + instruction
            );
        }

        switch (instruction) {
            case "add":
            case "sub":
            case "and":
            case "or":
            case "nor":
                return assembleRType(opcode, token);
            case "sll":
            case "srl":
                return assembleSType(opcode, token);
            case "addi":
            case "subi":
            case "andi":
            case "ori":
                return assembleImmediateType(opcode, token);
            case "lw":
            case "sw":
                return assembleMemoryType(opcode, token);
            case "beq":
            case "bneq":
                return assembleBranchType(opcode, token, labels, pc);
            case "j":
                return assembleJumpType(opcode, token, labels);
            default:
                throw new RuntimeException("Unsupported instruction: " + instruction);
        }
    }


    // ============================================================
    // R TYPE
    //
    // Assignment format:
    //
    // Opcode | Src Reg 1 | Src Reg 2 | Dst Reg
    //
    // Assembly syntax:
    //
    // add $t2, $t0, $t1
    //
    // means:
    //
    // $t2 = $t0 + $t1
    // ============================================================

    private static int assembleRType(
            int opcode,
            String[] token
    ) {

        requireOperands(token, 4);

        int dst =
                getRegister(token[1]);

        int src1 =
                getRegister(token[2]);

        int src2 =
                getRegister(token[3]);

        return (opcode << 12)
                | (src1 << 8)
                | (src2 << 4)
                | dst;
    }


    // ============================================================
    // S TYPE
    //
    // Opcode | Src Reg | Dst Reg | Shamt
    //
    // Example:
    //
    // sll $t1, $t0, 2
    //
    // means:
    //
    // $t1 = $t0 << 2
    // ============================================================

    private static int assembleSType(
            int opcode,
            String[] token
    ) {

        requireOperands(token, 4);

        int dst =
                getRegister(token[1]);

        int src =
                getRegister(token[2]);

        int shamt =
                parseUnsigned4Bit(token[3]);

        return (opcode << 12)
                | (src << 8)
                | (dst << 4)
                | shamt;
    }


    // ============================================================
    // IMMEDIATE
    //
    // Opcode | Src Reg | Dst Reg | Immediate
    //
    // Example:
    //
    // addi $t1, $t0, 3
    //
    // means:
    //
    // $t1 = $t0 + 3
    // ============================================================

    private static int assembleImmediateType(
            int opcode,
            String[] token
    ) {

        requireOperands(token, 4);

        int dst =
                getRegister(token[1]);

        int src =
                getRegister(token[2]);

        int immediate =
                parse4BitValue(token[3]);

        return (opcode << 12)
                | (src << 8)
                | (dst << 4)
                | immediate;
    }


    // ============================================================
    // MEMORY
    //
    // Syntax:
    //
    // lw $t0, 3($t1)
    // sw $t0, 3($t1)
    //
    // Machine format:
    //
    // Opcode | Base Register | Data Register | Offset
    // ============================================================

    private static int assembleMemoryType(
            int opcode,
            String[] token
    ) {

        requireOperands(token, 3);

        int dataRegister =
                getRegister(token[1]);

        String operand = token[2];

        int open =
                operand.indexOf('(');

        int close =
                operand.indexOf(')');

        if (open <= 0
                || close <= open
                || close != operand.length() - 1) {

            throw new RuntimeException(
                    "Invalid memory operand: "
                            + operand
                            + "\nExpected format: "
                            + "3($t0)"
            );
        }

        String offsetText =
                operand.substring(0, open);

        String baseText =
                operand.substring(
                        open + 1,
                        close
                );

        int offset =
                parse4BitValue(offsetText);

        int base =
                getRegister(baseText);

        return (opcode << 12)
                | (base << 8)
                | (dataRegister << 4)
                | offset;
    }


    // ============================================================
    // BRANCH
    //
    // Syntax:
    //
    // beq  $t0, $t1, LOOP
    // bneq $t0, $t1, LOOP
    //
    // Uses a 4-bit signed PC-relative offset:
    //
    // offset = target - (PC + 1)
    //
    // CPU side must therefore use:
    //
    // PCnext = PC + 1 + signExtend(offset)
    // ============================================================

    private static int assembleBranchType(
            int opcode,
            String[] token,
            Map<String, Integer> labels,
            int pc
    ) {

        requireOperands(token, 4);

        int src1 =
                getRegister(token[1]);

        int src2 =
                getRegister(token[2]);

        String target =
                token[3];

        int branchValue;

        if (labels.containsKey(target)) {

            int targetAddress =
                    labels.get(target);

            int offset =
                    targetAddress - (pc + 1);

            if (offset < -8 || offset > 7) {

                throw new RuntimeException(
                        "Branch to \"" + target
                                + "\" is too far.\n"
                                + "4-bit signed branch range "
                                + "is -8 to +7 instructions."
                );
            }

            branchValue =
                    offset & 0xF;

        } else {

            int offset =
                    parseNumber(target);

            if (offset < -8 || offset > 7) {

                throw new RuntimeException(
                        "Branch offset must be "
                                + "between -8 and 7."
                );
            }

            branchValue =
                    offset & 0xF;
        }

        return (opcode << 12)
                | (src1 << 8)
                | (src2 << 4)
                | branchValue;
    }


    // ============================================================
    // J TYPE
    //
    // Assignment format:
    //
    // Opcode | Target Jump Address (8 bits) | 0000
    //
    // Example:
    //
    // j LOOP
    // ============================================================

    private static int assembleJumpType(
            int opcode,
            String[] token,
            Map<String, Integer> labels
    ) {

        requireOperands(token, 2);

        String target =
                token[1];

        int address;

        if (labels.containsKey(target)) {

            address =
                    labels.get(target);

        } else {

            address =
                    parseNumber(target);
        }

        if (address < 0 || address > 255) {

            throw new RuntimeException(
                    "Jump address must be "
                            + "between 0 and 255."
            );
        }

        return (opcode << 12)
                | (address << 4);
    }


    // ============================================================
    // REGISTER
    // ============================================================

    private static int getRegister(
            String text
    ) {

        text =
                text.toLowerCase();

        Integer value =
                REGISTER.get(text);

        if (value == null) {

            throw new RuntimeException(
                    "Unknown register: "
                            + text
            );
        }

        return value;
    }


    // ============================================================
    // NUMBERS
    // ============================================================

    private static int parseNumber(
            String text
    ) {

        text =
                text.trim().toLowerCase();

        try {

            if (text.startsWith("-0x")) {

                return -Integer.parseInt(
                        text.substring(3),
                        16
                );
            }

            if (text.startsWith("0x")) {

                return Integer.parseInt(
                        text.substring(2),
                        16
                );
            }

            if (text.startsWith("-0b")) {

                return -Integer.parseInt(
                        text.substring(3),
                        2
                );
            }

            if (text.startsWith("0b")) {

                return Integer.parseInt(
                        text.substring(2),
                        2
                );
            }

            return Integer.parseInt(text);

        } catch (NumberFormatException e) {

            throw new RuntimeException(
                    "Invalid number: "
                            + text
            );
        }
    }


    /*
     * Used for immediate / memory offset.
     *
     * Allows:
     *
     * -8 ... 15
     *
     * Examples:
     *
     * 5   -> 0101
     * 15  -> 1111
     * -1  -> 1111
     * -8  -> 1000
     */

    private static int parse4BitValue(
            String text
    ) {

        int value =
                parseNumber(text);

        if (value < -8 || value > 15) {

            throw new RuntimeException(
                    "Value " + value
                            + " does not fit "
                            + "in the 4-bit field."
            );
        }

        return value & 0xF;
    }


    /*
     * Shift amount should not be negative.
     */

    private static int parseUnsigned4Bit(
            String text
    ) {

        int value =
                parseNumber(text);

        if (value < 0 || value > 15) {

            throw new RuntimeException(
                    "Shift amount must be "
                            + "between 0 and 15."
            );
        }

        return value;
    }


    // OPERAND COUNT

    private static void requireOperands(
            String[] token,
            int expected
    ) {

        if (token.length != expected) {

            throw new RuntimeException(
                    "Incorrect number of operands."
            );
        }
    }

    // REMOVE COMMENTS

    private static String cleanLine(
            String line
    ) {

        int hash =
                line.indexOf('#');

        if (hash >= 0)
            line =
                    line.substring(0, hash);

        int slash =
                line.indexOf("//");

        if (slash >= 0)
            line =
                    line.substring(0, slash);

        return line.trim();
    }

    // REMOVE LABELS FROM INSTRUCTIO

    private static String removeLabels(
            String line
    ) {

        while (line.contains(":")) {

            int colon =
                    line.indexOf(':');

            line =
                    line.substring(
                            colon + 1
                    ).trim();
        }

        return line;
    }
}
