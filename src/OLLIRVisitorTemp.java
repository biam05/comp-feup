import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OLLIRVisitorTemp extends AJmmVisitor<String, OllirObject> {
    private final GrammarSymbolTable symbolTable;
    private final List<Report> reports;
    private final OllirObject code;
    private final int loop_counter = 1;
    private int if_counter = 1;
    private int var_temp = 0;
    private SymbolMethod currentMethod;

    public OLLIRVisitorTemp(GrammarSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.reports = new ArrayList<>();
        this.code = new OllirObject(OLLIRUtils.init(symbolTable.getClassName(), symbolTable.getFields()));
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
        System.out.println("method = " + node + ", " + children);
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
                result.appendToCode(visit(child));
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
        this.code.appendToCode(result);

        return result;
    }


    private OllirObject visitReturn(JmmNode node, String dummy) {
        JmmNode returnExpression = node.getChildren().get(0).getChildren().get(0);
        OllirObject result = visit(returnExpression);

        checkReturnTemporary(returnExpression, result);

        return result;
    }

    private void checkReturnTemporary(JmmNode node, OllirObject object) {
        //object.append(checkExpressionTemporary(node));
        System.out.println("return -> " + object.getCode());
        if (object.returnNeedsTemp()) {
            var_temp++;
            object.getReturn(var_temp);
        } else object.getReturn();
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
            if (!aux.getCode().isEmpty() && !aux.getCode().endsWith(";") && !aux.getCode().endsWith(";\n"))
                aux.appendCode(";\n");
            result.appendToCode(aux);
        }
        return result;
    }

    private OllirObject visitExpression(JmmNode node, String dummy) {
        return visit(node.getChildren().get(0));
    }

    private OllirObject visitClassDeclaration(JmmNode node, String dummy) {
        OllirObject result = new OllirObject("");
        for (JmmNode child : node.getChildren()) result.appendToCode(visit(child));
        return result;
    }

    private OllirObject visitAssign(JmmNode node, String dummy) {
        List<JmmNode> children = node.getChildren();
        JmmNode leftchild = children.get(0);
        JmmNode rightchild = children.get(1);

        OllirObject left = visit(leftchild);
        OllirObject right = visit(rightchild);

        /*if (left.contains("putfield")) {
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
        }*/

        String type = OLLIRUtils.getReturnTypeExpression(left.getCode());
        OllirObject result = new OllirObject(OLLIRUtils.assign(left.getCode(), type, right.getCode()));
        result.appendTemps(left);
        result.appendTemps(right);
        return result;
    }

    //TODO
    private OllirObject visitWhile(JmmNode node, String dummy) {
        OllirObject result = new OllirObject("");

        /*List<JmmNode> children = node.getChildren();
        JmmNode condition = children.get(0);
        JmmNode body = children.get(1);

        result.appendCode("\nLoop" + loop_counter + ":\n");
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

        loop_counter++;*/
        return result;
    }

    //TODO
    private OllirObject visitIfElse(JmmNode node, String dummy) {
        OllirObject result = new OllirObject("");

        List<JmmNode> children = node.getChildren();

        /*
        res.append("\nif (").append(visit(children.get(0))).append(") goto else").append(if_counter).append(";\n");
        res.append(visit(children.get(1)));
        res.append("\ngoto endif").append(if_counter).append(";\n");
        res.append("else").append(if_counter).append(":\n");
        res.append(visit(children.get(3)));
        res.append("\nendif").append(if_counter).append(":\n");
        */
        if_counter++;
        return result;
    }

    //TODO : newintarrayexpression and its temporary variables
    private OllirObject visitFinalTerms(JmmNode node, String dummy) {
        OllirObject result = new OllirObject("");

        List<JmmNode> children = node.getChildren();
        JmmNode child = children.get(0);
        String value;

        if (child.getKind().contains("Number")) {
            value = child.getKind().replace("Number", "").replaceAll("'", "").replaceAll(" ", "");
            result.appendCode(value + ".i32");

        } else if (child.getKind().equals("NewIntArrayExpression")) { //TODO: check if var needs temporary variable
            OllirObject var = visit(child.getChildren().get(0));
            result.appendCode(var.getAboveTemp());
            result.appendCode("new(array," + var.getCode() + ").array.i32");
            result.appendCode(var.getBelowTemp()); //TODO: check this

        } else if (child.getKind().contains("NewIdentifier")) {
            value = child.getKind().replaceAll("'", "").replace("NewIdentifier ", "").trim();
            result.appendCode("new(" + value + ")." + value); // new(myClass).myClass
            OllirObject var = visit(node.getParent().getParent().getChildren().get(0)); //TODO: checkar se preciso de lidar com below e above
            result.addBelowTemp("invokespecial(" + var.getCode() + ", \"<init>\").V;\n");
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
        OllirObject resultLeft, resultRight;

        switch (node.getKind()) {
            case "Add":
            case "Sub":
            case "Mult":
            case "Div":
            case "And":
            case "Less":
                resultLeft = checkExpressionTemporary(node.getChildren().get(0));
                resultRight = checkExpressionTemporary(node.getChildren().get(1));
                System.out.println("left: " + resultLeft + ", " + resultRight);
                return new OllirObject(visit(node.getChildren().get(0)) + " " + OLLIRUtils.getOperationType(node) + " " + visit(node.getChildren().get(1)));

            case "Not":
                resultRight = checkExpressionTemporary(node.getChildren().get(0));
                System.out.println("not -> right: " + resultRight);
                return resultRight;
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
        if (type.length() - type.replaceAll("\\.", "").length() > 1) type = type.substring(type.lastIndexOf("."));

        int ident;

        try {
            int index = a.getCode().indexOf(".");
            ident = Integer.parseInt(aCode.substring(0, index));
            var_temp++;
            aCode = "aux" + var_temp + ".i32";
            result.addAboveTemp(aCode + " :=.i32 " + ident + ".i32;");
        } catch (NumberFormatException ignored) {
        }

        result.appendCode(OLLIRUtils.getIdentifierExpression(i.getCode()) + "[" + aCode + "]" + type);

        return result;
    }

    private OllirObject visitCall(JmmNode node, String dummy) {

        List<JmmNode> children = node.getChildren();
        JmmNode left = children.get(0);
        JmmNode right = children.get(1);

        if (left.getKind().equals("FinalTerms") && right.getKind().equals("MethodCall"))
            return visitMethodCall(node, left, right);
        else if (left.getKind().equals("FinalTerms") && right.getKind().equals("Length")) return visitLength(left);

        return new OllirObject("");
    }

    private OllirObject visitMethodCall(JmmNode call, JmmNode finalTerm, JmmNode method) {
        OllirObject identifier = visit(finalTerm);
        String invokeType = OLLIRUtils.getInvokeType(identifier.getCode(), method.getChildren().get(0), symbolTable);

        switch (invokeType) {
            case "virtual":
                List<String> temporary = new ArrayList<>();
                List<String> args = getMethodArgs(method);

                for (String s : args) {
                    String[] splitted = s.split("\\n");
                    temporary.addAll(Arrays.asList(splitted).subList(1, splitted.length));
                }

                String methodInfo = OLLIRUtils.getMethodInfo(method, args);
                SymbolMethod met = symbolTable.getMethodByInfo(methodInfo);
                OllirObject result = new OllirObject("");

                for (String temp : temporary) result.addAboveTemp(temp + ";");

                String methodCode = OLLIRUtils.invokeStaticVirtual(false, identifier.getCode(), OLLIRUtils.getMethodName(method.getChildren().get(0)), args, met.getReturnType().toOLLIR());
                result.appendCode(methodCode);

                return result;
            case "static":
                List<String> arg = getMethodArgs(method);
                String returnType = getStaticReturnType(call);
                return new OllirObject(OLLIRUtils.invokeStaticVirtual(true, identifier.getCode(), OLLIRUtils.getMethodName(method.getChildren().get(0)), arg, returnType));
            default:
                return new OllirObject("");
        }
    }

    //TODO
    public List<String> getMethodArgs(JmmNode method) {
        List<JmmNode> children = method.getChildren();
        List<String> args = new ArrayList<>();

        for (int i = 1; i < children.size(); i++) {
            /*String result = checkReturnTemporary(children.get(i).getChildren().get(0));
            if (result.equals(""))*/
            args.add(visit(children.get(i)).getCode());
            /*else
                args.add("aux" + var_temp + OLLIRUtils.getReturnTypeExpression(visit(children.get(i).getChildren().get(0))) + "\n" + result);
            */
        }
        return args;
    }

    public String getStaticReturnType(JmmNode method) {
        if (method.getParent().getKind().equals("Assign")) {
            JmmNode brother = method.getParent().getChildren().get(0);
            return OLLIRUtils.getReturnTypeExpression(visit(brother).getCode());
        } else return ".V";
    }

    //TODO: ver quando sao necessarias variaveis temporarias para o array Ex: oi.getArray().length
    private OllirObject visitLength(JmmNode identifier) {
        OllirObject array = visit(identifier);
        //String res = checkReturnTemporary(identifier); //ver se o identifier precisa de variavel
        return new OllirObject("arraylength(" + array.getCode() + ").i32;\n");
    }

    private OllirObject checkExpressionTemporary(JmmNode expression) {
        System.out.println("Expression: " +  expression);
        OllirObject result = new OllirObject("");

        if (OLLIRUtils.hasOperation(expression) || OLLIRUtils.hasCall(expression) || OLLIRUtils.hasField(expression, symbolTable, currentMethod)) {
            OllirObject aux = visit(expression);

            String type;
            if (expression.getKind().equals("Call") || expression.getKind().contains("FinalTerms"))
                type = OLLIRUtils.getReturnTypeExpression(aux.getCode());
            else
                type = OLLIRUtils.getReturnTypeExpression(visit(expression.getChildren().get(0)).getCode());

            var_temp++;
            result.appendCode("aux" + var_temp + type + " :=" + type + " " + aux.getCode());
        }

        System.out.println("result -> " + result);
        return result;
    }

}
