import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import symbolTable.GrammarMethod;
import symbolTable.GrammarSymbol;
import symbolTable.GrammarSymbolTable;
import symbolTable.GrammarType;

import java.util.ArrayList;
import java.util.List;

public class OptimizationVisitor extends AJmmVisitor<Boolean, Boolean> {
    private final GrammarSymbolTable symbolTable;
    private GrammarMethod currentMethod;

    public OptimizationVisitor(GrammarSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        addVisit("MethodDeclaration", this::visitMethod);
        addVisit("Return", this::visitReturn);
        addVisit("MethodBody", this::visitMethodBody);
        addVisit("Statement", this::visitStatement);
        addVisit("IfElse", this::visitFirstChild);
        addVisit("Assign", this::visitAssign);
        addVisit("IfExpression", this::visitFirstChild);
        addVisit("Expression", this::visitFirstChild);
        addVisit("Call", this::visitCall);
        addVisit("MethodCall", this::visitMethodCall);
        addVisit("Not", this::visitOperation);
        addVisit("And", this::visitOperation);
        addVisit("Less", this::visitOperation);
        addVisit("Add", this::visitOperation);
        addVisit("Sub", this::visitOperation);
        addVisit("Mult", this::visitOperation);
        addVisit("Div", this::visitOperation);
        addVisit("FinalTerms", this::visitFinalTerms);
        addVisit("ArrayAccess", this::visitArrayAccess);
        addVisit("WhileStatement", this::visitWhile);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean defaultVisit(JmmNode node, Boolean dummy) {
        for (JmmNode child : node.getChildren())
            visit(child);
        return dummy;
    }

    private Boolean visitWhile(JmmNode node, Boolean dummy) {
        return dummy;
    }

    private Boolean visitMethod(JmmNode node, Boolean dummy) {
        List<JmmNode> children = node.getChildren();

        StringBuilder methodInfo = new StringBuilder();
        Boolean alreadyInBody = false;

        for (int i = 0; i < children.size(); i++) {
            JmmNode child = children.get(i);
            if (child.getKind().equals("LParenthesis")) { // parameters
                if (!alreadyInBody) {
                    List<JmmNode> parameters = new ArrayList<>();
                    while (true) {
                        i++;
                        JmmNode aux = children.get(i);
                        if (aux.getKind().equals("RParenthesis")) break;
                        parameters.add(aux);
                    }
                    methodInfo.append(semanticAnalysis.SemanticAnalysisUtils.getTypeParameters(parameters));
                }
            } else if (child.getKind().equals("Main")) {
                methodInfo.append("main(");
            } else if (child.getKind().contains("Identifier")) {
                String methodName = child.getKind().replaceAll("'", "").replace("Identifier ", "");
                methodInfo.append(methodName).append("(");
            } else if (child.getKind().equals("MethodBody")) {
                methodInfo.append(")");
                this.currentMethod = symbolTable.getMethodByInfo(methodInfo.toString());
                visit(child);
                alreadyInBody = true;
            } else if (child.getKind().equals("Return")) {
                visit(child);
            }
        }

        this.currentMethod = null;
        return dummy;
    }

    private Boolean visitReturn(JmmNode node, Boolean dummy) {
        visit(node.getChildren().get(0).getChildren().get(0));
        return dummy;
    }

    private Boolean visitMethodBody(JmmNode node, Boolean dummy) {
        for (JmmNode child : node.getChildren()) visit(child);
        return dummy;
    }

    private Boolean visitStatement(JmmNode node, Boolean dummy) {
        for (JmmNode child : node.getChildren()) visit(child);
        return dummy;
    }

    private Boolean visitFirstChild(JmmNode node, Boolean dummy) {
        visit(node.getChildren().get(0));
        return dummy;
    }

    private Boolean visitCall(JmmNode node, Boolean dummy) {
        JmmNode secondChild = node.getChildren().get(1);

        if (secondChild.getKind().equals("MethodCall"))
            return visit(secondChild);

        return dummy;
    }

    private Boolean visitMethodCall(JmmNode node, Boolean dummy) {
        for (int i = 1; i < node.getChildren().size(); i++) {
            visit(node.getChildren().get(i).getChildren().get(0));
        }
        return dummy;
    }

    private Boolean visitAssign(JmmNode node, Boolean dummy) {
        visit(node.getChildren().get(1));
        JmmNode lhsVariable = node.getChildren().get(0).getChildren().get(0).getChildren().get(0);
        JmmNode rhsVariable = node.getChildren().get(1).getChildren().get(0).getChildren().get(0);
        if (lhsVariable.getKind().contains("Identifier") && rhsVariable.getKind().contains("Number")) {
            String varName = lhsVariable.getKind().replaceAll("'", "").replace("Identifier ", "");
            int value = Integer.parseInt(rhsVariable.getKind().replaceAll("'", "").replace("Number ", ""));
            if (currentMethod.hasVariable(new GrammarSymbol(new GrammarType("Int", false), varName))) {
                currentMethod.updateLocalVariable(new GrammarSymbol(new GrammarType("Int", false), varName), value);
            }
        }
        return dummy;
    }

    private Boolean visitOperation(JmmNode node, Boolean dummy) {
        switch (node.getKind()) {
            case "Add":
            case "Sub":
            case "Mult":
            case "Div":
            case "And":
            case "Less":
                visit(node.getChildren().get(0));
                visit(node.getChildren().get(1));
                break;
            case "Not":
                visit(node.getChildren().get(0));
                break;
        }
        return dummy;
    }

    private Boolean visitFinalTerms(JmmNode node, Boolean dummy) {
        JmmNode finalTerm = node.getChildren().get(0);

        if (finalTerm.getKind().contains("Identifier")) {
            String varName = finalTerm.getKind().replaceAll("'", "").replace("Identifier ", "");
            Integer value = currentMethod.getLocalVariable(varName);
            if (value != null) {
                JmmNodeImpl newChild = new JmmNodeImpl("Number '" + value + "'");
                for (JmmNode child : finalTerm.getChildren())
                    newChild.add(child);
                newChild.put("col", finalTerm.get("col"));
                newChild.put("line", finalTerm.get("line"));
                node.removeChild(finalTerm);
                node.add(newChild);
            }

        } else if (finalTerm.getKind().equals("Expression"))
            visit(finalTerm);

        return dummy;
    }

    private Boolean visitArrayAccess(JmmNode node, Boolean dummy) {
        visit(node.getChildren().get(1));
        return dummy;
    }
}
