import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OLLIRUtils {
    public static String fields(List<Symbol> fields) {
        if (fields.isEmpty()) return "";
        StringBuilder res = new StringBuilder();

        for (Symbol field : fields) {
            res.append(".field ").append(field.toOLLIR()).append(";\n");
        }

        return res.toString();
    }

    public static String init(String className, List<Symbol> fields) {
        return className + " {\n" + fields(fields) + "\n.construct " + className + "().V {\n" +
                invokeSpecial("this") +
                "}\n\n";
    }


    public static String invokeSpecial(String identifierClass) {
        return "invokespecial(" + identifierClass + ", \"<init>\").V;\n";
    }

    public static String invokeStaticVirtual(boolean isStatic, String identifier, String methodName, List<String> fields, String returnType) {

        StringBuilder result = new StringBuilder();
        List<String> temporary = new ArrayList<>();

        if (isStatic) result.append("invokestatic");
        else result.append("invokevirtual");

        result.append("(").append(identifier).append(", \"").append(methodName).append("\"");
        for (String field : fields) {
            String[] splitted = field.split("\n");
            result.append(", ").append(splitted[0]);
            temporary.addAll(Arrays.asList(splitted).subList(1, splitted.length));
        }
        result.append(")").append(returnType);

        for (String temp : temporary) {
            result.append("\n").append(temp);
        }
        return result.toString();
    }

    public static String returnVoid() {
        return "ret.V;\n";
    }

    public static String assign(String left, String type, String right) {
        return left + " :=" + type + " " + right + ";\n";
    }

    public static String methodDeclaration(SymbolMethod method) {
        StringBuilder res = new StringBuilder();
        res.append(".method public ");
        if (method.getName().equals("main")) res.append("static ");
        res.append(method.getName()).append("(").append(parameters(method.getParameters())).append(")").append(method.getReturnType().toOLLIR()).append(" ");
        return res.toString();
    }

    public static String parameters(List<Symbol> parameters) {
        if (parameters.isEmpty()) return "";

        List<String> param = new ArrayList<>();
        for (Symbol parameter : parameters) param.add(parameter.toOLLIR());

        return String.join(", ", param);
    }

    public static String getOperationType(JmmNode node) {
        switch (node.getKind()) {
            case "And":
                return "&&.bool";
            case "Not":
                return "!.bool";
            case "Less":
                return "<.i32";
            case "Add":
                return "+.i32";
            case "Sub":
                return "-.i32";
            case "Mult":
                return "*.i32";
            case "Div":
                return "/.i32";
            default:
                return "";
        }
    }

    public static String getIdentifier(JmmNode node, GrammarSymbolTable symbolTable, SymbolMethod method) {
        String ret = node.getKind().replaceAll("'", "").replace("Identifier ", "").trim();
        if (symbolTable.returnFieldTypeIfExists(ret) != null) { //its a class field
            JmmNode parent = node.getParent().getParent().getParent();
            if (parent.getKind().equals("Assign") && parent.getChildren().get(0).getChildren().get(0).equals(node.getParent()))
                return "putfield = " + ret + getIdentifierType(node, symbolTable, method);

            return getField(node, symbolTable, method);
        } else if (method.returnTypeIfExists(ret) != null) {
            int param = method.getParameterOrder(ret);

            if (param > 0) {
                ret = "$" + param + "." + ret;
                return ret;
            }
            return ret;
        }
        return ret;
    }

    public static String getIdentifierType(JmmNode node, GrammarSymbolTable symbolTable, SymbolMethod method) {
        String ret = node.getKind().replaceAll("'", "").replace("Identifier ", "").trim();
        Type type = SemanticAnalysisUtils.checkIfIdentifierExists(symbolTable, method, ret);
        if (type == null) return "";
        return type.toOLLIR();
    }


    public static String getField(JmmNode node, GrammarSymbolTable symbolTable, SymbolMethod method) {
        String var = node.getKind().replaceAll("'", "").replace("Identifier ", "").trim();
        String type = getIdentifierType(node, symbolTable, method);
        return "getfield(this" + ", " + var + type + ")" + type;
    }


    public static String putField(String assigned, String assignee) {
        return "putfield(this" + ", " + assigned + ", " + assignee + ").V;\n";
    }

    public static String getReturnTypeExpression(String expression) { //for example, parse a.i32: return .i32
        String res = "";
        if (expression.equals("")) return "";
        int index = expression.lastIndexOf('.');
        if (expression.length() > 6 && index > 5f)
            if (expression.startsWith("array", index - 5)) res += ".array";

        return res + "." + expression.substring(index + 1).trim().replaceAll("[() ;+<*/&\\-]", "");
    }

    public static String getIdentifierExpression(String expression) { //for example, parse a.i32: return a
        String[] values = expression.split("\\.");

        if (values.length == 2) return values[0].trim();
        if (values.length > 2) {
            String no_last = values[values.length - 2];
            int index = 1;
            if (no_last.equals("array")) index = 2;
            List<String> ret = new ArrayList<>(Arrays.asList(values).subList(0, values.length - index));
            return String.join(".", ret);
        }

        return expression;
    }

    public static boolean hasOperation(JmmNode expression) {
        return expression.getKind().equals("Add") ||
                expression.getKind().equals("Sub") ||
                expression.getKind().equals("Mult") ||
                expression.getKind().equals("Div") ||
                expression.getKind().equals("Less") ||
                expression.getKind().equals("Not") ||
                expression.getKind().equals("And");
    }

    public static boolean hasCall(JmmNode expression) {
        if (expression.getChildren().size() < 2) return false;
        return expression.getChildren().get(1).getKind().equals("Length") ||
                expression.getChildren().get(1).getKind().equals("MethodCall");
    }

    public static boolean hasField(JmmNode expression, GrammarSymbolTable symbolTable, SymbolMethod currentMethod) {
        if (expression.getKind().equals("FinalTerms")) {
            JmmNode child = expression.getChildren().get(0);
            if (child.getKind().contains("Identifier")) {
                String value = OLLIRUtils.getIdentifier(child, symbolTable, currentMethod);
                return value.contains("putfield") || value.contains("getfield");
            }
        }
        return false;
    }


    public static String getInvokeType(String identifier, JmmNode method, GrammarSymbolTable symbolTable) {
        String methodName = getMethodName(method);
        if (identifier.contains("this") || symbolTable.hasMethod(methodName))
            return "virtual"; // if it belongs to the class
        return "static";
    }

    public static String getMethodName(JmmNode method) {
        return method.getKind().replaceAll("'", "").replace("Identifier ", "").trim();
    }

    public static String getMethodInfo(JmmNode method, List<String> p) {
        StringBuilder res = new StringBuilder();

        String methodName = OLLIRUtils.getMethodName(method.getChildren().get(0));
        if (p.size() == 0) return methodName + "()";

        List<String> param = new ArrayList<>();

        for (String s : p) {
            param.add(OLLIRUtils.getReturnTypeExpression(s));
        }

        res.append(methodName).append("(").append(ollirListToJavaType(param)).append(")");
        return res.toString();
    }

    public static String ollirListToJavaType(List<String> ollir) {
        List<String> types = new ArrayList<>();
        for (String aux : ollir) {
            switch (aux) {
                case ".i32":
                    types.add("Int");
                    break;
                case ".array.i32":
                    types.add("Int[]");
                    break;
                case ".array.String":
                    types.add("String[]");
                    break;
                case ".bool":
                    types.add("Boolean");
                    break;
                case ".V":
                    types.add("Void");
                default:
                    types.add(aux.replaceAll("\\.", ""));
                    break;
            }
        }
        return String.join(",", types);
    }
}
