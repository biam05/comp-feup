import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OLLIRVisitor extends PreorderJmmVisitor<Boolean, Boolean> {
    private GrammarSymbolTable symbolTable;
    private List<Report> reports;
    private String code;

    public OLLIRVisitor(GrammarSymbolTable symbolTable){
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
        StringBuilder res = new StringBuilder("");

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

            } else if (childKind.contains("Identifier") || childKind.equals("Main")) {
                String methodName = childKind.replaceAll("'", "").replace("Identifier ", "");
                methodInfo.append(methodName).append("(");

            } else if (childKind.equals("MethodBody")) { //method body (local variables)
                methodInfo.append(")");
                method = symbolTable.getMethodByInfo(methodInfo.toString());

                if (method == null) return false;

                res.append(OLLIRTemplates.methodDeclaration(method));
                res.append(" {\n").append(visitMethodBody(method, child)).append("\n}\n");

                alreadyInBody = true;
            }
        }

        this.code += res.toString();
        return true;
    }

    private String visitMethodBody(SymbolMethod method, JmmNode node) {
        StringBuilder res = new StringBuilder(OLLIRTemplates.localVariables(method.getLocalVariables()));

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
                    res.append("");
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
        res += leftOperandType.get(0) + " =" + leftOperandType.get(1) + " ";

        List<String> rightOperandType = visitExpression(children.get(1), method);
        if (rightOperandType == null)
            return "";

        res += rightOperandType.get(0) + ";\n";
        return res;
    }

    public List<String> visitExpression(JmmNode node, SymbolMethod method){
        List<JmmNode> children = node.getChildren();
        System.out.println("Visit Expression: " + node + ", " + children);
        String ret = "";
        String res = "";
        if (children.size() == 1) {
            JmmNode child = children.get(0);
            List<String> eval;
            System.out.println("child : " + child);
            switch (child.getKind()) {
                case "And":
                    eval = visitOperator();
                    res += eval.get(0) + " &&.bool " + eval.get(1);
                    ret = ".bool";
                    break;

                case "Less":
                    eval = visitOperator();
                    res += eval.get(0) + " <.i32 " + eval.get(1);
                    ret = ".i32";
                    break;

                case "Add":
                    eval = visitOperator();
                    res += eval.get(0) + " +.i32 " + eval.get(1);
                    ret = ".i32";
                    break;

                case "Sub":
                    eval = visitOperator();
                    res += eval.get(0) + " -.i32 " + eval.get(1);
                    ret = ".i32";
                    break;

                case "Div":
                    eval = visitOperator();
                    res += eval.get(0) + " /.i32 " + eval.get(1);
                    ret = ".i32";
                    break;

                case "Mult":
                    eval = visitOperator();
                    res += eval.get(0) + " *.i32 " + eval.get(1);
                    break;

                case "Not":
                    eval = visitOperator();
                    res += eval.get(0) + " !.bool " + eval.get(1);
                    ret = ".bool";
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

    private List<String> visitFinalTerms(JmmNode node, SymbolMethod method) {
        List<JmmNode> children = node.getChildren();
        List<String> result = new ArrayList<>();

        JmmNode firstChild = children.get(0);
        String ret = "";
        String res = "";

        if (firstChild.getKind().contains("Number")) {
            ret = ".i32";
            res = firstChild.getKind().replace("Number", "").replaceAll("'", "") + ".i32";
        }
        else if (firstChild.getKind().equals("NewIntArrayExpression")) { //not for this checkpoint
            ret = "";
            res = "";
        } else if (firstChild.getKind().contains("NewIdentifier")) {
            String identifier = firstChild.getKind().replaceAll("'", "").replace("NewIdentifier ", "");
            res = "new(" + identifier + ")." + identifier;
            ret = "." + identifier;

        } else if (firstChild.getKind().contains("Identifier")) {
            String identifier = firstChild.getKind().replaceAll("'", "").replace("Identifier ", "");
            String type = SemanticAnalysisUtils.checkIfIdentifierExists(symbolTable, method, identifier).toOLLIR();
            res = identifier + type;
            ret = type;
        } else if (firstChild.getKind().equals("True") || firstChild.getKind().equals("False")) {
            int bool = firstChild.getKind().equals("True") ? 1 : 0;
            res = bool + ".bool";
            ret = ".bool";
        }
        result.add(res);
        result.add(ret);
        return result;
    }

    public List<String> visitOperator(){
        List<String> a = new ArrayList<>();
        a.add("aa");
        a.add("bb");

        return a;
    }
}
