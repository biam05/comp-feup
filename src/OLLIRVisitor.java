import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OLLIRVisitor extends AJmmVisitor<StringBuilder, String> {
    private final GrammarSymbolTable symbolTable;
    private final List<Report> reports;
    private String code;
    private int var_temp = 0;
    private SymbolMethod currentMethod;
    private int loop_counter = 1;

    public OLLIRVisitor(GrammarSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.reports = new ArrayList<>();
        this.code = OLLIRUtils.init(symbolTable.getClassName(), symbolTable.getFields());
        addVisit("MethodDeclaration", this::visitMethod);
        addVisit("Statement", this::visitStatement);
        addVisit("Expression", this::visitExpression);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("Return", this::visitReturn);
        addVisit("Assign", this::visitAssign);
        addVisit("MethodBody", this::visitMethodBody);
        addVisit("FinalTerms", this::visitFinalTerms);
        addVisit("Call", this::visitCall);
        addVisit("Add", this::visitOperation);
        addVisit("Sub", this::visitOperation);
        addVisit("Mult", this::visitOperation);
        addVisit("Div", this::visitOperation);
        addVisit("Not", this::visitOperation);
        addVisit("And", this::visitOperation);
        addVisit("Less", this::visitOperation);
        addVisit("ArrayAccess", this::visitArrayAccess);
        addVisit("WhileStatement", this::visitWhile);
        addVisit("IfElse", this::visitIfElse);
        setDefaultVisit(this::defaultVisit);
    }

    public List<Report> getReports() {
        return reports;
    }

    public String getCode() {
        return code;
    }

    private String defaultVisit(JmmNode node, StringBuilder stringBuilder) {
        List<JmmNode> nodes = node.getChildren();
        StringBuilder res = new StringBuilder();
        for (JmmNode child : nodes) {
            res.append(visit(child));
        }

        return res.toString();
    }

    private String visitMethod(JmmNode node, StringBuilder stringBuilder) {
        var_temp = 0;
        StringBuilder res = new StringBuilder();
        List<JmmNode> children = node.getChildren();
        boolean ret = false;
        boolean alreadyInBody = false;

        StringBuilder methodInfo = new StringBuilder();

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
                res.append(OLLIRUtils.methodDeclaration(this.currentMethod));
                res.append("{\n").append(visit(child)).append("\n");
                alreadyInBody = true;
            } else if (child.getKind().equals("Return")) {
                ret = true;
                res.append(visit(child));
                res.append("\n}\n\n");
            }
        }

        if (!ret) {
            res.append(OLLIRUtils.returnVoid());
            res.append("}\n\n");
        }

        this.currentMethod = null;
        this.code += res.toString();
        return res.toString();
    }

    private String visitReturn(JmmNode node, StringBuilder stringBuilder) {
        String result = checkReturnTemporary(node.getChildren().get(0).getChildren().get(0));
        if (result.equals(""))
            return OLLIRUtils.returnTemplate(visit(node.getChildren().get(0)), OLLIRUtils.getReturnTypeExpression(visit(node.getChildren().get(0))));
        else
            return OLLIRUtils.returnTemplate("aux" + var_temp + OLLIRUtils.getReturnTypeExpression(visit(node.getChildren().get(0))) + "\n" + result, OLLIRUtils.getReturnTypeExpression(visit(node.getChildren().get(0))));
    }

    private String visitAssign(JmmNode node, StringBuilder stringBuilder) {
        List<JmmNode> children = node.getChildren();
        JmmNode leftchild = children.get(0);
        JmmNode rightchild = children.get(1);

        String left = visit(leftchild);
        String right = visit(rightchild);

        if (left.contains("putfield")) {
            String[] args = left.split(" ");
            String var = args[2];

            StringBuilder aux = new StringBuilder();
            String result = checkReturnTemporary(rightchild.getChildren().get(0));
            if (result.equals(""))
                aux.append(visit(rightchild));
            else
                aux.append("aux").append(var_temp).append(OLLIRUtils.getReturnTypeExpression(visit(rightchild))).append("\n").append(result);

            String[] temporary = aux.toString().split("\\n");

            StringBuilder res = new StringBuilder();
            for (int i = temporary.length - 1; i >= 1; i--) {
                res.append(temporary[i]).append(";\n");
            }
            return res + OLLIRUtils.putField(var, temporary[0]);
        } else {
            String type = OLLIRUtils.getReturnTypeExpression(left);
            return OLLIRUtils.assign(left, type, right);
        }
    }


    private String visitMethodBody(JmmNode node, StringBuilder stringBuilder) {
        StringBuilder res = new StringBuilder();
        List<JmmNode> children = node.getChildren();

        for (JmmNode child : children) res.append(visit(child));

        return res.toString();
    }

    private String visitStatement(JmmNode node, StringBuilder stringBuilder) {
        StringBuilder res = new StringBuilder();
        List<JmmNode> children = node.getChildren();

        for (JmmNode child : children) {
            String str = visit(child);
            if (!str.endsWith(";") && str.contains("invokestatic")) {
                str += ";";
            }
            res.append(str);
        }


        return res.toString();
    }

    private String visitWhile(JmmNode node, StringBuilder stringBuilder) {
        StringBuilder res = new StringBuilder();
        List<JmmNode> children = node.getChildren();
        JmmNode condition = children.get(0);
        JmmNode body = children.get(1);

        res.append("\nLoop").append(loop_counter).append(":\n");
        String cond = visit(condition);

        if (cond.contains("\n")) {
            String[] args = cond.split("\n");
            res.append(args[1]).append("\n");
            res.append("if (").append(args[0]).append(") goto Body").append(loop_counter).append(";\n");
        } else {
            res.append("if (").append(cond).append(")").append("goto Body").append(loop_counter).append(";\n");
        }

        res.append("goto EndLoop").append(loop_counter).append(";");
        res.append("\nBody").append(loop_counter).append(":\n").append(visit(body));
        res.append("\nEndLoop").append(loop_counter).append(":\n");

        loop_counter++;
        return res.toString();
    }

    private String visitExpression(JmmNode node, StringBuilder stringBuilder) {
        return visit(node.getChildren().get(0));
    }

    private String visitClassDeclaration(JmmNode node, StringBuilder stringBuilder) {
        StringBuilder res = new StringBuilder();
        for (JmmNode child : node.getChildren()) res.append(visit(child));

        return res.toString();
    }


    //TODO : newintarrayexpression and its temporary variables
    private String visitFinalTerms(JmmNode node, StringBuilder stringBuilder) {
        StringBuilder result = new StringBuilder();
        List<JmmNode> children = node.getChildren();
        JmmNode child = children.get(0);
        String ret, res = "", value;
        if (child.getKind().contains("Number")) {
            ret = ".i32";
            value = child.getKind().replace("Number", "").replaceAll("'", "").replaceAll(" ", "");
            res = value + ret;
        } else if (child.getKind().equals("NewIntArrayExpression")) {
            String var = visit(child.getChildren().get(0));
            //TODO: check if var needs temporary variable
            res = "new(array," + var + ").array.i32";
        } else if (child.getKind().contains("NewIdentifier")) {
            value = child.getKind().replaceAll("'", "").replace("NewIdentifier ", "").trim();
            res = "new(" + value + ")." + value; // new(myClass).myClass
            res += ";\n" + "invokespecial(" + visit(node.getParent().getParent().getChildren().get(0)) + ", \"<init>\").V";
        } else if (child.getKind().contains("Identifier")) {
            value = OLLIRUtils.getIdentifier(child, symbolTable, currentMethod);
            res = value;
            if (!value.contains("putfield") && !value.contains("getfield"))
                res += OLLIRUtils.getIdentifierType(child, symbolTable, currentMethod);
        } else if (child.getKind().equals("True") || child.getKind().equals("False")) {
            ret = ".bool";
            int bool = child.getKind().equals("True") ? 1 : 0;
            res = bool + ret;
        } else if (child.getKind().equals("Expression")) {
            res = visit(child);
        } else if (child.getKind().equals("This")) {
            res = "this";
        }

        result.append(res);
        return result.toString();
    }

    private String visitCall(JmmNode node, StringBuilder stringBuilder) {
        List<JmmNode> children = node.getChildren();
        JmmNode left = children.get(0);
        JmmNode right = children.get(1);

        if (left.getKind().equals("FinalTerms") && right.getKind().equals("MethodCall"))
            return visitMethodCall(node, left, right);
        else if (left.getKind().equals("FinalTerms") && right.getKind().equals("Length")) return visitLength(left);
        return "";
    }


    private String visitMethodCall(JmmNode call, JmmNode finalTerm, JmmNode method) {
        String identifier = visit(finalTerm);
        String invokeType = OLLIRUtils.getInvokeType(identifier, method.getChildren().get(0), symbolTable);

        switch (invokeType) {
            case "virtual":
                List<String> temporary = new ArrayList<>();
                List<String> args = getMethodArgs(method);
                System.out.println("args = " + args + "!");

                for (String s : args) {
                    String[] splitted = s.split("\\n");
                    temporary.addAll(Arrays.asList(splitted).subList(1, splitted.length));
                }

                String methodInfo = OLLIRUtils.getMethodInfo(method, args);
                System.out.println("method info = " + methodInfo);
                SymbolMethod met = symbolTable.getMethodByInfo(methodInfo);
                StringBuilder res = new StringBuilder();
                for (String temp : temporary) {
                    res.append(temp).append(";\n");
                }
                return res + OLLIRUtils.invokeStaticVirtual(false, identifier, OLLIRUtils.getMethodName(method.getChildren().get(0)), args, met.getReturnType().toOLLIR()) + ";\n";
            case "static":
                List<String> arg = getMethodArgs(method);
                String returnType = getStaticReturnType(call);
                return OLLIRUtils.invokeStaticVirtual(true, identifier, OLLIRUtils.getMethodName(method.getChildren().get(0)), arg, returnType);
            default:
                return "";
        }
    }

    public List<String> getMethodArgs(JmmNode method) {
        List<JmmNode> children = method.getChildren();
        List<String> args = new ArrayList<>();
        for (int i = 1; i < children.size(); i++) {
            String result = checkReturnTemporary(children.get(i).getChildren().get(0));
            if (result.equals(""))
                args.add(visit(children.get(i)));
            else
                args.add("aux" + var_temp + OLLIRUtils.getReturnTypeExpression(visit(children.get(i).getChildren().get(0))) + "\n" + result);
        }
        return args;
    }

    public String getStaticReturnType(JmmNode method) {
        if (method.getParent().getKind().equals("Assign")) {
            JmmNode brother = method.getParent().getChildren().get(0);
            return OLLIRUtils.getReturnTypeExpression(visit(brother));
        } else return ".V";
    }

    //TODO: ver quando sao necessarias variaveis temporarias para o array Ex: oi.getArray().length
    private String visitLength(JmmNode identifier) {
        String array = visit(identifier);
        String res = checkReturnTemporary(identifier); //ver se o identifier precisa de variavel
        return "arraylength(" + array + ").i32;";
    }


    private String visitOperation(JmmNode node, StringBuilder stringBuilder) {
        String resultLeft, resultRight;
        switch (node.getKind()) {
            case "Add":
            case "Sub":
            case "Mult":
            case "Div":
            case "And":
            case "Less":
                resultLeft = checkReturnTemporary(node.getChildren().get(0));
                String leftAux = "aux" + var_temp;
                resultRight = checkReturnTemporary(node.getChildren().get(1));
                String rightAux = "aux" + var_temp;
                System.out.println("result left : " + resultLeft + "| result right: " + resultRight + "|");

                if (resultLeft.equals("") && resultRight.equals(""))
                    return visit(node.getChildren().get(0)) + " " + OLLIRUtils.getOperationType(node) + " " + visit(node.getChildren().get(1));
                else if (resultLeft.equals("")) {
                    //System.out.println("resultleft vaizo: " + visit(node.getChildren().get(0)) + " | " + OLLIRTemplates.getOperationType(node) + " | " + rightAux + OLLIRTemplates.getReturnTypeExpression(visit(node.getChildren().get(0))) + " | " + resultRight + " |");
                    return resultRight + ";\n" + visit(node.getChildren().get(0)) + " " + OLLIRUtils.getOperationType(node) + " " + rightAux + OLLIRUtils.getReturnTypeExpression(visit(node.getChildren().get(0)));
                }
                else if (resultRight.equals("")) {

                    return resultLeft + ";\n" + leftAux + OLLIRUtils.getReturnTypeExpression(visit(node.getChildren().get(0))) + " " + OLLIRUtils.getOperationType(node) + " " + visit(node.getChildren().get(1));
                }
                else
                    return leftAux + OLLIRUtils.getReturnTypeExpression(visit(node.getChildren().get(0))) + " " + OLLIRUtils.getOperationType(node) + " " + rightAux + OLLIRUtils.getReturnTypeExpression(visit(node.getChildren().get(0))) + ";\n" + resultRight + ";\n" + resultLeft;
            case "Not":
                resultRight = checkReturnTemporary(node.getChildren().get(0));
                if (resultRight.equals(""))
                    return OLLIRUtils.getOperationType(node) + " " + visit(node.getChildren().get(0));
                else
                    return OLLIRUtils.getOperationType(node) + " " + resultRight;
            default: // FinalTerms
                return "";
        }
    }

    private String visitArrayAccess(JmmNode node, StringBuilder stringBuilder) {
        StringBuilder res = new StringBuilder();
        JmmNode identifier = node.getChildren().get(0);
        JmmNode access = node.getChildren().get(1);
        String i = visit(identifier);
        String a = visit(access);

        String type = OLLIRUtils.getReturnTypeExpression(a);
        if (type.length() - type.replaceAll("\\.", "").length() > 1) type = type.substring(type.lastIndexOf("."));

        int ident;
        try {
            int index = a.indexOf(".");
            ident = Integer.parseInt(a.substring(0, index));
            var_temp++;
            a = "aux" + var_temp + ".i32";
            res.append(a).append(" :=.i32 ").append(ident).append(".i32;\n");
        } catch (NumberFormatException ignored) {
        }

        res.append(OLLIRUtils.getIdentifierExpression(i)).append("[").append(a).append("]").append(type);

        return res.toString();
    }

    private String checkReturnTemporary(JmmNode expression) {
        StringBuilder result = new StringBuilder();
        if (OLLIRUtils.hasOperation(expression) || OLLIRUtils.hasCall(expression) || OLLIRUtils.hasField(expression, symbolTable, currentMethod)) {
            String aux = visit(expression);

            String type;
            if (expression.getKind().equals("Call") || expression.getKind().contains("FinalTerms"))
                type = OLLIRUtils.getReturnTypeExpression(visit(expression));
            else
                type = OLLIRUtils.getReturnTypeExpression(visit(expression.getChildren().get(0)));

            var_temp++;
            result.append("aux").append(var_temp).append(type).append(" :=").append(type).append(" ").append(aux);
        }
        return result.toString();
    }

    private String visitIfElse(JmmNode node, StringBuilder stringBuilder) {
        StringBuilder res = new StringBuilder();

        List<JmmNode> children = node.getChildren();
        int if_counter = 1;
        res.append("\nif (").append(visit(children.get(0))).append(") goto else").append(if_counter).append(";\n");
        res.append(visit(children.get(1)));
        res.append("\ngoto endif").append(if_counter).append(";\n");
        res.append("else").append(if_counter).append(":\n");
        res.append(visit(children.get(3)));
        res.append("\nendif").append(if_counter).append(":\n");

        return res.toString();
    }

}
