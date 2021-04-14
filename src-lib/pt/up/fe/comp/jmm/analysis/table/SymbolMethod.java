package pt.up.fe.comp.jmm.analysis.table;

import java.util.*;

public class SymbolMethod {

    private String name;
    private Type returnType;
    private final List<Symbol> parameters;
    private final List<Symbol> localVariables;

    public SymbolMethod(String name, Type returnType, List<Symbol> parameters, List<Symbol> localVariables) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
        this.localVariables = localVariables;
    }

    public SymbolMethod() {
        this.parameters = new ArrayList<>();
        this.localVariables = new ArrayList<>();
    }

    public void setName(String name) {
        System.out.println("Name: " + name);
        this.name = name;
    }

    public void setReturnType(Type returnType) {
        System.out.println("Return type: " + returnType);
        this.returnType = returnType;
    }

    public void addParameter(Symbol parameter) {
        System.out.println("Parameter: " + parameter);
        this.parameters.add(parameter);
    }

    public void addLocalVariables(Symbol localVariable) {
        System.out.println("LocalVariable: " + localVariable);
        this.localVariables.add(localVariable);
    }

    public String getName() {
        return name;
    }

    public Type getReturnType() {
        return returnType;
    }

    public Type getVariableType(String name) {
        for(Symbol symbol: localVariables){
            if(symbol.getName().equals(name))
                return symbol.getType();
        }
        return null;
    }

    public List<Symbol> getParameters() {
        return parameters;
    }

    public List<Symbol> getLocalVariables() {
        return localVariables;
    }

    public boolean equalsMethod(List<String> info) {
        if(info == null || info.size() < 1) return false;

        if((info.size() - 1) != this.parameters.size()) return false;

        String name = info.get(0);
        if(!name.equals(this.name)) return false;

        for(int i = 1; i < info.size(); i++) {
            Type type = createType(info.get(i));
            if(!type.equals(this.parameters.get(i).getType())) return false;
        }

        return true;
    }

    private Type createType(String t) {

        boolean isArray = t.contains("[]");
        String type = t.replace("[]", "");

        return new Type(type, isArray);
    }
}
