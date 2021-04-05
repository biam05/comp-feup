package pt.up.fe.comp.jmm.analysis.table;

import java.util.*;

public class SymbolClass {
    private String name;
    private final List<Symbol> attributes;
    private final List<Symbol> methods;

    public SymbolClass() {
        this.attributes = new ArrayList<>();
        this.methods = new ArrayList<>();
    }

    public SymbolClass(String name, ArrayList<Symbol> attributes, ArrayList<Symbol> methods) {
        this.name = name;
        this.attributes = attributes;
        this.methods = methods;

    }

    public void setName(String name) {
        System.out.println("Name: " + name);
        this.name = name;
    }

    public void addAttribute(Symbol attribute) {
        System.out.println("Attribute: " + attribute);
        this.attributes.add(attribute);
    }

    public void addMethod(Symbol method) {
        System.out.println("Method: " + method);
        this.methods.add(method);
    }

    public String getName() {
        return name;
    }

    public List<Symbol> getAttributes() {
        return attributes;
    }

    public List<Symbol> getMethods() {
        return methods;
    }
}
