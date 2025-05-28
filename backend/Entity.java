package backend;

import java.util.HashMap;

public class Entity {

    String name;
    GateType type;
    DataWrapper<Entity> fanIn, fanOut;
    int state;
    int level;

    Entity(String name, GateType type) {
        this.name = name;
        this.type = type;
        this.fanIn = null;
        this.fanOut = null;
        this.level = -1;
        this.state = 4;
    }

    public String printClass() {
        return "Entity";
    }

    @Override
    public String toString() {
        return name;
    }

    String getName() {
        return this.name;
    }

    GateType getType() {
        return this.type;
    }

    int getLevel() {
        return level;
    }

    int getState() {
        return this.state;
    }

    protected void setTheState(int state) {
        this.state = state;
    }

    void setLevel(int level) {
        this.level = level;
    }

    DataWrapper<Entity> getFanIn() {
        return fanIn;
    }

    protected void setFanIn(DataWrapper<Entity> gate) {
        fanIn = gate;
    }

    protected void setFanOut(DataWrapper<Entity> gate) {
        fanOut = gate;
    }

    DataWrapper<Entity> getFanOut() {
        return fanOut;
    }

    DataWrapper<Entity> deleteInput(Entity data) {
        return deleteFromList(data, fanIn);
    }

    DataWrapper<Entity> deleteOutput(Entity data) {
        return deleteFromList(data, fanOut);
    }

    private DataWrapper<Entity> deleteFromList(Entity data, DataWrapper<Entity> list) {
        // Check to see if list is single entry
        if (list.next == null) {
            // Delete single entry
            if (list.data == data) {
                return null;
                // Data not in single entry
            } else {
                return list;
            }
            // Multiple entries in list
        } else {
            DataWrapper<Entity> first = list;
            DataWrapper<Entity> ptr = first;
            // Check if item in first entry
            if (list.data == data) {
                return list.next;
            }
            // iterate to data entry before target (or to END of list)
            while (ptr.next != null && ptr.next.data != data) {
                ptr = ptr.next;
            }
            // Check to see if next entry is the target (otherwise next entry does not
            // exist)
            if (ptr.next != null && ptr.next.data == data) {
                if (ptr.next.next != null) {
                    ptr.next = ptr.next.next;
                } else {
                    ptr.next = null;
                }
            }
            return first;
        }
    }

    /**
     * 
     * @param level
     * @param sched         hashmap that organizes each entity by level
     */
    void calculateLevels(int newLevel, HashMap<Integer, HashMap<String, Entity>> sched) {
        // If we reach a flip flop, stop and do not calibrate
        if (this.type == GateType.DFF)
            return;
        // If entity's max level is lower its new level, calibrate and traverse outputs
        if (this.level < newLevel) {
            int oldLevel = this.level;
            this.level = newLevel;
            recordLevel(oldLevel, newLevel, sched);
            DataWrapper<Entity> ptr = this.fanOut;
            while (ptr != null) {
                if (ptr.data != null)
                    ptr.data.calculateLevels(newLevel + 1, sched);
                ptr = ptr.next;
            }
        }
    }

    /**
     * Helper method for calculateLevels that checks to see if the entity is already
     * logged into the sched data set,
     * and updates the value accordingly
     * 
     * @param oldLevel 
     * @param newLevel the 
     * @param sched the scheduling database
     */
    void recordLevel(int oldLevel, int newLevel, HashMap<Integer, HashMap<String, Entity>> sched) {
        // If the entity was previously logged into sched, remove it
        if (sched.containsKey(oldLevel) && sched.get(oldLevel).containsKey(this.name)) {
            sched.get(oldLevel).remove(this.name);
        }
        // If sched does not have a map for current level, create it
        if (!sched.containsKey(newLevel)) {
            sched.put(Integer.valueOf(newLevel), new HashMap<>(100));
        }
        // Add entity to new spot in sched
        sched.get(newLevel).put(this.name, this);
    }

    void calculateState() {
        if (this.type == GateType.INPUT || this.fanIn == null) {
            return;
        }
        switch (this.type) {
            case DFF:
                state = fanIn.data.getState();
                break;
            case BUF:
                state = fanIn.data.getState();
                break;
            case OUTPUT:
                state = fanIn.data.getState();
                break;
            case AND:
                runAND();
                break;
            case NAND:
                runNAND();
                break;
            case OR:
                runOR();
                break;
            case NOR:
                runNOR();
                break;
            case NOT:
                this.state = calcNOT(fanIn.data.getState());
                break;
            default:
                break;
        }
    }

    void runAND() {
        int lastCalc; // holds 'sum' of last 2 inputs
        DataWrapper<Entity> ptr = fanIn;
        lastCalc = ptr.data.getState();
        ptr = ptr.next;
        while (ptr != null) {
            lastCalc = calcAND(lastCalc, ptr.data.getState());
            ptr = ptr.next;
        }
        this.state = lastCalc;
    }

    void runNAND() {
        int lastCalc; // holds 'sum' of last 2 inputs
        DataWrapper<Entity> ptr = fanIn;
        lastCalc = ptr.data.getState();
        ptr = ptr.next;
        while (ptr != null) {
            lastCalc = calcAND(lastCalc, ptr.data.getState());
            ptr = ptr.next;
        }
        this.state = calcNOT(lastCalc);
    }

    void runOR() {
        int lastCalc; // holds 'sum' of last 2 inputs
        DataWrapper<Entity> ptr = fanIn;
        lastCalc = ptr.data.getState();
        ptr = ptr.next;
        while (ptr != null) {
            lastCalc = calcOR(lastCalc, ptr.data.getState());
            ptr = ptr.next;
        }
        this.state = lastCalc;
    }

    void runNOR() {
        int lastCalc; // holds 'sum' of last 2 inputs
        DataWrapper<Entity> ptr = fanIn;
        lastCalc = ptr.data.getState();
        ptr = ptr.next;
        while (ptr != null) {
            lastCalc = calcOR(lastCalc, ptr.data.getState());
            ptr = ptr.next;
        }
        this.state = calcNOT(lastCalc);
    }

    int calcAND(int x, int y) {
        if (x == 0 || y == 0)
            return 0;
        if (x == 1 && y == 1)
            return 1;
        return 4;
    }

    int calcOR(int x, int y) {
        if (x == 1 || y == 1)
            return 1;
        if (x == 0 && y == 0)
            return 0;
        return 4;
    }

    int calcNOT(int x) {
        if (x == 1)
            return 0;
        if (x == 0)
            return 1;
        return 4;
    }

}
