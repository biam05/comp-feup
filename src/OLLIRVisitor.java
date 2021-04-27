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
        String res = OLLIRTemplates.localVariables(method.getLocalVariables());

        List<JmmNode> children = node.getChildren();

        for (JmmNode child : children) {
            if (child.getKind().equals("Statement")) res += visitStatement(method, child);
        }

        return res;
    }

    public String visitStatement(SymbolMethod method, JmmNode node) {
        String res = "";
        List<JmmNode> children = node.getChildren();

        for (JmmNode child : children) {
            switch (child.getKind()) {
                case "WhileStatement":
                case "IfExpression":
                    return "";
                case "Expression":
                    //chamar metodo
                    res += "";
                    break;
                case "Assign":
                    res += visitAssign(method, child);
                    break;
                case "Statement":
                    res += visitStatement(method, child);
                    break;
            }
        }

        return res;
    }

    public String visitAssign(SymbolMethod method, JmmNode node) {
        System.out.println(node.toJson());
        List<JmmNode> children = node.getChildren();
        String res = "";

        if (children.size() != 2) return "";
        /*if (!children.get(0).getKind().equals("Expression") || !children.get(1).getKind().equals("Expression"))
            return "";*/

        String leftOperandType = visitExpression(children.get(0));
        if (leftOperandType == null) {
            return "";
        }

        String rightOperandType = visitExpression(children.get(1));
        if (rightOperandType == null)
            return "";

        return res;
    }

    public String visitExpression(JmmNode node){
        List<JmmNode> children = node.getChildren();
        String res = "";
        if (children.size() == 1) {
            JmmNode child = children.get(0);
            if (child.getKind().equals("And")) {
                String[] eval = visitOperator();
                res = eval[0] + " &&.bool " + eval[1];
            } else if (child.getKind().equals("Less")) {
                String[] eval = visitOperator();
                res = eval[0] + " <.i32 " + eval[1];
            } else if (child.getKind().equals("Add")){
                String[] eval = visitOperator();
                res = eval[0] + " +.i32 " + eval[1];
            } else if(child.getKind().equals("Sub")){
                String[] eval = visitOperator();
                res = eval[0] + " -.i32 " + eval[1];
            } else if(child.getKind().equals("Div")){
                String[] eval = visitOperator();
                res = eval[0] + " /.i32 " + eval[1];
            } else if(child.getKind().equals("Mult")) {
                String[] eval = visitOperator();
                res = eval[0] + " *.i32 " + eval[1];
            } else if (child.getKind().equals("Not")) {
                String[] eval = visitOperator();
                res = eval[0] + " !.bool " + eval[1];
            } else if (child.getKind().equals("FinalTerms")) {
                res = "hi2";
            } else if (child.getKind().equals("ArrayAccess")) {
                res = "hi3";
            } else if (child.getKind().equals("Length")) {
                res = "hi4";
            }
        } else if (children.size() == 2) {
            if (children.get(0).getKind().equals("FinalTerms") && children.get(1).getKind().equals("MethodCall")) {
                res = "oo";
            }
        }

        return res;
    }

    private static Type visitFinalTerms(JmmNode node) {
        List<JmmNode> children = node.getChildren();
        System.out.println("he");
        JmmNode firstChild = children.get(0);

        /*if (firstChild.getKind().contains("Number")) return new Type("Int", false);
        else if (firstChild.getKind().equals("NewIntArrayExpression") && rightOperand) {
            if (evaluatesToInteger(symbolTable, method, firstChild.getChildren().get(0), reports))
                return new Type("Int", true);
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(firstChild.getChildren().get(0).get("line")), Integer.parseInt(firstChild.getChildren().get(0).get("col")), "bad array access: integer expected"));
        } else if (firstChild.getKind().contains("NewIdentifier") && rightOperand) {
            String newIdentifier = firstChild.getKind().replaceAll("'", "").replace("NewIdentifier ", "");
            return new Type(newIdentifier, false);
        } else if (firstChild.getKind().contains("Identifier")) {
            String identifier = firstChild.getKind().replaceAll("'", "").replace("Identifier ", "");
            Type type = checkIfIdentifierExists(symbolTable, method, identifier);
            if (type == null)
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(firstChild.get("line")), Integer.parseInt(firstChild.get("col")), "identifier '" + identifier + "' is not declared"));
            return type;
        } else if (rightOperand && (firstChild.getKind().equals("True") || firstChild.getKind().equals("False")))
            return new Type("Boolean", false);*/


        return null;
    }

    public String[] visitOperator(){


        return null;
    }
}
