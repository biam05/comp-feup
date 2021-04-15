import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.*;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;
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
            else if (child.getKind().equals("Statement"))
                visitStatement(method, child);
        }
    }

    public void visitStatement(SymbolMethod method, JmmNode node){
        List <JmmNode> children = node.getChildren();
        for(JmmNode child: children){
            System.out.println(child);
            if(child.getKind().equals("WhileStatement") || child.getKind().equals("IfExpression")) visitConditionalStatement(child);

            else if(child.getKind().equals("Expression")) visitExpression(child);

            else if(child.getKind().equals("Assign")) visitAssign(method, child);

            else if (child.getKind().equals("Statement")) visitStatement(method, child);

        }
    }

    public void visitConditionalStatement(JmmNode node){
        List <JmmNode> children = node.getChildren();
        if(children.size() != 1 || !children.get(0).getKind().equals("Expression")){
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(children.get(0).get("line")), Integer.parseInt(children.get(0).get("col")), "no while conditional expression");
            this.reports.add(report);
        }
        else evaluatesToBoolean(children.get(0), true);

    }

    //if it finds an expression in the while it call it goes to visit expression

    public boolean evaluatesToBoolean( JmmNode node, boolean addsReport){
        List <JmmNode> children = node.getChildren();
        for(int i = 0; i < children.size(); i++) {
            JmmNode child = children.get(i);
            System.out.println("---> " + children.get(i).getKind());
            Report report;
            switch (child.getKind()) {
                case "And":
                    if (!evaluatesToBoolean(child.getChildren().get(0), false)){
                        report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.get("line")), Integer.parseInt(child.get("col")), "left operand for binary operator '&&' is not a boolean");
                        break;
                    }
                    if(!evaluatesToBoolean(child.getChildren().get(1), false)){
                        report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.get("line")), Integer.parseInt(child.get("col")), "right operand for binary operator '&&' is not a boolean");
                        break;
                    }
                    return true;

                case "Less":
                    if (evaluatesToInteger(child.getChildren().get(0), false)){
                        report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.get("line")), Integer.parseInt(child.get("col")), "left operand types for binary operator '<' is not an integer");
                        break;
                    }
                    if(evaluatesToInteger(child.getChildren().get(0), false)){
                        report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.get("line")), Integer.parseInt(child.get("col")), "right operand types for binary operator '<' is not an integer");
                        break;
                    }

                    return true;

                case "Not":
                    if (evaluatesToBoolean(child.getChildren().get(0), false)) return true;
                    report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.get("line")), Integer.parseInt(child.get("col")), "bad operand type for binary operator '!': boolean expected");
                    break;
                case "FinalTerms":
                    //if(child.getChildren().size)
                case "True":
                case "False":
                    return true;
                //variable is boolean
                default:
                    return false;
            }

            if(addsReport){
                System.out.println(report.toString());
                this.reports.add(report);
            }


        }
        //falta o caso da expressao avaliar para booleano
        //verificar metodos?

        return false;
    }
    public boolean evaluatesToInteger(JmmNode node, boolean addsReport){
        Report report;
        List <JmmNode> children = node.getChildren();
        if(children.size() == 1){
            if(children.get(0).getKind().equals("int")) return true;
            else if(isIdentifier(children.get(0).getKind())) return true;
        }
        for(JmmNode child: children){
            //check if expression evaluates to int
            //method
        }
        if(addsReport){
            //this.reports.add(report);
        }
        return false;

        //a[0] se a é int[] avalia para int
    }

    public boolean isIdentifier(String kind){
        String[] parts = kind.split(" ");
        return parts[0].equals("Identifier");
    }

    // TO DO : fazer metodos

    public void visitAssign(SymbolMethod method, JmmNode node){
        System.out.println("----> Assign children:");
        for(JmmNode child: node.getChildren()){
            System.out.println("child");
            List<String> param = visitExpression(method, child);
            if(param != null && (param.size() == 2)) System.out.println("->" + param.get(0) + param.get(1));
        }
        System.out.println("----> Final children");
    }

    public List<String> visitFinalTerms(JmmNode node) {

        /*String[] childType = child.get(0).getKind().split(" ");
        System.out.println("size: " + childType.length);
        System.out.println("->" + childType[0] + ", " + childType[1]);
        String name = childType[1].replaceAll("'", "");*/

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

    /*
        [FinalTerms (col: 13, line: 22), Period (col: 14, line: 22), Identifier 'printL' (col: 21, line: 22), LParenthesis (col: 21, line: 22), Expression (col: 22, line: 22), RParenthesis (col: 23, line: 22)]
        [Not (col: 21, line: 38)]
        [FinalTerms (col: 9, line: 51)]
        [FinalTerms (col: 25, line: 51), Period (col: 26, line: 51), Length (col: 27, line: 51)]
        [FinalTerms (col: 20, line: 41), Period (col: 24, line: 41), Identifier 'quicksort' (col: 34, line: 41), LParenthesis (col: 34, line: 41), Expression (col: 35, line: 41), Comma (col: 36, line: 41), Expression (col: 38, line: 41), Comma (col: 39, line: 41), Expression (col: 41, line: 41), RParenthesis (col: 53, line: 41)

    */
    public List<String> visitExpression(SymbolMethod method, JmmNode node) {
        System.out.println(node + ", "+ node.getChildren());
        return null;
    }

    public void visitExpression(JmmNode node){
        Report report = null;
        //System.out.println("Expression children:");
        for(JmmNode children: node.getChildren()){
            System.out.println(children);
            List<JmmNode> child = children.getChildren();

            switch(children.getKind()) {
                case "AND":
                    if(!evaluatesToBoolean(child.get(0), false)){
                        report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.get(0).get("line")), Integer.parseInt(child.get(0).get("col")), "left operand for binary operator '&&' is not a boolean");
                        break;
                    }
                    else if(!evaluatesToBoolean(child.get(1), false)){
                        report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.get(1).get("line")), Integer.parseInt(child.get(1).get("col")), "left operand for binary operator '&&' is not a boolean");
                        break;
                    }
                    else return;

                case "LESS":
                case "ADDSUB":
                case "MultDiv":
                    if(!evaluatesToInteger(child.get(0), false)){
                        report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.get(0).get("line")), Integer.parseInt(child.get(0).get("col")), "left operand types for binary operator '<' is not an integer");
                        break;
                    }
                    else if(!evaluatesToInteger(child.get(1), false)){
                        report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.get(0).get("line")), Integer.parseInt(child.get(0).get("col")), "right operand types for binary operator '<' is not an integer");
                        break;
                    }else return;
                case "NOT":
                    if(!evaluatesToBoolean(child.get(0), false)){
                        report = new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.get(0).get("line")), Integer.parseInt(child.get(0).get("col")), "bad operand type for binary operator '!': boolean expected");
                        break;
                    }
                    else return;
            }
            this.reports.add(report);
        }


        //verificar se operações são efetuadas com o mesmo tipo (e.g. int + boolean tem de dar erro)
        //não é possível utilizar arrays diretamente para operações aritmeticas (e.g. array1 + array2) --> quando se tem um operador verificar se se avalia para um inteiro
        //verificar se um array access é de facto feito sobre um array (e.g. 1[10] não é permitido) --> quando se encontra um ArrayAccess verificar se antes tem uma variabel array
        //verificar se o indice do array access é um inteiro (e.g. a[true] não é permitido) --> quando se encontra um ArrayAccess a expressao interior tem que avaliar para um inteiro
        //verificar se valor do assignee é igual ao do assigned (a_int = b_boolean não é permitido!) --> quando se encontra um assign, verificar que os dois lados avaliam para o mesmo
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
