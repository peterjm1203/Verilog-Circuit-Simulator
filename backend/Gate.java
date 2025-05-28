package backend;

import java.io.FileWriter;
import java.io.IOException;

public class Gate extends Entity {

    String name;
    Gate nextGate;

    Gate(String name, GateType type) {
        super(name, type);
    }

    @Override
    public String printClass() {
        return "Gate";
    }

    void setNextGate(Gate gate) {
        this.nextGate = gate;
    }

    Gate getNexGate() {
        return nextGate;
    }

    // Format is OUTPUT, inputs<-->
    void addFanIn(Entity entity) {
        if (fanIn != null) {
            fanIn.add(entity);
        } else {
            fanIn = new DataWrapper<>(entity);
        }
    }

    void addFanOut(Entity entity) {
        if (fanOut != null) {
            fanOut.add(entity);
        } else {
            fanOut = new DataWrapper<>(entity);
        }
    }

    void printDetails(FileWriter writer) throws IOException {
        // Handle null safety for fanIn and fanOut
        int fanInCount = (fanIn != null) ? fanIn.count() : 0;
        String fanInWires = (fanIn != null) ? fanIn.toString() : "N/A";
        int fanOutCount = (fanOut != null) ? fanOut.count() : 0;
        String fanOutWires = (fanOut != null) ? fanOut.toString() : "N/A";

        // Print the gate details in the specified format
        String data = String.format("%-10s %-10s %-10d %-10d %-20s %-10d %-20s %-10s%n\n",
                GateType.readType(type), // Gate type
                (fanOut != null && fanOut.data != null) ? fanOut.data.toString() : "N/A", // Output wire
                level, // Gate level
                fanInCount, // Fan-in count
                fanInWires, // Fan-in wires
                fanOutCount, // Fan-out count
                fanOutWires, // Fan-out wires
                getName() // Gate name
        );

        writer.write(data);
    }
}
