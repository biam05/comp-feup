import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GrammarSymbolTable implements SymbolTable {

    private final List<String> imports = new ArrayList<>();
    private final List<Symbol> classFields = new ArrayList<>();
    private final List<SymbolMethod> methods = new ArrayList<>();
    private String className = "";
    private String superExtends = "";

    public void setSuperExtends(String superExtends) {
        this.superExtends = superExtends;
    }

    public void addImport(String importName) {
        this.imports.add(importName);
    }

    public void addClassField(Symbol classField) {
        this.classFields.add(classField);
    }

    public void addMethod(SymbolMethod method) {
        this.methods.add(method);
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String getSuper() {
        return superExtends;
    }

    @Override
    public List<Symbol> getFields() {
        return classFields;
    }

    @Override
    public List<String> getMethods() {
        List<String> methodsNames = new ArrayList<>();

        for (SymbolMethod method : methods) {
            methodsNames.add(method.getName());
        }

        return methodsNames;
    }

    private List<String> parseMethodInfo(String info) {
        List<String> list = new ArrayList<>();

        String[] firstParse = info.split("\\(");
        list.add(firstParse[0]); //method name

        String[] types = firstParse[1].replace(")", "").split(","); //parameters type
        list.addAll(Arrays.asList(types));

        return list;
    }

    public SymbolMethod getMethodByInfo(String methodInfo) {
        List<String> info = parseMethodInfo(methodInfo);

        for (SymbolMethod method : this.methods) {
            if (method.equalsMethod(info)) return method;
        }

        return null;
    }

    @Override
    public Type getReturnType(String methodName) { //methodName(returnP[],returnP,returnP,....)
        SymbolMethod method = getMethodByInfo(methodName);
        if (method == null) return null;
        return method.getReturnType();
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        SymbolMethod method = getMethodByInfo(methodName);
        if (method == null) return null;
        return method.getParameters();
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        SymbolMethod method = getMethodByInfo(methodName);
        if (method == null) return null;
        return method.getLocalVariables();
    }

    public Type returnFieldTypeIfExists(String field) {
        for (Symbol symbol : classFields) {
            if (symbol.getName().equals(field)) return symbol.getType();
        }
        return null;
    }
}
