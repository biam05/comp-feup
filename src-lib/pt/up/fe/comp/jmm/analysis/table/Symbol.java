package pt.up.fe.comp.jmm.analysis.table;

import pt.up.fe.comp.jmm.JmmNode;

public class Symbol {
    private final Type type;
    private final String name;

    public Symbol(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    public Symbol(JmmNode type, JmmNode name) {
        this.type = new Type(type);
        this.name = name.getKind().replaceAll("'", "").replace("Identifier ", "");
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Symbol [type=" + type + ", name=" + name + "]";
    }

}
