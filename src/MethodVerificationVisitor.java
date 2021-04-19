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

public class MethodVerificationVisitor extends PreorderJmmVisitor<Boolean, Boolean> {
    private final GrammarSymbolTable symbolTable;
    private final List<Report> reports;
    private final SymbolMethod method;

    public MethodVerificationVisitor(GrammarSymbolTable symbolTable, SymbolMethod method) {
        this.symbolTable = symbolTable;
        this.reports = new ArrayList<>();
        this.method = method;
        addVisit("Expression", this::visitExpression);
    }

    public GrammarSymbolTable getSymbolTable() {
        return symbolTable;
    }

    public List<Report> getReports() {
        return reports;
    }

    public Boolean visitExpression(JmmNode node, Boolean dummy) {
        List<JmmNode> children = node.getChildren();
        for (int index = 0; index < children.size(); index++) {
            JmmNode child = children.get(index);
            if (child.getKind().equals("Period")) {
                index++;
                // Get method name after period
                String methodName;
                if (children.get(index).getKind().contains("Identifier"))
                    methodName = children.get(index).getKind().replaceAll("'", "").replace("Identifier ", "");
                else continue;
                index -= 2;
                // Get var name before period
                JmmNode aux = children.get(index).getChildren().get(0);
                String varName = aux.getKind().replaceAll("'", "").replace("Identifier ", "");
                index += 3;
                List<JmmNode> parameters = new ArrayList<>();
                // Get parameters passed on method call
                if (children.get(index).getKind().equals("LParenthesis")) {
                    while (true) {
                        index++;
                        aux = children.get(index);
                        if (aux.getKind().equals("RParenthesis")) break;
                        parameters.add(aux);
                    }
                }
                int line = Integer.parseInt(child.get("line"));
                int col = Integer.parseInt(child.get("col"));

                // Call function to check if var exists and has method methodName and check argument types and number
                if (!isValidMethodCall(method, varName, methodName, parameters)) {
                    Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Invalid method call");
                    reports.add(report);
                    System.out.println(report);
                }
            }
        }
        return true;
    }

    public Boolean isValidMethodCall(SymbolMethod currentMethod, String varName, String methodName, List<JmmNode> parameters) {
        for (String importName: symbolTable.getImports()) {
            if(varName.equals(importName)) return true;
        }
        if (currentMethod.getLocalVariables().size() != 0)
            for (Symbol var: currentMethod.getLocalVariables()) {
                if ((var.getName().equals(varName) && var.getType().getName().equals(this.symbolTable.getClassName())) || varName.equals("This")) {
                    for (SymbolMethod method: symbolTable.getMethodsAndParameters()) {
                        if (method.getName().equals(methodName)) {
                            if (method.getParameters().size() != parameters.size())
                                return false;
                            for (int i = 0; i < method.getParameters().size(); i++)
                                if ((!method.getParameters().get(i).getType().equals(getType(parameters.get(i), currentMethod))) && (!getType(parameters.get(i), currentMethod).equals(new Type("imported", false))))
                                    return false;
                            return true;
                        }
                    }
                    return symbolTable.getSuper() != null;
                }
            }
        else if (varName.equals("This")) {
            for (SymbolMethod method : symbolTable.getMethodsAndParameters()) {
                if (method.getName().equals(methodName)) {
                    if (method.getParameters().size() != parameters.size())
                        return false;
                    for (int i = 0; i < method.getParameters().size(); i++)
                        if ((!method.getParameters().get(i).getType().equals(getType(parameters.get(i), currentMethod))) && (!getType(parameters.get(i), currentMethod).equals(new Type("imported", false))))
                            return false;
                    return true;
                }
            }
            return symbolTable.getSuper() != null;
        }
        return false;
    }

    public Type getType(JmmNode node, SymbolMethod currentMethod) {
        visitExpression(node, true);
        if (node.getChildren().size() == 1) {
            String varName = node.getChildren().get(0).getChildren().get(0).getKind().replaceAll("'", "").replace("Identifier ", "");
            for (Symbol var : currentMethod.getLocalVariables())
                if (var.getName().equals(varName))
                    return var.getType();
        }
        else
            for (int i = 0; i < node.getChildren().size(); i++)
                if (node.getChildren().get(i).getKind().equals("Period")) {
                    i++;
                    String methodName;
                    if (node.getChildren().get(i).getKind().contains("Identifier"))
                        methodName = node.getChildren().get(i).getKind().replaceAll("'", "").replace("Identifier ", "");
                    else continue;
                    for (SymbolMethod method : symbolTable.getMethodsAndParameters())
                        if (method.getName().equals(methodName))
                            return method.getReturnType();
                    return new Type("imported", false);
                }
        return new Type("", false);
    }
}
