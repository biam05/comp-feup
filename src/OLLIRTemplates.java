import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;

public class OLLIRTemplates {
    public static String fields(List<Symbol> fields) {
        if (fields.isEmpty()) return "";
        StringBuilder res = new StringBuilder();

        for (Symbol field : fields) {
            res.append(".field private ").append(field.toOLLIR()).append(";\n");
        }

        return res.toString();
    }

    public static String init(String className, List<Symbol> fields) {
        StringBuilder res = new StringBuilder(className);
        res.append(" {\n").append(fields(fields)).append("\n.construct ").append(className).append("().V {\n");
        res.append(invokeSpecial("this"));
        res.append("}\n\n");
        return res.toString();
    }


    public static String invokeSpecial(String identifierClass) {
        return "invokespecial(" + identifierClass + ", \"<init>\").V;\n"; //invokespecial(A.myClass,"<init>").V
    }

    //invokestatic(io, "println", t2.String, t1.i32).V; //io.println("val = ", this.get());  -> invokeStaticVirtual(true, "io", "println", ["t2.String", "t1.i32"], ".V")
    //invokevirtual(c1.myClass, "put", 2.i32).V; // c1.put(2); ->  chamaria: invokeStaticVirtual(false, "c1.myClass", "put", ["2.i32"], ".V")
    public static String invokeStaticVirtual(boolean isStatic, String identifier, String methodName, List<String> fields, String returnType){
        StringBuilder result = new StringBuilder();
        List<String> temporary = new ArrayList<>();

        if(isStatic) result.append("invokestatic");
        else result.append("invokevirtual");

        result.append( "(" + identifier + ", \"" + methodName + "\"");
        for(String field: fields){
            String[] splitted = field.split("\n");
            result.append(", " + splitted[0]);
            for(int i = 1; i < splitted.length; i++){
                temporary.add(splitted[i]);
            }
        }
        result.append( ")" + returnType);
        for(String temp : temporary){
            result.append("\n" + temp);
        }
        return result.toString();
    }

    public static String returnVoid() {
        return "ret.V;\n";
    }

    public static String returnTemplate(String identifier, String type) {
        StringBuilder result = new StringBuilder();
        String[] reversed = (identifier).split("\\n");
        for (int i = reversed.length - 1; i >= 1; i--)
            result.append(reversed[i]).append(";\n");
        result.append("ret").append(type).append(" ").append(reversed[0]).append(type);
        return result + ";\n";
    }

    public static String assign(String left, String type, String right) {
        StringBuilder result = new StringBuilder();
        String[] reversed = (left + " :=" + type + " " + right).split("\\n");
        for (int i = reversed.length - 1; i >= 1; i--)
            result.append(reversed[i]).append(";\n");
        result.append(reversed[0]);
        return result.toString();
    }

    public static String methodDeclaration(SymbolMethod method) {
        StringBuilder res = new StringBuilder();
        res.append(".method public ").append(method.getName()).append("(").append(parameters(method.getParameters())).append(")").append(method.getReturnType().toOLLIR());
        return res.toString();
    }

    public static String parameters(List<Symbol> parameters) {
        if (parameters.isEmpty()) return "";

        List<String> param = new ArrayList<>();
        for (Symbol parameter : parameters) param.add(parameter.toOLLIR());


        return String.join(", ", param);
    }

    public static String getOperationType(JmmNode node){
        switch(node.getKind()){
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

    public static String getOperationReturn(JmmNode node){
        switch(node.getKind()){
            case "And":
            case "Not":
                return ".bool";
            case "Add":
            case "Sub":
            case "Mult":
            case "Div":
            case "Less":
                return ".i32";
            default:
                return "";
        }
    }

    public static boolean isOperator(JmmNode node){
        return node.getKind().equals("Add") || node.getKind().equals("Sub") || node.getKind().equals("Mul") || node.getKind().equals("Div") || node.getKind().equals("And") || node.getKind().equals("Not") || node.getKind().equals("Less");
    }

    public static String getIdentifier(JmmNode node, GrammarSymbolTable symbolTable, SymbolMethod method){
        String ret = node.getKind().replaceAll("'", "").replace("Identifier ", "").trim();
        if(symbolTable.returnFieldTypeIfExists(ret) != null){ //é field da classe, get field e put field
            if(node.getParent().getKind().equals("Assign") && node.getParent().getChildren().get(0).getChildren().get(0).equals(node.getParent())){
                return "putfield = " + ret + getIdentifierType(node, symbolTable, method);
            }
            return getField(ret, node, symbolTable, method);
        }
        return ret;
    }

    public static String getIdentifierType(JmmNode node, GrammarSymbolTable symbolTable, SymbolMethod method){
        String ret = node.getKind().replaceAll("'", "").replace("Identifier ", "").trim();
        Type type = SemanticAnalysisUtils.checkIfIdentifierExists(symbolTable, method, ret);
        if(type == null) return "";
        return type.toOLLIR();
    }


    public static String getField(String obj, JmmNode node, GrammarSymbolTable symbolTable, SymbolMethod method){
        String var = node.getKind().replaceAll("'", "").replace("Identifier ", "").trim();
        String type = getIdentifierType(node, symbolTable, method);
        return "getfield(this" + "," + var + ")" + type; //getfield(obj, variable).returnType
    }


    public static String putField(String assigned, String assignee){
        return "putfield(this" +  "," + assigned + "," + assignee + ").V;"; // putfield(obj, variable, value).V
    }

    public static String getReturnTypeExpression(String expression) { //for example, parse a.i32: return .i32
        if(expression.equals("")) return "";
        String[] values = expression.split("\\.");
        if(values.length < 2) return "";
        if(values.length == 2) return "." + values[1].trim();
        return "." + values[1].split(" ")[0];
    }

    public static String getIdentifierExpression(String expression) { //for example, parse a.i32: return a
        String[] values = expression.split("\\.");
        return values[0].trim();
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


    public static String getInvokeType(String identifier, JmmNode method, GrammarSymbolTable symbolTable){
        String methodName = getMethodName(method);
        if(identifier.contains("this") || symbolTable.hasMethod(methodName)) return "virtual"; // if it belongs to the class
        return "static";
    }

    public static String getMethodName(JmmNode method){
        return method.getKind().replaceAll("'", "").replace("Identifier ", "").trim();
    }

    public static String getMethodInfo(JmmNode method, List<String> p){
        List<JmmNode> children = method.getChildren();
        String methodName = OLLIRTemplates.getMethodName(children.get(0));
        if(children.size() == 1) return methodName + "()";
        List<String> param = new ArrayList<>();
        for(String s: p){
            param.add(OLLIRTemplates.getReturnTypeExpression(s));
        }

        String res =  methodName + "(" + ollirListToJavaType(param) + ")";
        return res;
    }

    public static String ollirListToJavaType(List<String> ollir){
        List<String> types = new ArrayList<>();
        for(String aux: ollir){
            switch(aux){
                case ".i32":
                    types.add("Int");
                    break;
                case ".array":
                    types.add("Int[]");
                    break;
                case ".bool":
                    types.add("Boolean");
                    break;
                case ".V":
                    types.add("Void");
                default:
                    types.add(aux.replaceAll(".", ""));
                    break;
            }
        }
        return String.join(",", types);
    }

}
