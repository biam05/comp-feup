import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.*;
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
            }
            else if (childKind.contains("Identifier")) {
                String name = childKind.replaceAll("'", "").replace("Identifier ", "");
                symbolTable.setSuperExtends(name);
            }
            else if (childKind.equals("{")) {
                while (true) {
                    i++;
                    JmmNode aux = children.get(i);
                    if (aux.getKind().equals("}")) break;
                    else if (aux.getKind().equals("MethodDeclaration")) continue;
                    symbolTable.addClassField(parseVarDeclaration(aux));
                }
            }
        }
        return true;
    }

    public Boolean visitImport(JmmNode node, Boolean dummy) {

        List<JmmNode> children = node.getChildren();

        for (JmmNode child : children) {
            String childKind = child.getKind();
            if (childKind.contains("Identifier")) {
                String name = childKind.replaceAll("'", "").replace("Identifier ", "");
                this.symbolTable.addImport(name);
            }
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

        for (int i = 0; i < nodes.size(); i++) {
            JmmNode nodeType = nodes.get(i++);
            JmmNode nodeName = nodes.get(i);
            Symbol symbol = new Symbol(nodeType, nodeName);
            method.addParameter(symbol);
        }
    }

    public void visitMethodBody(SymbolMethod method, JmmNode node) {
        System.out.println("Visit Method Body: " + node + ", " + node.getChildren());

        List<JmmNode> children = node.getChildren();

        for (JmmNode child : children) {
            if (child.getKind().equals("VarDeclaration")) {
                Symbol localVariable = parseVarDeclaration(child);
                method.addLocalVariables(localVariable);
            }
            else if (child.getKind().equals("Statement"))
                visitStatement(method, child);
        }
    }

    public void visitStatement(SymbolMethod method, JmmNode node){
        List <JmmNode> children = node.getChildren();
        System.out.println("Visit Statement: " + node + ", " + node.getChildren());

        for(JmmNode child: children){

            switch (child.getKind()) {
                case "WhileStatement":
                case "IfExpression":
                    visitConditionalStatement(method, child);
                    break;
                case "Expression":
                    visitExpression(method, child);
                    break;
                case "Assign":
                    visitAssign(method, child);
                    break;
                case "Statement":
                    visitStatement(method, child);
                    break;
            }

        }
    }

    public void visitConditionalStatement(SymbolMethod method, JmmNode node){
        List <JmmNode> children = node.getChildren();
        System.out.println("Visit Conditional Statement: " + node + ", " + node.getChildren());
        EvaluateUtils.evaluatesToBoolean(symbolTable, method, children.get(0), this.reports);
    }

    //if it finds an expression in the while it call it goes to visit expression


    public void visitAssign(SymbolMethod method, JmmNode node){
        System.out.println("Visit Assign: " + node + ", " + node.getChildren());
        List<JmmNode> children = node.getChildren();

    }

    public List<String> visitFinalTerms(JmmNode node) {


        /*
        FINAL TERMS:
            -> Identifier '...'
            -> Number '...'
            -> This


        */

        List<JmmNode> children = node.getChildren();

        if(children.get(0).getKind().contains("Identifier")) {
            //String[] childType =
        }

        return null;
    }

    public void visitExpression(SymbolMethod method, JmmNode node){
        System.out.println("Visit Expression: " + node + ", " + node.getChildren());
        for(JmmNode child: node.getChildren()){
            List<Report> allReports = new ArrayList<>();

            switch(child.getKind()) {
                case "AND":
                    allReports = EvaluateUtils.evaluateOperationWithBooleans(symbolTable, child, method);
                    break;

                case "LESS":
                case "ADD":
                case "SUB":
                case "MULT":
                case "DIV":
                    allReports = EvaluateUtils.evaluateOperationWithIntegers(symbolTable, child, method);
                    break;

                case "NOT":
                    allReports = EvaluateUtils.evaluateNotOperation(symbolTable, child, method);
                    break;
            }

            this.reports.addAll(allReports);
        }


        //verificar se operações são efetuadas com o mesmo tipo (e.g. int + boolean tem de dar erro)
        //verificar se operação booleana (&&, < ou !) é efetuada só com booleanos
        //não é possível utilizar arrays diretamente para operações aritmeticas (e.g. array1 + array2) --> quando se tem um operador verificar se se avalia para um inteiro

        //verificar se o indice do array access é um inteiro (e.g. a[true] não é permitido) --> quando se encontra um ArrayAccess a expressao interior tem que avaliar para um inteiro
        //verificar se um array access é de facto feito sobre um array (e.g. 1[10] não é permitido) --> quando se encontra um ArrayAccess verificar se antes tem uma variabel array


        //verificar se valor do assignee é igual ao do assigned (a_int = b_boolean não é permitido!) --> quando se encontra um assign, verificar que os dois lados avaliam para o mesmo

    }


    public Symbol parseVarDeclaration(JmmNode node) {
        List<JmmNode> children = node.getChildren();
        if (children.size() < 2) return null;

        JmmNode type = children.get(0);
        JmmNode name = children.get(1);

        return new Symbol(type, name);
    }
}
