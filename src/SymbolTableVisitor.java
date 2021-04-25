import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

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

    public GrammarSymbolTable getSymbolTable() {
        return symbolTable;
    }

    public List<Report> getReports() {
        return reports;
    }

    public Boolean visitClass(JmmNode node, Boolean dummy) {

        List<JmmNode> children = node.getChildren();

        Boolean classNameSet = false;
        for (int i = 0; i < children.size(); i++) {
            JmmNode child = children.get(i);
            String childKind = child.getKind();
            if (childKind.contains("Identifier") && !classNameSet) {
                String name = childKind.replaceAll("'", "").replace("Identifier ", "");
                symbolTable.setClassName(name);
                classNameSet = true;
            } else if (childKind.contains("Identifier")) {
                String name = childKind.replaceAll("'", "").replace("Identifier ", "");
                symbolTable.setSuperExtends(name);
            } else if (childKind.equals("LBrace")) {
                while (true) {
                    i++;
                    JmmNode aux = children.get(i);
                    if (aux.getKind().equals("RBrace")) break;
                    else if (aux.getKind().equals("MethodDeclaration")) continue;
                    Symbol localVariable = parseVarDeclaration(aux);
                    if (localVariable != null) symbolTable.addClassField(localVariable);
                }
            }
        }
        return true;
    }

    public Boolean visitImport(JmmNode node, Boolean dummy) {

        List<JmmNode> children = node.getChildren();

        for (int i = 0; i < children.size(); i++) {
            StringBuilder importName = null;

            int counter = 1;
            if (children.get(i).getKind().equals("Import")) {
                importName = new StringBuilder(children.get(i + counter).getKind().replaceAll("'", "").replace("Identifier ", ""));

                while (true) {
                    counter++;
                    String kind = children.get(i + counter).getKind();
                    if (kind.equals("Semicolon")) break;
                    if (kind.equals("Period")) continue;
                    if (kind.contains("Identifier"))
                        importName.append(".").append(kind.replaceAll("'", "").replace("Identifier ", ""));
                }

                i += counter;
            }

            if (importName != null)
                this.symbolTable.addImport(importName.toString());
        }
        return true;
    }

    public Boolean visitMethod(JmmNode node, Boolean dummy) {

        SymbolMethod method = new SymbolMethod();

        List<JmmNode> children = node.getChildren();

        boolean alreadyInBody = false;

        for (int i = 0; i < children.size(); i++) {

            JmmNode child = children.get(i);
            String childKind = child.getKind();

            if (childKind.equals("Type") || childKind.equals("Void"))
                method.setReturnType(new Type(child)); // return type

            else if (childKind.equals("LParenthesis")) { // parameters
                if (alreadyInBody) break;

                List<JmmNode> parameters = new ArrayList<>();
                while (true) {
                    i++;
                    JmmNode aux = children.get(i);
                    if (aux.getKind().equals("RParenthesis")) break;
                    parameters.add(children.get(i));
                }
                getMethodParameters(method, parameters);
            } else if (childKind.equals("MethodBody")) {
                visitMethodBody(method, child);
                alreadyInBody = true;
            } else if (childKind.contains("Identifier") || childKind.equals("Main")) {
                String name = childKind.replaceAll("'", "").replace("Identifier ", "");
                method.setName(name);
            }
        }
        this.symbolTable.addMethod(method);

        return true;
    }

    public void visitMethodBody(SymbolMethod method, JmmNode node) {
        List<JmmNode> children = node.getChildren();

        for (JmmNode child : children) {
            if (child.getKind().equals("VarDeclaration")) {
                Symbol localVariable = parseVarDeclaration(child);
                if (localVariable != null) method.addLocalVariables(localVariable);
                else
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.get("line")), Integer.parseInt(child.get("col")), "type is not valid"));
            }
        }
    }


    public void getMethodParameters(SymbolMethod method, List<JmmNode> nodes) {

        if ((nodes.size() == 0) || (nodes.size() % 2 != 0)) return;

        for (int i = 0; i < nodes.size(); i++) {
            JmmNode nodeType = nodes.get(i++);
            JmmNode nodeName = nodes.get(i);
            Symbol symbol = new Symbol(nodeType, nodeName);
            method.addParameter(symbol);
        }
    }

    public Symbol parseVarDeclaration(JmmNode node) {
        List<JmmNode> children = node.getChildren();
        if (children.size() < 2) return null;

        JmmNode type = children.get(0);
        JmmNode name = children.get(1);

        Symbol symbol = new Symbol(type, name);

        return checkValidType(symbol);
    }

    private Symbol checkValidType(Symbol symbol) {
        String type = symbol.getType().getName();

        if (type.equals("String") || type.equals("Int") || type.equals("Boolean") || type.equals(symbolTable.getClassName()) || type.equals(symbolTable.getSuper()))
            return symbol;

        for (String importName : symbolTable.getImports()) {
            String[] imports = importName.split("\\.");
            if (imports[imports.length - 1].equals(type)) return symbol;
        }

        return null;
    }
}
