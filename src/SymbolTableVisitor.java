import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.*;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class SymbolTableVisitor extends PreorderJmmVisitor<Boolean, Boolean> {
    private final GrammarSymbolTable symbolTable;
    private final List<Report> reports;

    public SymbolTableVisitor() {
        this.symbolTable = new GrammarSymbolTable();
        this.reports = new ArrayList<>();
        addVisit("ClassDeclaration", this::visitClass);
        addVisit("ImportDeclaration", this::visitImport);
        addVisit("MethodDeclaration", this::visitMethod);
    }

    public Boolean visitClass(JmmNode node, Boolean dummy) {
        System.out.println("Class: " + node);

        SymbolClass symbolClass = new SymbolClass();

        List<JmmNode> children = node.getChildren();

        for (int i = 0; i < children.size(); i++) {
            JmmNode child = children.get(i);
            String childKind = child.getKind();
            if (childKind.contains("Identifier")) {
                String name = childKind.replaceAll("'", "").replace("Identifier ", "");
                symbolClass.setName(name);
            }
            else if (childKind.equals("{")) { // attributes
                List<JmmNode> attributes_methods = new ArrayList<>();
                while (true) {
                    i++;
                    JmmNode aux = children.get(i);
                    if (aux.getKind().equals("}")) break;
                    attributes_methods.add(children.get(i));
                }
                getClassAttributesAndMethods(symbolClass, attributes_methods);
            }
        }
        this.symbolTable.addClassField(symbolClass);

        return true;
    }

    public void getClassAttributesAndMethods(SymbolClass symbolClass, List<JmmNode> nodes) {

        if ((nodes.size() == 0)) return;

        for (int i = 0; i < nodes.size(); i++) {
            JmmNode node = nodes.get(i);
            if (node.getKind().equals("VarDeclaration")) {
                Symbol symbol = parseVarDeclaration(node);
                symbolClass.addAttribute(symbol);
            }
            else
            {
                visitMethod(node, true);
            }
        }
    }

    public Boolean visitImport(JmmNode node, Boolean dummy) {
        System.out.println("Import: " + node);

        SymbolImport symbolImport = new SymbolImport();

        List<JmmNode> children = node.getChildren();

        for (JmmNode child : children) {
            String childKind = child.getKind();
            if (childKind.contains("Identifier")) {
                String name = childKind.replaceAll("'", "").replace("Identifier ", "");
                symbolImport.setName(name);
            }
        }
        this.symbolTable.addImport(symbolImport);
        return true;
    }

    public Boolean visitMethod(JmmNode node, Boolean dummy) {
        System.out.println("Method: " + node);

        SymbolMethod method = new SymbolMethod();

        List<JmmNode> children = node.getChildren();

        Boolean alreadyInBody = false;

        for (int i = 0; i < children.size(); i++) {

            JmmNode child = children.get(i);
            String childKind = child.getKind();

            if (childKind.equals("Type") || childKind.equals("void")) method.setReturnType(new Type(child)); // return type

            else if (childKind.equals("(")) { // parameters
                if (alreadyInBody) break;
                List<JmmNode> parameters = new ArrayList<>();
                while (true) {
                    i++;
                    JmmNode aux = children.get(i);
                    if (aux.getKind().equals(")")) break;
                    parameters.add(children.get(i));
                }
                getMethodParameters(method, parameters);
            }

            else if(childKind.equals("MethodBody")) { //method body (local variables)
                visitMethodBody(method, child);
                alreadyInBody = true;
            }

            else if (childKind.contains("Identifier") || childKind.equals("main")) {
                String name = childKind.replaceAll("'", "").replace("Identifier ", "");
                method.setName(name);
            }

        }

        this.symbolTable.addMethod(method);
        return true;
    }


    public void getMethodParameters(SymbolMethod method, List<JmmNode> nodes) {

        if ((nodes.size() == 0) || (nodes.size() % 2 != 0)) return;

        System.out.println("Method Parameters!");

        for (int i = 0; i < nodes.size(); i++) {
            JmmNode nodeType = nodes.get(i++);
            JmmNode nodeName = nodes.get(i);
            Symbol symbol = new Symbol(nodeType, nodeName);
            method.addParameter(symbol);
        }
    }

    public void visitMethodBody(SymbolMethod method, JmmNode node) {
        System.out.println("Method Body!");

        List<JmmNode> children = node.getChildren();

        for (JmmNode child : children) {
            if (child.getKind().equals("VarDeclaration")) {
                Symbol localVariable = parseVarDeclaration(child);
                method.addLocalVariables(localVariable);
            }
        }
    }

    public Symbol parseVarDeclaration(JmmNode node) {
        List<JmmNode> children = node.getChildren();
        if (children.size() < 2) return null;

        JmmNode type = children.get(0);
        JmmNode name = children.get(1);

        return new Symbol(type, name);
    }
}
