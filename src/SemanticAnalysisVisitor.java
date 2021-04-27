import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class SemanticAnalysisVisitor extends PreorderJmmVisitor<Boolean, Boolean> {
    private final GrammarSymbolTable symbolTable;
    private final List<Report> reports;

    public SemanticAnalysisVisitor(GrammarSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.reports = new ArrayList<>();
        addVisit("MethodDeclaration", this::visitMethod);
    }

    public List<Report> getReports() {
        return reports;
    }

    public Boolean visitMethod(JmmNode node, Boolean dummy) {

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

                if (method == null)
                    return false;

                visitMethodBody(method, child);
                alreadyInBody = true;
            }
        }

        return true;
    }

    public void visitMethodBody(SymbolMethod method, JmmNode node) {
        List<JmmNode> children = node.getChildren();

        for (JmmNode child : children) {
            if (child.getKind().equals("Statement")) visitStatement(method, child);
        }
    }

    public void visitStatement(SymbolMethod method, JmmNode node) {
        List<JmmNode> children = node.getChildren();

        for (JmmNode child : children) {
            switch (child.getKind()) {
                case "WhileStatement":
                case "IfExpression":
                    visitConditionalStatement(method, child);
                    break;
                case "Expression":
                    SemanticAnalysisUtils.evaluateExpression(symbolTable, method, child, reports, true);
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

    public void visitConditionalStatement(SymbolMethod method, JmmNode node) {
        List<JmmNode> children = node.getChildren();
        SemanticAnalysisUtils.evaluatesToBoolean(symbolTable, method, children.get(0), this.reports);
    }

    public void visitAssign(SymbolMethod method, JmmNode node) {
        List<JmmNode> children = node.getChildren();
        if (children.size() != 2) return; // ver isto
        if (!children.get(0).getKind().equals("Expression") || !children.get(1).getKind().equals("Expression"))
            return; //ver isto

        Type leftOperandType = SemanticAnalysisUtils.evaluateExpression(symbolTable, method, children.get(0), reports, false);
        if (leftOperandType == null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(children.get(0).get("line")), Integer.parseInt(children.get(0).get("col")), "unexpected type in left assigned operator"));
            return;
        }

        Type rightOperandType = SemanticAnalysisUtils.evaluateExpression(symbolTable, method, children.get(1), reports, true);

        if (rightOperandType == null)
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(children.get(1).get("line")), Integer.parseInt(children.get(1).get("col")), "unexpected type in right assigned operator"));
        else if (rightOperandType.getName().equals("Accepted")) return;
        else if (!leftOperandType.getName().equals(rightOperandType.getName()))
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(children.get(1).get("line")), Integer.parseInt(children.get(1).get("col")), "unexpected type in right assigned operator: should be " + leftOperandType.getName() + " but it is " + rightOperandType.getName()));
        if (rightOperandType != null && leftOperandType.isArray())
            if (!rightOperandType.isArray())
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(children.get(1).get("line")), Integer.parseInt(children.get(1).get("col")), "expected array type in right assigned operator"));

    }

}
