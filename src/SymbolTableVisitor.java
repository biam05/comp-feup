import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.*;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTableVisitor extends PreorderJmmVisitor<Boolean, Boolean> {
    private final GrammarSymbolTable symbolTable;
    private final List<Report> reports;
    private final Map<JmmNode, SymbolMethod> expressionNodes;

    public SymbolTableVisitor() {
        this.symbolTable = new GrammarSymbolTable();
        this.reports = new ArrayList<>();
        this.expressionNodes = new HashMap<>();
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

    public Map<JmmNode, SymbolMethod> getExpressionNodes() {
        return expressionNodes;
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
            else if (childKind.equals("LBrace")) {
                while (true) {
                    i++;
                    JmmNode aux = children.get(i);
                    if (aux.getKind().equals("RBrace")) break;
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

        Boolean alreadyInBody = false;

        for (int i = 0; i < children.size(); i++) {

            JmmNode child = children.get(i);
            String childKind = child.getKind();

            if (childKind.equals("Type") || childKind.equals("Void")) method.setReturnType(new Type(child)); // return type

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
            }

            else if(childKind.equals("MethodBody")) { //method body (local variables)
                visitMethodBody(method, child);
                alreadyInBody = true;
            }

            else if (childKind.contains("Identifier") || childKind.equals("Main")) {
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
            else if (child.getKind().equals("Statement"))
                visitStatement(method, child);
        }
    }

    public void visitStatement(SymbolMethod method, JmmNode node){
        List <JmmNode> children = node.getChildren();
        for(JmmNode child: children){
            if(child.getKind().equals("WhileStatement") || child.getKind().equals("IfExpression")){
                visitConditionalStatement(child);
            }
            else if(child.getKind().equals("Expression")){
                checkExpressionHasMethod(method, child);
            }
        }
    }

    public void visitConditionalStatement(JmmNode node){
        List <JmmNode> children = node.getChildren();
        for(JmmNode child: children){
            if(child.getKind().equals("Expression")){
                evaluatesToBoolean(child);
            }
        }
    }

    //if it is inside of the while and is a statement goes back to visitStatement
    //if it finds an expression in the while it call it goes to visit expression

    public boolean evaluatesToBoolean( JmmNode node){
        List <JmmNode> children = node.getChildren();
        for(int i = 0; i < children.size(); i++) {
            JmmNode child = children.get(i);
            System.out.println("---> " + children.get(i).getKind());
            Report report = null;
            switch (child.getKind()) {
                case "And":
                    if (evaluatesToBoolean(child.getChildren().get(0)) && evaluatesToBoolean(child.getChildren().get(1))) return true;
                    report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.get("line")), Integer.parseInt(child.get("col")), "bad operand types for binary operator '&&'");
                    break;

                case "Less":
                    if (evaluatesToInteger(child.getChildren().get(0)) && evaluatesToInteger(child.getChildren().get(0))) return true;
                    report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.get("line")), Integer.parseInt(child.get("col")), "bad operand types for binary operator '<'");
                    break;

                case "Not":
                    if (evaluatesToBoolean(child.getChildren().get(0))) return true;
                    report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.get("line")), Integer.parseInt(child.get("col")), "bad operand type for binary operator '!'");

                    break;

                case "True":
                case "False":
                    return true;

                default:
                    return false;
            }
            
            if(report != null) {
                System.out.println(report);
                this.reports.add(report);
            }
        }
        //falta o caso da expressao avaliar para booleano
        //verificar metodos?

        //verificar se conditional expressions (if e while) resulta num booleano
        //se sim nice
        //senao adiciona aos reports
        return false;
    }
    public boolean evaluatesToInteger(JmmNode node){
        List <JmmNode> children = node.getChildren();
        if(children.size() == 1){
            if(children.get(0).getKind().equals("int")) return true;
            else if(isIdentifier(children.get(0).getKind())) return true;
        }
        for(JmmNode child: children){
            //check if expression evaluates to int
            //method
        }
        return false;
    }

    public boolean isIdentifier(String kind){
        String[] parts = kind.split(" ");
        return parts[0].equals("Identifier");
    }

    public void checkExpressionHasMethod(SymbolMethod method, JmmNode node){
        List <JmmNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            JmmNode child = children.get(i);
            if (child.getKind().equals("Period")) {
                i++;
                if (!children.get(i).getKind().contains("Identifier"))
                    continue;
                // Add to map to post-process
                expressionNodes.put(node, method);
            }
        }
        //verificar se operações são efetuadas com o mesmo tipo (e.g. int + boolean tem de dar erro)
        //não é possível utilizar arrays diretamente para operações aritmeticas (e.g. array1 + array2)
        //verificar se um array access é de facto feito sobre um array (e.g. 1[10] não é permitido)
        //verificar se o indice do array access é um inteiro (e.g. a[true] não é permitido)
        //verificar se valor do assignee é igual ao do assigned (a_int = b_boolean não é permitido!)
        //verificar se operação booleana (&&, < ou !) é efetuada só com booleanos
    }



    public Symbol parseVarDeclaration(JmmNode node) {
        List<JmmNode> children = node.getChildren();
        if (children.size() < 2) return null;

        JmmNode type = children.get(0);
        JmmNode name = children.get(1);

        return new Symbol(type, name);
    }
}
