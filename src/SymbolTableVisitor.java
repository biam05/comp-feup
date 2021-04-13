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
            if(child.getKind().equals("WhileStatement") || child.getKind().equals("IfExpression")){
                visitConditionalStatement(method, child);
            }
            else if(child.getKind().equals("Expression")){
                visitExpression(method, child);
            }
        }
    }

    public void visitConditionalStatement(SymbolMethod method, JmmNode node){
        List <JmmNode> children = node.getChildren();
        for(JmmNode child: children){
            if(child.getKind().equals("Expression")){
                checkBooleanExpression(child);
            }
        }
    }

    public boolean checkBooleanExpression( JmmNode node){
        System.out.println("got the while");
        List <JmmNode> children = node.getChildren();
        for(int i = 0; i < children.size(); i++){
            JmmNode child = children.get(i);
            System.out.println(child);
            if(child.getKind().equals("&&")){ //dois booleanos
                if(checkBooleanExpression(children.get(i-1)) && checkBooleanExpression(children.get(i+1))) return true;
                else return false;
            }
            else if(child.getKind().equals("<")){
                // dois valores
            }
            else if(child.getKind().equals("!")){
                if(checkBooleanExpression(children.get(i+1))) return true;
                else return false;
            }
            else if(child.getKind().equals("true") || child.getKind().equals("false")){
                return true;
            }
            else if(child.getKind().equals("FinalTerms")){
                System.out.println("Yep we can catch expression");
            }
            //falta o caso da expressao avaliar para booleano
            //verificar metodos?
        }
        //verificar se conditional expressions (if e while) resulta num booleano
        //se sim nice
        //senao adiciona aos reports
        return true;
    }

    public void visitExpression(SymbolMethod method, JmmNode node){

        System.out.println("Expression children:");
        for(JmmNode child: node.getChildren()){
            System.out.println(child);
        }
        //verificar se operações são efetuadas com o mesmo tipo (e.g. int + boolean tem de dar erro)
        //não é possível utilizar arrays diretamente para operações aritmeticas (e.g. array1 + array2)
        //verificar se um array access é de facto feito sobre um array (e.g. 1[10] não é permitido)
        //verificar se o indice do array access é um inteiro (e.g. a[true] não é permitido)
        //verificar se valor do assignee é igual ao do assigned (a_int = b_boolean não é permitido!)
        //verificar se operação booleana (&&, < ou !) é efetuada só com booleanos
        /*
        -> verificar se o "target" do método existe, e se este contém o método (e.g. a.foo, ver se 'a' existe e se tem um método 'foo')
            - caso seja do tipo da classe declarada (e.g. a usar o this), se não existir declaração na própria classe: se não tiver extends retorna erro, se tiver extends assumir que é da classe super.
        -> caso o método não seja da classe declarada, isto é uma classe importada, assumir como existente e assumir tipos esperados. (e.g. a = Foo.b(), se a é um inteiro, e Foo é uma classe importada, assumir que o método b é estático (pois estamos a aceder a uma método diretamente da classe), que não tem argumentos e que retorna um inteiro)
        -> verificar se o número de argumentos na invocação é igual ao número de parâmetros da declaração
        -> verificar se o tipo dos parâmetros coincide com o tipo dos argumentos
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
