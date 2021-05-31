package symbolTable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GrammarSymbolTable implements SymbolTable {

    private final List<String> imports = new ArrayList<>();
    private final List<GrammarSymbol> classFields = new ArrayList<>();
    private final List<GrammarMethod> methods = new ArrayList<>();
    private String className = "";
    private String superExtends = "";

    public void setSuperExtends(String superExtends) {
        this.superExtends = superExtends;
    }

    public void addImport(String importName) {
        this.imports.add(importName);
    }

    public void addClassField(GrammarSymbol classField) {
        this.classFields.add(classField);
    }

    public void addMethod(GrammarMethod method) {
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

    public List<GrammarSymbol> getGrammarFields() {
        return classFields;
    }

    @Override
    public List<Symbol> getFields() {
        return new ArrayList<>(classFields);
    }

    @Override
    public List<String> getMethods() {
        List<String> methodsNames = new ArrayList<>();

        for (GrammarMethod method : methods) {
            methodsNames.add(method.getName());
        }

        return methodsNames;
    }

    public List<GrammarMethod> methods() {
        return methods;
    }

    private List<String> parseMethodInfo(String info) {
        List<String> list = new ArrayList<>();

        int indexBeg = info.indexOf('(');
        list.add(info.substring(0, indexBeg));

        int indexEnd = info.indexOf(')');
        String param = info.substring(indexBeg + 1, indexEnd).trim();

        if (!param.equals("")) {
            String[] params = param.split(",");
            list.addAll(Arrays.asList(params));
        }

        return list;
    }

    public GrammarMethod getMethodByInfo(String methodInfo) {
        List<String> info = parseMethodInfo(methodInfo);

        for (GrammarMethod method : this.methods)
            if (method.equalsMethod(info)) return method;

        return null;
    }

    @Override
    public GrammarType getReturnType(String methodName) { //methodName(returnP[],returnP,returnP,....)
        GrammarMethod method = getMethodByInfo(methodName);
        if (method == null) return null;
        return method.getReturnType();
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        return new ArrayList<>(getGrammarParameters(methodName));
    }

    public List<GrammarSymbol> getGrammarParameters(String methodName) {
        GrammarMethod method = getMethodByInfo(methodName);
        if (method == null) return null;
        return method.getParameters();
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        return new ArrayList<>(getGrammarLocalVariables(methodName));
    }

    public List<GrammarSymbol> getGrammarLocalVariables(String methodName) {
        GrammarMethod method = getMethodByInfo(methodName);
        if (method == null) return null;
        return method.getLocalVariables();
    }

    public GrammarType returnFieldTypeIfExists(String field) {
        for (GrammarSymbol symbol : classFields)
            if (symbol.getName().equals(field)) return symbol.getGrammarType();
        return null;
    }

    public GrammarType hasImport(String identifierName) {
        for (String importName : getImports()) {
            String[] imports = importName.split("\\.");
            if (imports[imports.length - 1].equals(identifierName)) return new GrammarType("Accepted", false);
        }
        return null;
    }

    public Boolean hasMethod(String methodInfo) {
        return getMethodByInfo(methodInfo) != null;
    }

    @Override
    public String print() {
        var builder = new StringBuilder();

        builder.append("Class: " + getClassName() + "\n");
        var superClass = getSuper() != null ? getSuper() : "java.lang.Object";
        builder.append("Super: " + superClass + "\n");
        builder.append("\nImports:");
        var imports = getImports();

        if (imports.isEmpty()) {
            builder.append(" <no imports>\n");
        } else {
            builder.append("\n");
            imports.forEach(fullImport -> builder.append(" - " + fullImport + "\n"));
        }

        var fields = getFields();
        builder.append("\nFields:");
        if (fields.isEmpty()) {
            builder.append(" <no fields>\n");
        } else {
            builder.append("\n");
            fields.forEach(field -> builder.append(" - " + field.print() + "\n"));
        }

        var methods = methods();
        builder.append("\nMethods: " + methods.size() + "\n");

        for (var method : methods) {
            var returnType = method.getReturnType();
            var params = method.getParameters();
            builder.append(" - " + returnType.print() + " " + method + "(");
            var paramsString = params.stream().map(param -> param != null ? param.print() : "<null param>")
                    .collect(Collectors.joining(", "));
            builder.append(paramsString + ")\n");
        }

        return builder.toString();
    }

}
