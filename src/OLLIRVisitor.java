import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class OLLIRVisitor extends PreorderJmmVisitor<Boolean, Boolean> {
    private final GrammarSymbolTable symbolTable;
    private final List<Report> reports;
    private String code;
    private int var_temp = 1;

    public OLLIRVisitor(GrammarSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.reports = new ArrayList<>();
        this.code = OLLIRTemplates.init(symbolTable.getClassName(), symbolTable.getFields());
        addVisit("MethodDeclaration", this::visitMethod);
    }

    public List<Report> getReports() {
        return reports;
    }

    public String getCode() {
        return code;
    }

    public Boolean visitMethod(JmmNode node, Boolean dummy) {
        StringBuilder res = new StringBuilder();

        List<JmmNode> children = node.getChildren();

        boolean alreadyInBody = false;

        StringBuilder methodInfo = new StringBuilder();
        SymbolMethod method;

        for (int i = 0; i < children.size(); i++) {

            JmmNode child = children.get(i);
            String childKind = child.getKind();

            if (childKind.equals("LParenthesis")) { // parameters
                if (alreadyInBody) break;
                List<JmmNode> parameters = new ArrayList<>();
                while (true) {
                    i++;
                    JmmNode aux = children.get(i);
                    if (aux.getKind().equals("RParenthesis")) break;
                    parameters.add(children.get(i));
                }
                methodInfo.append(SemanticAnalysisUtils.getTypeParameters(parameters));

            } else if (childKind.equals("Main")) {
                methodInfo.append("Main(");
            } else if (childKind.contains("Identifier")) {
                String methodName = childKind.replaceAll("'", "").replace("Identifier ", "");
                methodInfo.append(methodName).append("(");

            } else if (childKind.equals("MethodBody")) { //method body (local variables)
                methodInfo.append(")");
                method = symbolTable.getMethodByInfo(methodInfo.toString());

                if (method == null) return false;

                res.append(OLLIRTemplates.methodDeclaration(method));
                res.append(" {\n").append(visitMethodBody(method, child)).append("\n");

                res.append(visitReturn(method, children.get(++i)));
                res.append("}\n");

                alreadyInBody = true;
            }
        }

        this.code += res.toString();
        return true;
    }

    private String visitReturn(SymbolMethod method, JmmNode node) {
        if(!node.getKind().equals("Return")) return OLLIRTemplates.returnVoid();
        System.out.println("visit return : " + node + ", " + node.getChildren());
        List<String> values = visitExpression(node.getChildren().get(0), method);
        return OLLIRTemplates.returnTemplate(values.get(0), method.getReturnType().toOLLIR());
    }

    private String visitMethodBody(SymbolMethod method, JmmNode node) {
        StringBuilder res = new StringBuilder();

        List<JmmNode> children = node.getChildren();

        for (JmmNode child : children) {
            if (child.getKind().equals("Statement")) res.append(visitStatement(method, child));
        }

        return res.toString();
    }

    public String visitStatement(SymbolMethod method, JmmNode node) {
        StringBuilder res = new StringBuilder();
        List<JmmNode> children = node.getChildren();

        for (JmmNode child : children) {
            switch (child.getKind()) {
                case "WhileStatement":
                case "IfExpression":
                    return "";
                case "Expression":
                    //chamar metodo
                    break;
                case "Assign":
                    res.append(visitAssign(method, child));
                    break;
                case "Statement":
                    res.append(visitStatement(method, child));
                    break;
            }
        }

        return res.toString();
    }

    public String visitAssign(SymbolMethod method, JmmNode node) {
        List<JmmNode> children = node.getChildren();
        String res = "";

        if (children.size() != 2) return "";

        List<String> leftOperandType = visitExpression(children.get(0), method);
        if (leftOperandType == null) {
            return "";
        }
        res += leftOperandType.get(0) + " :=" + leftOperandType.get(1) + " ";

        List<String> rightOperandType = visitExpression(children.get(1), method);
        if (rightOperandType == null)
            return "";

        res += rightOperandType.get(0) + ";\n";

        //isto não está bonito
        if (rightOperandType.get(0).contains("new"))
            res += OLLIRTemplates.invokeSpecial(leftOperandType.get(2) + "." + rightOperandType.get(2));

        return res;
    }

    public List<String> visitExpression(JmmNode node, SymbolMethod method) {
        List<JmmNode> children = node.getChildren();
        System.out.println("Visit Expression: " + node + ", " + children);
        String ret = "";
        String res = "";
        if (children.size() == 1) {
            JmmNode child = children.get(0);
            System.out.println("child : " + child);
            switch (child.getKind()) {
                case "And":
                    res = visitOperator(child, method);
                    ret = ".bool";
                    break;
                case "Less":
                case "Add":
                case "Sub":
                case "Div":
                case "Mult":
                case "Not":
                    res = visitOperator(child, method);
                    ret = OLLIRTemplates.getOperationReturn(child);
                    break;
                case "FinalTerms":
                    return visitFinalTerms(child, method);
                case "ArrayAccess":
                    res += ".hi3";
                    ret = ".ret_hi3";
                    break;
                case "Length":
                    res += ".hi4";
                    ret = ".ret_hi4";
                    break;
            }
        } else if (children.size() == 2) {
            if (children.get(0).getKind().equals("FinalTerms") && children.get(1).getKind().equals("MethodCall")) {
                res += ".hi5";
                ret = ".ret_hi5";
            }
        }

        List<String> result = new ArrayList<>();
        result.add(res);
        result.add(ret);

        return result;
    }


    private List<String> visitFinalTerms(JmmNode node, SymbolMethod method) { //melhorar codigo por favorzinho
        List<JmmNode> children = node.getChildren();
        List<String> result = new ArrayList<>();
        System.out.println("final terms");
        JmmNode firstChild = children.get(0);
        String ret = "", res = "", value = "";

        if (firstChild.getKind().contains("Number")) {
            ret = ".i32";
            value = firstChild.getKind().replace("Number", "").replaceAll("'", "").replaceAll(" ", "");
            res = value + ret;
        } else if (firstChild.getKind().equals("NewIntArrayExpression")) { //not for this checkpoint
            ret = "";
            res = "";
        } else if (firstChild.getKind().contains("NewIdentifier")) {
            value = firstChild.getKind().replaceAll("'", "").replace("NewIdentifier ", "");
            res = "new(" + value + ")." + value;
            ret = "." + value;
        } else if (firstChild.getKind().contains("Identifier")){
            res = OLLIRTemplates.getIdentifier(firstChild.getKind(), symbolTable, method);
        } else if (firstChild.getKind().equals("True") || firstChild.getKind().equals("False")) {
            int bool = firstChild.getKind().equals("True") ? 1 : 0;
            res = bool + ".bool";
            ret = ".bool";
        }

        result.add(res);
        result.add(ret);
        result.add(value);
        return result;
    }

    public String visitOperator(JmmNode node, SymbolMethod method) {
        System.out.println("operator");
        StringBuilder res = new StringBuilder();
        List<JmmNode> children = node.getChildren();
        if (children.size() > 2) return "";
        else if (children.size() == 2) {
            JmmNode left = children.get(0);
            JmmNode right = children.get(1);
            List<String> lres = checkNodeAndVisit(left, method);
            List<String> rres = checkNodeAndVisit(right, method);
            //List<String> leftR = breakNestedOperations(left, lres.get(0));
            //List<String> rightR = breakNestedOperations(right, rres.get(0));
            if (!lres.isEmpty()) res.append(lres.get(0));
            if (!rres.isEmpty()) res.append(rres.get(0));
//            res.append(leftR.get(0)).append(rightR.get(0));
//            res.append(leftR.get(1));
//            res.append(" ").append(OLLIRTemplates.getOperationType(node)).append(" ");
//            res.append(rightR.get(1));
//            res.append(";");
        } else if (children.size() == 1) {
            JmmNode child = children.get(0);
            List<String> result = null;
            List<String> filho = checkNodeAndVisit(child, method);
            List<String> filhoR = breakNestedOperations(child, filho.get(0));
            res.append(filho);
            res.append(" ").append(OLLIRTemplates.getOperationType(node)).append(" ");
            res.append(filho);

        }


        return res.toString();
    }

    public List<String> breakNestedOperations(JmmNode node, String currentRes) {//deals with temp vars
        String aux = "";
        List<String> res = new ArrayList<>();
        //se tiver uma operação dentro de uma operaçao ou uma chamada a um metodo houver uma operaçao ou a chamada de outro metodo  tambem é necessário variavel temporaria
        if (OLLIRTemplates.isOperator(node) || node.getKind().equals("Call")) {
            //construct the needed temporary variables
            if (currentRes.contains(":=.")) {
                //se for num assign temos que partir a variavek temporaria e manter o assign
            } else {
                //apenas chamada do metodo
            }

            var_temp++;
        }

        res.add(aux);
        res.add(currentRes);
        return res;

    }

    public List<String> checkNodeAndVisit(JmmNode node, SymbolMethod method) {
        List<String> result = new ArrayList<>();
        String res = "";
        switch (node.getKind()) {
            case "Expression":
                result = visitExpression(node, method);
                break;
            case "FinalTerm":
                result = visitFinalTerms(node, method);
                break;
            case "Call":
                result = visitedCall(node, method);
            case "Assign":
                res = visitAssign(method, node);
                result.add(res);
                break;
            case "Statement":
                res = visitStatement(method, node);
                result.add(res);
                break;
            case "And":
            case "Less":
            case "Add":
            case "Sub":
            case "Div":
            case "Mult":
            case "Not":
                res = visitOperator(node, method);
                result.add(res);
                break;

        }
        return result;
    }

    public List<String> visitedCall(JmmNode node, SymbolMethod method) {
        List<String> res = new ArrayList<>();
        List<JmmNode> children = node.getChildren();
        JmmNode left = children.get(0);
        JmmNode right = children.get(1);
        if(left.getChildren().get(0).getKind().equals("This")) {
            if (right.getKind().equals("MethodCall"))
                visitMethodCall(children.get(1), method, "virtual");
            else if (right.getKind().equals("Identifier")) {
                if (node.getParent().getKind().equals("Assign")) {
                    List<JmmNode> nodes = node.getParent().getChildren();
                    if(nodes.get(0).getKind().equals("Call")){
                        //put field
                        String aux = "putfied(this, " + OLLIRTemplates.getIdentifier(right.getKind(), symbolTable, method);
                        //primeiro argumento é o identifier.tipo e o segundo é o valor que passa a ter
                        res.add(aux);
                    }
                    else if(nodes.get(1).getKind().equals("Call")){
                        //geet field
                        String aux = "getfield(this," + OLLIRTemplates.getIdentifier(right.getKind(), symbolTable, method);
                        //o argumento é o identifier.tipo).tipo

                    }
                    //getfield ou putfield

                }
            }
        }
        return res;
    }

    public List<String> visitMethodCall(JmmNode node, SymbolMethod method, String type) {
        List<String> res = new ArrayList<>();
        if (type.equals("virtual")) {
            res.add("invokevirtual(this,");
        }
        return res;
    }

}
