package symbolTable;

import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GrammarMethod {

    private final List<GrammarSymbol> parameters;
    private final HashMap<GrammarSymbol, Integer> localVariables;
    private String name;
    private GrammarType returnType;

    public GrammarMethod() {
        this.parameters = new ArrayList<>();
        this.localVariables = new HashMap<>();
    }

    public void addParameter(GrammarSymbol parameter) {
        this.parameters.add(parameter);
    }

    public void updateLocalVariable(GrammarSymbol localVariable, Integer value) {
        this.localVariables.put(localVariable, value);
    }

    public Integer getLocalVariable(String localVariable) {
        return this.localVariables.get(new GrammarSymbol(new GrammarType("Int", false), localVariable));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GrammarType getReturnType() {
        return returnType;
    }

    public void setReturnType(GrammarType returnType) {
        this.returnType = returnType;
    }

    public boolean hasVariable(GrammarSymbol symbol) {
        return returnTypeIfExists(symbol.getName()) != null;
    }

    public boolean isMain() {
        return name.equals("main");
    }

    public GrammarType returnTypeIfExists(String name) {
        for (GrammarSymbol symbol : localVariables.keySet())
            if (symbol.getName().equals(name)) return symbol.getGrammarType();
        for (GrammarSymbol symbol : parameters)
            if (symbol.getName().equals(name)) return symbol.getGrammarType();
        return null;
    }

    public List<GrammarSymbol> getParameters() {
        return parameters;
    }

    public List<GrammarSymbol> getLocalVariables() {
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

    private GrammarType createType(String t) {

        boolean isArray = t.contains("[]");
        String type = t.replace("[]", "");

        return new GrammarType(type, isArray);
    }

    @Override
    public String toString() {
        return ("Name: " + name + ", return: " + returnType + ", parameters: " + parameters);
    }

    public int getParameterOrder(String parameter) {
        int counter = 1;
        for (GrammarSymbol s : parameters) {
            if (s.getName().equals(parameter)) return counter;
            counter++;
        }
        return 0;
    }
}
