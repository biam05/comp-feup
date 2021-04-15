package pt.up.fe.comp.jmm.analysis.table;

import java.util.*;

public class SymbolClass {
    private String name;
    private final List<Symbol> attributes;

    public SymbolClass() {
        this.attributes = new ArrayList<>();
    }

    public SymbolClass(String name, List<Symbol> attributes) {
        this.name = name;
        this.attributes = attributes;
    }

    public void setName(String name) {
        System.out.println("Name: " + name);
        this.name = name;
    }

    public void addAttribute(Symbol attribute) {
        System.out.println("Attribute: " + attribute);
        this.attributes.add(attribute);
    }

    public String getName() {
        return name;
    }

    public List<Symbol> getAttributes() {
        return attributes;
    }
}
