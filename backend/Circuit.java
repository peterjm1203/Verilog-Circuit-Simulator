package backend;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class Circuit {

    HashMap<String, Wire> wireList;
    HashMap<String, Wire> inputs;
    HashMap<String, Wire> outputs;
    HashMap<Integer, HashMap<String, Entity>> sched;
    Gate firstGate;
    Gate lastGate;

    Circuit() {
        this.wireList = new HashMap<String, Wire>(27157);
        this.inputs = new HashMap<>(40);
        this.outputs = new HashMap<>(350);
        this.sched = new HashMap<>(1850);
    }

    /***
     * Adds a wire to the circuit registry. Inputs and outputs to
     * each wire will be taken care of while adding gates
     * 
     * @param name the name of the wire
     */
    public void addWire(String name) {
        wireList.put(name, new Wire(name));
    }

    /***
     * Adds a gate to the circuit registry. This method will construct
     * a gate given its name and type
     * 
     * @param name Name of the gate.
     * @param type backend.GateType ENUM class type. Available types are INPUT,
     *             OUTPUT, WIRE, AND, NAND, OR, NOR, NOT, and DFF;
     * @return Returns the created Gate. Use the returned gate in addFanIn,
     *         addFanOut, and addNextGate
     */
    public Gate addGate(String name, GateType type) {
        Gate gate = new Gate(name, type);
        if (firstGate == null)
            firstGate = gate;
        lastGate = gate;
        return gate;
    }

    /***
     * Adds the output to the gate. In a gate, the leftmost wire is the output
     * 
     * @param name the name of the wire
     * @param gate the gate you are adding the fanout to
     */
    public void addFanOut(String name, Gate gate) {
        Wire wire = wireList.get(name);
        wire.addInput(gate);
        gate.addFanOut(wire);
    }

    /***
     * Adds the inputs to the gate. In a gate, the 2nd, 3rd, 4th, etc wires are all
     * inputs
     * 
     * @param name the name of the wire
     * @param gate the gate you are adding the fanin to
     */
    public void addFanIn(String name, Gate gate) {
        Wire wire = wireList.get(name);
        wire.addOutput(gate);
        gate.addFanIn(wire);
    }

    /**
     * Add an input to the circuit registry
     * 
     * @param name the name of the input
     */
    public void addInput(String name) {
        Wire wire = new Wire(name, GateType.INPUT);
        wireList.put(name, wire);
        inputs.put(name, wire);

        // TODO Make sure this doesnt do anything
        // wire.addInput(new Gate(name, GateType.INPUT)); // PROBLEM PROBLEM PROBLEM
    }

    /**
     * Add an output wire to the circuit registry
     * 
     * @param name the name of the output
     */
    public void addOutput(String name) {
        Wire wire = new Wire(name, GateType.OUTPUT);
        wireList.put(name, wire);
        outputs.put(name, wire);
        // wire.addOutput(new Gate(name, GateType.OUTPUT));
    }

    /**
     * Add the next gate to a gate in linked list fashion (helps with parsing for
     * later operations)
     * 
     * @param previous the previous gate
     * @param next     the next gate
     */
    public void addNextGate(Gate previous, Gate next) {
        previous.nextGate = next;
    }

    /**
     * Prints the details of the circuit gates in a table format.
     * 
     * @throws IOException
     */
    void printContents(FileWriter writer) throws IOException {
        if (firstGate != null) {
            // Print the table header
            String header = String.format("%-10s %-10s %-10s %-10s %-20s %-10s %-20s %-10s%n\n",
                    "GateType", "Output", "GateLevel", "#faninN", "fanin", "#fanoutM", "fanout", "GateName");
            writer.write(header);
            // Print details of the first gate
            firstGate.printDetails(writer);

            // Iterate through the gates using the linked structure
            Gate gate = firstGate.nextGate;
            while (gate != null) {
                gate.printDetails(writer);
                gate = gate.nextGate;
            }
        } else {
            System.out.println("Circuit is empty!");
        }
    }

    // Input and output wires are not properly ordered. We will simply use the
    // string[] in/out again
    void createBuffers() {
        for (String wireName : wireList.keySet()) {
            Wire wire = wireList.get(wireName);
            Gate[] firstLastBuf = wire.createBuffers();
            if (firstLastBuf[0] != null) {
                this.lastGate.nextGate = firstLastBuf[0];
                this.lastGate = firstLastBuf[1];
            }
        }
        wireList.clear();
        /*
         * for (String wireName : inputs.keySet()) {
         * wireList.put(wireName, inputs.get(wireName));
         * }
         * for (String wireName : outputs.keySet()) {
         * wireList.put(wireName, outputs.get(wireName));
         * }
         */
    }

    /**
     * Iterate through all inputs and DFFs to calibrate gate levels
     */
    public void calculateLevels() {
        // Calibrate inputs
        Wire wire;
        // DataWrapper<Entity> fanOut_ptr;
        for (String wireName : inputs.keySet()) {
            wire = inputs.get(wireName);
            wire.calculateLevels(0, sched);
        }
        // Calibrate DFFs
        Gate gate_ptr = firstGate;
        int oldLevel;
        DataWrapper<Entity> out_ptr;
        while (gate_ptr != null) {
            if (gate_ptr.getType() == GateType.DFF) {
                oldLevel = gate_ptr.getLevel();
                gate_ptr.setLevel(0);
                gate_ptr.recordLevel(oldLevel, 0, sched);
                out_ptr = gate_ptr.fanOut;
                while (out_ptr != null) {
                    out_ptr.data.calculateLevels(1, sched);
                    out_ptr = out_ptr.next;
                }
                gate_ptr.fanOut.data.calculateLevels(1, sched);
            }
            gate_ptr = gate_ptr.nextGate;
        }
    }

    public void calibrateCircuit(FileWriter writer) throws IOException {
        // Create buffers
        long startTime = System.currentTimeMillis();

        createBuffers();

        long endTime = System.currentTimeMillis();
        System.out.println("Buffer creation took " + (endTime - startTime) + " ms");
        // Calibrate levels
        startTime = System.currentTimeMillis();

        calculateLevels();

        endTime = System.currentTimeMillis();
        System.out.println("Level calculation took " + (endTime - startTime) + " ms");

        writer.write(
                "----------------------------------------------------------------------------------------------------------\n");
        printContents(writer);
        writer.write(
                "----------------------------------------------------------------------------------------------------------\n");
    }

    /**
     * 
     * @param orderedInputs
     * @param orderedOutputs
     * @param vectors        List of vectors in format {"1", "0", "1", "0", "0"}
     * @throws IOException
     */
    // TODO actually implement filePath
    public void simulateCircuit(String[] orderedInputs, String[] orderedOutputs, String[][] vectors, FileWriter writer)
            throws IOException {
        if (orderedInputs.length != vectors[0].length) {
            System.err.println("Inputs list and vector length does not match!");
        }

        int i, j, state;
        String wireName;
        Gate gate;
        // Cycle through all vector combinations, top to bottom
        for (i = 0; i < vectors.length; i++) {
            // Assign input states
            for (j = 0; j < orderedInputs.length; j++) {
                wireName = orderedInputs[j];
                state = Integer.valueOf(vectors[i][j]);
                inputs.get(wireName).setState(state);
            }
            // Simulate circuit
            calculateStates();

            // Print inputs
            writer.write("Inputs: ");
            for (String input : vectors[i]) {
                writer.write(input);
            }
            writer.write("\n");

            // Print states
            writer.write("State: ");
            gate = firstGate;
            while (gate.getType() == GateType.DFF) {
                writer.write(String.valueOf(gate.getState()));
                gate = gate.nextGate;
            }
            writer.write("\n");

            // Print output states
            writer.write("OUTPUTS: ");
            for (j = 0; j < orderedOutputs.length; j++) {
                writer.write(String.valueOf(outputs.get(orderedOutputs[j]).getState()));
            }
            writer.write("\n");
            writer.write("\n");
        }

    }

    /**
     * Helper method to simulateCircuit that, after all input states are assigned,
     * simulates the circuit
     */
    public void calculateStates() {
        for (Integer level : sched.keySet()) {
            for (String entityName : sched.get(level).keySet()) {
                sched.get(level).get(entityName).calculateState();
            }
        }
    }

    // TODO make this
    public String createOutputFile() {
        return "filepath";
    }

    /**
     * The "main" method of Circuit class. It calibrates the circuit then simulates
     * it
     * 
     * @param inputs  A string array of inputs format {"G0","G1", etc}. Used to
     *                assign vector values in the proper order
     * @param outputs A string array of outputs format {"G17", etc}. Used to print
     *                out output values in the proper order
     * @param vectors A string double array format {VECTOR1[], VECTOR2[], etc}.
     *                VECTOR1, VECTOR2, etc use format {"1","0","0",etc}
     */
    public void mainMethod(String[] inputs, String[] outputs, String[][] vectors, String filePath) {
        String fileName = extractBetween(filePath) + "_simdata.txt";
        FileWriter writer;
        try {
            writer = new FileWriter(fileName);
            calibrateCircuit(writer);
            simulateCircuit(inputs, outputs, vectors, writer);
            writer.close();
        } catch (IOException e) {
            System.out.println("Error with writer");
            e.printStackTrace();
        }

    }

    private static String extractBetween(String input) {
        // Find the last occurrence of '/' and the first occurrence of '.'
        int start = input.lastIndexOf('/') + 1; // Start right after the last '/'
        int end = input.indexOf('.', start); // Find '.' after the start index

        if (start == -1)
            start = 0;

        // Extract and validate the substring
        if (end > start) { // Ensure valid positions
            return input.substring(start, end);
        } else {
            return "Invalid format";
        }
    }

    /**
     * Converts string "dff" and "nor" etc into GateType format
     * 
     * @param type the desired string type to be changed-
     *             does not work with spaces!
     */
    public static GateType parseType(String type) {
        return GateType.readType(type);
    }

    /**
     * Parses an array of input wire names
     * (will automatically add to wire list and input list)
     * 
     * @param inputs String array of inputs, format ["G0","G1", etc] NO spaces
     */
    public void parseInputs(String[] inputs) {
        for (String wire : inputs) {
            addInput(wire);
        }
    }

    /**
     * Parses an array of output wire names
     * (will automatically add to wire list and output list)
     * 
     * @param inputs String array of inputs, format ["G17","G18", etc] NO spaces
     *               also works for an array with only one entry
     */
    public void parseOutputs(String[] outputs) {
        for (String wire : outputs) {
            addOutput(wire);
        }
    }

    /**
     * Parses an array of wires
     * 
     * @param wires String array of wires, format ["G5",G6",etc] NO spaces
     */
    public void parseWires(String[] wires) {
        for (String wire : wires) {
            addWire(wire);
        }
    }

    /**
     * Parses a single gate's info and returns the address of the created gate
     * 
     * @param gateData String array for a single gate
     *                 format [type, name, output, input1, input2, etc] NO spaces
     * @return
     */
    public Gate parseSingleGate(String[] gateData) {
        int i;
        Gate gate = null;
        for (i = 1; i < gateData.length; i++) {
            switch (i) {
                case 1:
                    gate = addGate(gateData[1], parseType(gateData[0]));
                    break;
                case 2:
                    addFanOut(gateData[2], gate);
                    break;
                default:
                    addFanIn(gateData[i], gate);
                    break;
            }
        }
        return gate;
    }
}