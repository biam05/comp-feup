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
                visitExpression(method, child);
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

    // TODO : fazer metodos

    public void visitExpression(SymbolMethod method, JmmNode node){

        List <JmmNode> children = node.getChildren();
        List<MethodCall> methodCalls = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            JmmNode child = children.get(i);
            if (child.getKind().equals("Period")) {
                i++;
                // Get method name after period
                String methodName;
                if (children.get(i).getKind().contains("Identifier"))
                     methodName = children.get(i).getKind().replaceAll("'", "").replace("Identifier ", "");
                else continue;
                i -= 2;
                // Get var name before period
                JmmNode aux = children.get(i).getChildren().get(0);
                String varName;
                varName = aux.getKind().replaceAll("'", "").replace("Identifier ", "");
                i += 3;
                // Get parameters passed on method call
                List<Type> parameters = new ArrayList<>();
                if (children.get(i).getKind().equals("LParenthesis")) {
                    while (true) {
                        i++;
                        aux = children.get(i);
                        if (aux.getKind().equals("RParenthesis")) break;
                        //TODO: Add type of the parameter being passed
                    }
                }
                // Add to list to post-process
                methodCalls.add(new MethodCall(method, varName, methodName, parameters, Integer.parseInt(children.get(i - 1).get("line")), Integer.parseInt(children.get(i - 1).get("col"))));
            }
        }
        for (MethodCall methodCall : methodCalls)
        // Call function to check if var exists and has method methodName and check argument types and number
        if (!isValidMethodCall(methodCall)) {
            Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, methodCall.getLine(), methodCall.getCol(), "Invalid method call");
            reports.add(report);
            System.out.println(report);
        }

        //verificar se operações são efetuadas com o mesmo tipo (e.g. int + boolean tem de dar erro)
        //não é possível utilizar arrays diretamente para operações aritmeticas (e.g. array1 + array2)
        //verificar se um array access é de facto feito sobre um array (e.g. 1[10] não é permitido)
        //verificar se o indice do array access é um inteiro (e.g. a[true] não é permitido)
        //verificar se valor do assignee é igual ao do assigned (a_int = b_boolean não é permitido!)
        //verificar se operação booleana (&&, < ou !) é efetuada só com booleanos

    }

    public Boolean isValidMethodCall(MethodCall methodCall) {
        for (String importName: symbolTable.getImports()) {
            if(methodCall.getVarName().equals(importName)) return true;
        }
        for (Symbol var: methodCall.getMethod().getLocalVariables()) {
            if ((var.getName().equals(methodCall.getVarName()) && var.getType().getName().equals(this.symbolTable.getClassName())) || methodCall.getVarName().equals("this")) {
                for (SymbolMethod method: symbolTable.getMethodsAndParameters()) {
                    if (method.getName().equals(methodCall.getMethodName())) {
                        //TODO: Delete this return
                        return true;
                        /*if (method.getParameters().size() != methodCall.getParameters().size())
                            return false;
                        for (int i = 0; i < method.getParameters().size(); i++)
                            if (method.getParameters().get(i).getType() != methodCall.getParameters().get(i))
                                return false;
                        return true;*/
                    }
                }
                //TODO: confirm if "!= null" is right
                return symbolTable.getSuper() != null;
            }
        }
        return false;
        /*
        -> verificar se o "target" do método existe, e se este contém o método (e.g. a.foo, ver se 'a' existe e se tem um método 'foo')
            - caso seja do tipo da classe declarada (e.g. a usar o this), se não existir declaração na própria classe: se não tiver extends retorna erro, se tiver extends assumir que é da classe super.
        -> caso o método não seja da classe declarada, isto é uma classe importada, assumir como existente e assumir tipos esperados. (e.g. a = Foo.b(), se a é um inteiro, e Foo é uma classe importada, assumir que o método b é estático (pois estamos a aceder a uma método diretamente da classe), que não tem argumentos e que retorna um inteiro)
        -> verificar se o número de argumentos na invocação é igual ao número de parâmetros da declaração
        -> verificar se o tipo dos parâmetros coincide com o tipo dos argumentos

        ->  this.foo();, class FindMaximum {FindMaximum fm; fm.foo();}
        ->  class Lazysort extends Quicksort {Lazysort ls; ls.quicksort();} (quicksort belonging to class Quicksort)
        ->  import ioPlus; class FindMaximum {a = ioPlus.foo();}
        */
    }

    public Symbol parseVarDeclaration(JmmNode node) {
        List<JmmNode> children = node.getChildren();
        if (children.size() < 2) return null;

        JmmNode type = children.get(0);
        JmmNode name = children.get(1);

        return new Symbol(type, name);
    }
}
