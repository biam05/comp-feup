import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class OLLIRVisitor extends AJmmVisitor<String, OllirObject> {
    private final GrammarSymbolTable symbolTable;
    private final List<Report> reports;
    private final OllirObject code;
    private int loop_counter = 0;
    private int if_counter = 0;
    private int var_temp = 0;
    private SymbolMethod currentMethod;

    public OLLIRVisitor(GrammarSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.reports = new ArrayList<>();
        this.code = new OllirObject(OLLIRUtils.init(symbolTable.getClassName(), symbolTable.getSuper(), symbolTable.getFields()));
        addVisit("MethodDeclaration", this::visitMethod);
        addVisit("Statement", this::visitStatement);
        addVisit("Expression", this::visitExpression);
        addVisit("IfExpression", this::visitExpressionParent);
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
        return code.getCode();
    }

    private OllirObject defaultVisit(JmmNode node, String dummy) {
        List<JmmNode> nodes = node.getChildren();
        OllirObject result = new OllirObject("");
        for (JmmNode child : nodes)
            result.appendToCode(visit(child));


        return result;
    }

    private OllirObject visitMethod(JmmNode node, String dummy) {
        var_temp = 0;

        OllirObject result = new OllirObject("");
        List<JmmNode> children = node.getChildren();

        StringBuilder methodInfo = new StringBuilder();
        boolean alreadyInBody = false;
        boolean hasReturn = false;

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
                result.appendCode(OLLIRUtils.methodDeclaration(this.currentMethod));
                result.appendCode("{\n");
                result.append(visit(child));
                result.appendCode("\n"); //ver isto

                alreadyInBody = true;
            } else if (child.getKind().equals("Return")) {
                hasReturn = true;
                result.appendToCode(visit(child));
                result.appendCode("\n}\n\n");
            }
        }

        if (!hasReturn) result.appendCode(OLLIRUtils.returnVoid() + "}\n\n");

        this.currentMethod = null;
        this.code.appendCode(result.getCode());

        return result;
    }


    private OllirObject visitReturn(JmmNode node, String dummy) {
        JmmNode returnExpression = node.getChildren().get(0).getChildren().get(0);
        OllirObject result = visit(returnExpression);

        result.setCode(checkExpressionTemporary("", result));
        result.getReturn();
        return result;
    }

    private OllirObject visitMethodBody(JmmNode node, String dummy) {
        OllirObject result = new OllirObject("");
        List<JmmNode> children = node.getChildren();
        for (JmmNode child : children) result.appendToCode(visit(child));
        return result;
    }

    private OllirObject visitStatement(JmmNode node, String dummy) {
        OllirObject result = new OllirObject("");
        List<JmmNode> children = node.getChildren();

        for (JmmNode child : children) {
            OllirObject aux = visit(child);
            List<JmmNode> childrenC = child.getChildren();

            if (!aux.getCode().isEmpty() && !aux.getCode().endsWith(";") && !aux.getCode().endsWith(";\n") &&
                    !child.getKind().equals("WhileStatement") && !child.getKind().equals("IfElse") &&
                    !childrenC.get(childrenC.size() - 1).getKind().equals("WhileStatement") && !childrenC.get(childrenC.size() - 1).getKind().equals("IfElse")) {
                aux.appendCode(";\n");
            }

            result.appendToCode(aux);
        }
        return result;
    }

    private OllirObject visitExpressionParent(JmmNode node, String dummy) {
        return visit(node.getChildren().get(0));
    }

    private OllirObject visitExpression(JmmNode node, String dummy) {
        return visit(node.getChildren().get(0));
    }

    private OllirObject visitClassDeclaration(JmmNode node, String dummy) {
        OllirObject result = new OllirObject("");
        for (JmmNode child : node.getChildren()) visit(child);
        return result;
    }

    private OllirObject visitAssign(JmmNode node, String dummy) {
        List<JmmNode> children = node.getChildren();
        JmmNode leftchild = children.get(0);
        JmmNode rightchild = children.get(1);

        OllirObject left = visit(leftchild);
        OllirObject right = visit(rightchild);
        String leftC = left.getCode();
        String rightC = right.getCode();

        OllirObject result = new OllirObject("");

        if (leftC.contains("putfield")) {
            leftC = leftC.replace("putfield = ", "");
            rightC = checkExpressionTemporary("", right);
            result.appendCode(OLLIRUtils.putField(leftC, rightC));
        } else {
            String type = OLLIRUtils.getReturnTypeExpression(leftC);
            if (rightC.startsWith("invoke") && rightC.endsWith(".V")) {
                rightC = rightC.substring(0, rightC.lastIndexOf(".V"));
                rightC += type;
            }
            result.appendCode(OLLIRUtils.assign(leftC, type, rightC));
        }

        result.appendTemps(left);
        result.appendTemps(right);

        if (right.getCode().contains("new(") && !right.getCode().contains("new(array,"))
            result.addBelowTemp(OLLIRUtils.invokeSpecial(left.getCode()));
        return result;
    }

    private OllirObject visitWhile(JmmNode node, String dummy) {
        loop_counter++;
        int loop_c = loop_counter;
        OllirObject result = new OllirObject("");

        List<JmmNode> children = node.getChildren();
        JmmNode condition = children.get(0);
        JmmNode body = children.get(1);

        result.appendCode("\nLoop" + loop_c + ":\n");
        OllirObject cond = visit(condition);
        cond.setCode(checkExpressionTemporary("", cond));
        result.appendCode(cond.getAboveTemp());

        result.appendCode("if (" + cond.getCode() + " ==.bool 0.bool) " + "goto EndLoop" + loop_c + ";\n");
        result.append(visit(body));
        result.appendCode("goto Loop" + loop_c + ";\nEndLoop" + loop_c + ":\n");

        return result;
    }

    private OllirObject visitIfElse(JmmNode node, String dummy) {
        if_counter++;
        int if_c = if_counter;

        OllirObject result = new OllirObject("");
        List<JmmNode> children = node.getChildren();
        OllirObject aux = visit(children.get(0));
        result.appendTemps(aux);
        String code = checkExpressionTemporary(aux.getCode(), result);

        result.appendCode("\nif (" + code + " ==.bool 0.bool) goto else" + if_c + ";\n");

        OllirObject aux1 = visit(children.get(1));
        result.append(aux1);

        result.appendCode("\ngoto endif" + if_c + ";\n");
        result.appendCode("else" + if_c + ":\n");

        OllirObject aux2 = visit(children.get(3));
        result.append(aux2);
        result.appendCode("\nendif" + if_c + ":\n");

        return result;
    }

    private OllirObject visitFinalTerms(JmmNode node, String dummy) {
        OllirObject result = new OllirObject("");

        List<JmmNode> children = node.getChildren();
        JmmNode child = children.get(0);
        String value;


        if (child.getKind().contains("Number")) {
            value = child.getKind().replace("Number", "").replaceAll("'", "").replaceAll(" ", "");
            result.appendCode(value + ".i32");

        } else if (child.getKind().equals("NewIntArrayExpression")) {
            OllirObject var = visit(child.getChildren().get(0));
            var.setCode(checkExpressionTemporary("", var));
            result.appendTemps(var);
            result.appendCode("new(array, " + var.getCode() + ").array.i32");

        } else if (child.getKind().contains("NewIdentifier")) {
            value = child.getKind().replaceAll("'", "").replace("NewIdentifier ", "");
            result.appendCode("new(" + value + ")." + value); // new(myClass).myClass

        } else if (child.getKind().contains("Identifier")) {
            value = OLLIRUtils.getIdentifier(child, symbolTable, currentMethod);
            result.appendCode(value);
            if (!value.contains("putfield") && !value.contains("getfield"))
                result.appendCode(OLLIRUtils.getIdentifierType(child, symbolTable, currentMethod));


        } else if (child.getKind().equals("True") || child.getKind().equals("False")) {
            int bool = child.getKind().equals("True") ? 1 : 0;
            result.appendCode(bool + ".bool");

        } else if (child.getKind().equals("Expression"))
            result.append(visit(child));
        else if (child.getKind().equals("This"))
            result.appendCode("this");

        return result;
    }

    private OllirObject visitOperation(JmmNode node, String dummy) {
        OllirObject result = new OllirObject("");
        switch (node.getKind()) {
            case "Add":
            case "Sub":
            case "Mult":
            case "Div":
            case "And":
            case "Less":
                String resultLeft = checkExpressionTemporary(node.getChildren().get(0), result);
                resultLeft = checkExpressionTemporary(resultLeft, result);

                String resultRight = checkExpressionTemporary(node.getChildren().get(1), result);
                resultRight = checkExpressionTemporary(resultRight, result);

                result.appendCode(resultLeft + " " + OLLIRUtils.getOperationType(node) + " " + resultRight);
                return result;
            case "Not":
                String resultR = checkExpressionTemporary(node.getChildren().get(0), result);
                resultR = checkExpressionTemporary(resultR, result);
                result.appendCode(OLLIRUtils.getOperationType(node) + " " + resultR);

                return result;
            default: // FinalTerms
                return new OllirObject("");
        }
    }

    private OllirObject visitArrayAccess(JmmNode node, String dummy) {
        OllirObject result = new OllirObject("");

        JmmNode identifier = node.getChildren().get(0);
        JmmNode access = node.getChildren().get(1);
        OllirObject i = visit(identifier);
        OllirObject a = visit(access);

        result.appendTemps(i);
        result.appendTemps(a);

        String aCode = a.getCode();
        String type = OLLIRUtils.getReturnTypeExpression(aCode);

        int ident;
        aCode = checkExpressionTemporary(aCode, result);

        try {
            int index = aCode.indexOf(".");
            ident = Integer.parseInt(aCode.substring(0, index));
            var_temp++;
            aCode = "aux" + var_temp + ".i32";
            result.addAboveTemp(aCode + " :=.i32 " + ident + ".i32;");
        } catch (NumberFormatException ignored) {
        }

        String finalExp = checkExpressionTemporary(i.getCode(), result);
        aCode = checkExpressionTemporary(aCode, result);
        if (!finalExp.equals(i.getCode())) finalExp = "aux" + var_temp;
        else finalExp = OLLIRUtils.getIdentifierExpression(i.getCode());

        result.appendCode(finalExp + "[" + aCode + "]" + type);

        return result;
    }

    private OllirObject visitCall(JmmNode node, String dummy) {

        List<JmmNode> children = node.getChildren();
        JmmNode child1 = children.get(0);
        JmmNode child2 = children.get(1);

        if (child2.getKind().equals("MethodCall"))
            return visitMethodCall(node, child1, child2);
        else if (child2.getKind().equals("Length")) return visitLength(child1);

        return new OllirObject("");
    }

    private OllirObject visitMethodCall(JmmNode call, JmmNode firstChild, JmmNode method) {

        OllirObject result = new OllirObject("");
        OllirObject identifier = visit(firstChild);

        List<String> args = getMethodArgs(method, result);
        for (int i = 0; i < args.size(); i++) {
            String s = args.get(i);
            if (s.contains("new(array") || s.contains("[")) {
                var_temp++;
                String type = OLLIRUtils.getReturnTypeExpression(s);
                String var_name = "aux" + var_temp + type;
                result.addAboveTemp(var_name + " :=" + type + " " + s);
                args.set(i, var_name);
            }
        }

        String methodInfo = OLLIRUtils.getMethodInfo(method, args);
        String invokeType = OLLIRUtils.getInvokeType(identifier.getCode(), methodInfo, symbolTable);

        switch (invokeType) {
            case "virtual":
                SymbolMethod met = symbolTable.getMethodByInfo(methodInfo);
                String type = met.getReturnType().toOLLIR();

                String aux = identifier.getCode();
                if (identifier.getCode().contains("new(") && !identifier.getCode().contains("new(array,")) {
                    String identifierType = OLLIRUtils.getReturnTypeExpression(identifier.getCode());

                    var_temp++;
                    aux = "aux" + var_temp + identifierType;

                    result.addAboveTemp(aux + " :=" + identifierType + " " + identifier.getCode() + ";\n");
                    result.addAboveTemp(OLLIRUtils.invokeSpecial(aux));

                } else if (identifier.getCode().contains("invokevirtual")) {
                    String identifierType = OLLIRUtils.getReturnTypeExpression(identifier.getCode());

                    var_temp++;
                    aux = "aux" + var_temp + identifierType;
                    result.addAboveTemp(aux + " :=" + identifierType + " " + identifier.getCode() + ";\n");
                } else result.appendTemps(identifier);

                String methodCode = OLLIRUtils.invokeMethod(invokeType, aux, OLLIRUtils.getMethodName(method.getChildren().get(0)), args, type);
                result.appendCode(methodCode);
                return result;
            case "static":
            case "special":
                String returnType = getStaticReturnType(call);
                result.appendCode(OLLIRUtils.invokeMethod(invokeType, identifier.getCode(), OLLIRUtils.getMethodName(method.getChildren().get(0)), args, returnType));
                return result;
            default:
                return result;
        }
    }

    public List<String> getMethodArgs(JmmNode method, OllirObject res) {
        List<JmmNode> children = method.getChildren();
        List<String> args = new ArrayList<>();

        for (int i = 1; i < children.size(); i++) {
            String result = checkExpressionTemporary(children.get(i).getChildren().get(0), res);
            args.add(result);
        }

        return args;
    }

    public String getStaticReturnType(JmmNode method) {
        if (method.getParent().getKind().equals("Assign")) {
            JmmNode brother = method.getParent().getChildren().get(0);
            return OLLIRUtils.getReturnTypeExpression(visit(brother).getCode());
        } else return ".V";
    }

    private OllirObject visitLength(JmmNode identifier) {
        OllirObject result = new OllirObject("");
        String ident = checkExpressionTemporary(identifier, result);

        result.appendCode("arraylength(" + ident + ").i32");

        return result;
    }

    private String checkExpressionTemporary(JmmNode expression, OllirObject res) {
        OllirObject aux = visit(expression);
        res.appendTemps(aux);

        if (OLLIRUtils.hasOperation(expression) || OLLIRUtils.hasCall(expression) || OLLIRUtils.hasField(expression, symbolTable, currentMethod)) {
            String type;
            if (expression.getKind().equals("Call") || expression.getKind().contains("FinalTerms"))
                type = OLLIRUtils.getReturnTypeExpression(aux.getCode());
            else
                type = OLLIRUtils.getReturnTypeExpression(visit(expression.getChildren().get(0)).getCode());

            var_temp++;
            String temp_name = "aux" + var_temp + type;

            res.addAboveTemp(temp_name + " :=" + type + " " + aux.getCode());
            return temp_name;
        }
        return aux.getCode();
    }

    //used when we want the code in a single variable
    private String checkExpressionTemporary(String result, OllirObject res) {
        String code;

        if (result.equals("")) code = res.getCode();
        else code = result;

        String aux = code;

        if (!code.startsWith("invoke") && !code.contains("[") && !code.startsWith("arraylength(")) {
            List<String> auxiliar = new LinkedList<>(Arrays.asList(code.split(" ")));
            if (auxiliar.size() == 1) return code;
            code = auxiliar.get(1);
            aux = String.join(" ", auxiliar);
        }

        String type;

        if(aux.contains("<")) type = ".bool";
        else type = OLLIRUtils.getReturnTypeExpression(code);

        var_temp++;
        String temp_name = "aux" + var_temp + type;

        String temp = temp_name + " :=" + type + " " + aux;
        res.addAboveTemp(temp);
        return temp_name;
    }

}
