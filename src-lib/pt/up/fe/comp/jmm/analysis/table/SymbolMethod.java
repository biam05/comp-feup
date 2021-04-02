package pt.up.fe.comp.jmm.analysis.table;

import java.util.List;

public class SymbolMethod {

    private String name;
    private Type returnType;
    private List<Symbol> parameters;
    private List<Symbol> localVariables;

    public SymbolMethod(String name, Type returnType, List<Symbol> parameters, List<Symbol> localVariables) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
        this.localVariables = localVariables;
    }

    public String getName() {
        return name;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Symbol> getParameters() {
        return parameters;
    }

    public List<Symbol> getLocalVariables() {
        return localVariables;
    }
}
