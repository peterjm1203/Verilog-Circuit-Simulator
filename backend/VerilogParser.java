package backend;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class VerilogParser {
    private String fileName;
    private Circuit circuit;

    public VerilogParser(String fileName) {
        this.fileName = fileName;
        this.circuit = new Circuit();
    }

    /**
     * Parses a Verilog file and returns the corresponding circuit object.
     * 
     * @return Circuit object representing the parsed Verilog design.
     * @throws IOException if the file cannot be read.
     */
    public String[][] parse() throws IOException {
        DataWrapper<String[]> inputs = new DataWrapper<String[]>(null);
        DataWrapper<String[]> outputs = new DataWrapper<String[]>(null);
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            Gate prevGate = null; // For linking gates in sequence

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip the module declaration
                if (line.startsWith("module")) {
                    continue;
                }

                if (line.startsWith("input")) {
                    inputs.add(parseInputs(line));
                } else if (line.startsWith("output")) {
                    outputs.add(parseOutputs(line));
                } else if (line.startsWith("wire")) {
                    circuit.parseInputs(stringListToArray(inputs));
                    circuit.parseOutputs(stringListToArray(outputs));
                    parseWires(line);
                } else if (line.matches("^[a-zA-Z]+\\s+\\w+\\s*\\(.*\\);")) {
                    Gate newGate = parseGate(line);
                    if (prevGate != null) {
                        circuit.addNextGate(prevGate, newGate);
                    }
                    prevGate = newGate;
                }
            }

        }
        return new String[][] { stringListToArray(inputs), stringListToArray(outputs) };
    }

    private String[] stringListToArray(DataWrapper<String[]> list) {
        String[] array = new String[list.count()];
        int i = 0;
        while (list != null) {
            array[i++] = list.data[0];
            list = list.next;
        }
        return array;
    }

    /**
     * Parses input declarations from a Verilog line.
     * 
     * @param line The Verilog line starting with "input".
     */
    private String[] parseInputs(String line) {
        return line.replace("input", "").replace(";", "").trim().split(",");
    }

    /**
     * Parses output declarations from a Verilog line.
     * 
     * @param line The Verilog line starting with "output".
     */
    private String[] parseOutputs(String line) {
        return line.replace("output", "").replace(";", "").trim().split(",");
    }

    /**
     * Parses wire declarations from a Verilog line.
     * 
     * @param line The Verilog line starting with "wire".
     */
    private void parseWires(String line) {
        String[] wires = line.replace("wire", "").replace(";", "").trim().split(",");
        for (String wire : wires) {
            circuit.addWire(wire.trim());
        }
    }

    /**
     * Parses a gate declaration from a Verilog line and adds it to the circuit.
     * 
     * @param line The Verilog line defining a gate.
     * @return The parsed Gate object.
     */
    private Gate parseGate(String line) {
        Pattern pattern = Pattern.compile("([a-zA-Z]+)\\s+(\\w+)\\s*\\((.*)\\);");
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            String gateType = matcher.group(1); // Gate type (EX AND, OR)
            String gateName = matcher.group(2); // Gate name (EX XG1, XG2)
            String[] connections = matcher.group(3).split(","); // Connections (wires)

            GateType type = GateType.valueOf(gateType.toUpperCase());
            Gate gate = circuit.addGate(gateName, type);

            // First connection is the output wire
            ensureWireExists(connections[0].trim());
            circuit.addFanOut(connections[0].trim(), gate);

            // Remaining connections are input wires
            for (int i = 1; i < connections.length; i++) {
                ensureWireExists(connections[i].trim());
                circuit.addFanIn(connections[i].trim(), gate);
            }

            return gate;
        }

        throw new IllegalArgumentException("Invalid gate declaration: " + line);
    }

    /**
     * Ensures a wire exists in the circuit. If it doesn't, adds it dynamically.
     * 
     * @param name The name of the wire to ensure exists.
     */
    private void ensureWireExists(String name) {
        if (!circuit.wireList.containsKey(name)) {
            circuit.addWire(name);
        }
    }

    /**
     * Parses vector file
     * 
     * @param vectorFilePath
     * @return
     * @throws IOException
     */
    public String[][] parseVectorFile(String vectorFilePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(vectorFilePath))) {
            List<String[]> vectors = new ArrayList<>();
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                vectors.add(line.split(""));
            }

            return vectors.toArray(new String[0][]);
        }
    }

    /**
     * Main method for testing the Verilog parser.
     * 
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java backend.VerilogParser <file-path> <vector-file-path");
            System.exit(1);
        }

        String filePath = args[0];
        String vectorFilePath = args[1];

        try {
            long totalStartTime = System.currentTimeMillis();

            VerilogParser parser = new VerilogParser(filePath);
            String[][] inputsOutputsList = parser.parse();
            String[][] vectors = parser.parseVectorFile(vectorFilePath);
            // iterate through wires appropriately

            // String[][] vectors
            parser.circuit.mainMethod(inputsOutputsList[0], inputsOutputsList[1], vectors, filePath);
            // Simulates circuit and prints output

            long totalEndTime = System.currentTimeMillis();

            System.out.println("Total simulation time: " + (totalEndTime - totalStartTime) + " ms");

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error parsing Verilog file: " + e.getMessage());
        }
    }
}
