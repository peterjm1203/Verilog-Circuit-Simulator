package backend;

public enum GateType {
    INPUT, OUTPUT, WIRE, AND, NAND, OR, NOR, NOT, DFF, BUF;

    static String readType(GateType type){
        switch (type) {
            case INPUT:
                return "INPUT";

            case OUTPUT:
                return "OUTPUT";

            case WIRE:
                return "WIRE";

            case AND:
                return "AND"; 

            case NAND:
                return "NAND";

            case OR:
                return "OR";

            case NOR:
                return "NOR";

            case NOT:
                return "NOT";

            case DFF:
                return "DFF";

            case BUF:
                return "BUF";

            default:
            System.err.println("Invalid gate type!");
            return "";
        }
    }

    static GateType readType(String type){
        switch (type) {
            case "input":
                return INPUT;

            case "output":
                return OUTPUT;

            case "wire":
                return WIRE;

            case "and":
                return AND; 

            case "nand":
                return NAND;

            case "or":
                return OR;

            case "nor":
                return NOR;

            case "not":
                return NOT;

            case "dff":
                return DFF;

            case "buf":
                return BUF;

            default:
                System.err.println("Tried to parse invalid string!");
                return null;
        }
    }
}
