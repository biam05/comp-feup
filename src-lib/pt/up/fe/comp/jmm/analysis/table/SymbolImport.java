package pt.up.fe.comp.jmm.analysis.table;

public class SymbolImport {
    private String name;

    public SymbolImport(String name) {
        this.name = name;
    }

    public SymbolImport() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        System.out.println("Name: " + name);
        this.name = name;
    }
}
