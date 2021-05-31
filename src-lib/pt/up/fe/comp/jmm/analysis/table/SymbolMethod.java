package pt.up.fe.comp.jmm.analysis.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SymbolMethod {

    private final List<Symbol> parameters;
    private final HashMap<Symbol, Integer> localVariables;
    private String name;
    private Type returnType;

    public SymbolMethod() {
        this.parameters = new ArrayList<>();
        this.localVariables = new HashMap<>();
    }

    public void addParameter(Symbol parameter) {
        this.parameters.add(parameter);
    }

    public void updateLocalVariable(Symbol localVariable, Integer value) {
        this.localVariables.put(localVariable, value);
    }

    public Integer getLocalVariable(String localVariable) {
        return this.localVariables.get(new Symbol(new Type("Int", false), localVariable));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public boolean hasVariable(Symbol symbol) {
        return returnTypeIfExists(symbol.getName()) != null;
    }

    public Type returnTypeIfExists(String name) {
        for (Symbol symbol : localVariables.keySet())
            if (symbol.getName().equals(name)) return symbol.getType();
        for (Symbol symbol : parameters)
            if (symbol.getName().equals(name)) return symbol.getType();
        return null;
    }

    public List<Symbol> getParameters() {
        return parameters;
    }

    public List<Symbol> getLocalVariables() {
        return new ArrayList<>(localVariables.keySet());
    }

    public boolean equalsMethod(List<String> info) {
        if (info == null || info.size() < 1) return false;

        if ((info.size() - 1) != this.parameters.size()) return false;

        String name = info.get(0);
        if (!name.equals(this.name)) return false;

        int n_param = 0;
        for (int i = 1; i < info.size(); i++) {
            Type type = createType(info.get(i));
            if (!type.equals(this.parameters.get(n_param).getType())) return false;
            n_param++;
        }

        return true;
    }

    private Type createType(String t) {

        boolean isArray = t.contains("[]");
        String type = t.replace("[]", "");

        return new Type(type, isArray);
    }

    @Override
    public String toString() {
        return ("Name: " + name + ", return: " + returnType + ", parameters: " + parameters);
    }

    public int getParameterOrder(String parameter) {
        int counter = 1;
        for (Symbol s : parameters) {
            if (s.getName().equals(parameter)) return counter;
            counter++;
        }
        return 0;
    }
}
