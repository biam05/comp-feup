import pt.up.fe.comp.jmm.analysis.table.*;


import java.util.ArrayList;
import java.util.List;

public class GrammarSymbolTable implements SymbolTable {

    private final List<SymbolImport> imports = new ArrayList<>();
    private String className;
    private String superExtends;
    private final List<SymbolClass> classFields = new ArrayList<>();
    private final List<SymbolMethod> methods = new ArrayList<>();

    public void setClassName(String className) {
        this.className = className;
    }

    public void setSuperExtends(String superExtends) {
        this.superExtends = superExtends;
    }

    public void addImport(SymbolImport importName) {
        this.imports.add(importName);
    }

    public void addClassField(SymbolClass classField) {
        this.classFields.add(classField);
    }

    public void addMethod(SymbolMethod method) {
        this.methods.add(method);
    }

    @Override
    public List<String> getImports() {
        return null;
    }

    @Override
    public String getClassName() {
        return null;
    }

    @Override
    public String getSuper() {
        return null;
    }

    @Override
    public List<Symbol> getFields() {
        return null;
    }

    @Override
    public List<String> getMethods() {
        return null;
    }

    @Override
    public Type getReturnType(String methodName) {
        return null;
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        return null;
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        return null;
    }
}
