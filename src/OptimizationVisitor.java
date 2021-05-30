import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;

import java.util.ArrayList;
import java.util.List;

public class OptimizationVisitor extends AJmmVisitor<Boolean, Boolean> {
    private final GrammarSymbolTable symbolTable;
    private SymbolMethod currentMethod;

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
    }

    private boolean visitMethod(JmmNode node, boolean dummy) {
        List<JmmNode> children = node.getChildren();

        StringBuilder methodInfo = new StringBuilder();
        boolean alreadyInBody = false;

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
                    methodInfo.append(SemanticAnalysisUtils.getTypeParameters(parameters));
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

    private boolean visitReturn(JmmNode node, boolean dummy) {
        visit(node.getChildren().get(0).getChildren().get(0));
        return dummy;
    }

    private boolean visitMethodBody(JmmNode node, boolean dummy) {
        for (JmmNode child : node.getChildren()) visit(child);
        return dummy;
    }

    private boolean visitStatement(JmmNode node, boolean dummy) {
        for (JmmNode child : node.getChildren()) visit(child);
        return dummy;
    }

    private boolean visitFirstChild(JmmNode node, boolean dummy) {
        visit(node.getChildren().get(0));
        return dummy;
    }

    private boolean visitCall(JmmNode node, boolean dummy) {
        JmmNode secondChild = node.getChildren().get(1);

        if (secondChild.getKind().equals("MethodCall"))
            return visit(secondChild);

        return dummy;
    }

    private boolean visitMethodCall(JmmNode node, boolean dummy) {
        //TODO: Change to checking if args are local variables and replacing them with their values
        //List<String> args = getMethodArgs(node, result);
        return dummy;
    }

    private boolean visitAssign(JmmNode node, boolean dummy) {
        //TODO: Check if right side are replaceable, save value for the variable on the left side in the symbol table
        return dummy;
    }

    private boolean visitOperation(JmmNode node, boolean dummy) {
        switch (node.getKind()) {
            case "Add":
            case "Sub":
            case "Mult":
            case "Div":
            case "And":
            case "Less":
                //TODO: Check if left or right operand can be replaced by their value
                JmmNode leftOperand = node.getChildren().get(0);
                JmmNode rightOperand = node.getChildren().get(1);
                break;
            case "Not":
                //TODO: Check if operand can be replaced by its value
                JmmNode operand = node.getChildren().get(0);
                break;
        }
        return dummy;
    }

    private boolean visitFinalTerms(JmmNode node, boolean dummy) {
        JmmNode child = node.getChildren().get(0);

        if (child.getKind().contains("Identifier")) {
            //TODO: Check if the variable can be replaced by its value
            /*variable = OLLIRUtils.getIdentifier(child, symbolTable, currentMethod);
            if (!value.contains("putfield") && !value.contains("getfield"))
                result.appendCode(OLLIRUtils.getIdentifierType(child, symbolTable, currentMethod));*/
        } else if (child.getKind().equals("Expression"))
            visit(child);

        return dummy;
    }

    private boolean visitArrayAccess(JmmNode node, boolean dummy) {
        visit(node.getChildren().get(1));
        return dummy;
    }
}
