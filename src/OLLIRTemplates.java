import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;

import java.util.ArrayList;
import java.util.Arrays;
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
        res.append("}\n");
        return res.toString();
    }

    //invokespecial(A.myClass,"<init>").V -> chamaria: invokeSpecial("A.myClass")
    public static String invokeSpecial(String identifierClass) {
        return "invokespecial(" + identifierClass + ", \"<init>\").V;\n";
    }

    //invokestatic(io, "println", t2.String, t1.i32).V; //io.println("val = ", this.get());  -> invokeStaticVirtual(true, "io", "println", ["t2.String", "t1.i32"], ".V")
    //invokevirtual(c1.myClass, "put", 2.i32).V; // c1.put(2); ->  chamaria: invokeStaticVirtual(false, "c1.myClass", "put", ["2.i32"], ".V")
    public static String invokeStaticVirtual(boolean isStatic, String identifier, String methodName, List<String> fields, String returnType){
        StringBuilder result = new StringBuilder();
        if(isStatic) result.append("invokestatic");
        else result.append("invokevirtual");

        result.append( "(" + identifier + ", \"" + methodName + "\"");
        for(String field: fields)
            result.append(", " + field);
        result.append( ")" + returnType);

        return result.toString();
    }

    public static String returnVoid() {
        return "ret.V;\n";
    }

    public static String returnTemplate(String identifier, String type) {
        return "ret" + type + " " + identifier + ";\n";
    }

    public static String assign(String left, String type, String right) {
        return (left + " :=" + type + " " + right + ";\n");
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
            case "Add":
                return "+.i32";
            case "Sub":
                return "-.i32";
            case "Mul":
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
            case "Mul":
            case "Div":
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
            if(node.getParent().getKind().equals("Assign") && node.getParent().getChildren().get(0).equals(node)){
                return putField(ret, node, node.getParent().getChildren().get(0), symbolTable, method);
            }
            return getField(ret, node, symbolTable, method);
        }
        else if(method.returnTypeIfExists(ret) != null){
            return "$" + method.getParameterOrder(ret) + "." + ret;
        }
        return ret;
    }

    public static String getIdentifierType(String value, GrammarSymbolTable symbolTable, SymbolMethod method){
        String ret = SemanticAnalysisUtils.checkIfIdentifierExists(symbolTable, method, value).toOLLIR();
        return ret;
    }

    //getfield(obj, variable).valor ex: this.a --> getfield(this, a.i32).i32
    public static String getField(String obj, JmmNode node, GrammarSymbolTable symbolTable, SymbolMethod method){
        String var = OLLIRTemplates.getIdentifier(node, symbolTable, method);
        String type = getIdentifierType(var, symbolTable, method);
        return "getfield(" + obj + "," + var + ")" + type;
    }

    // putfield(obj, variable, value).V
    public static String putField(String obj, JmmNode assignee, JmmNode assigned, GrammarSymbolTable symbolTable, SymbolMethod method){
        String value_1 = getIdentifier(assignee, symbolTable, method);
        String a_1 = value_1 + getIdentifierType(value_1,symbolTable, method);
        String value_2 = getIdentifier(assigned, symbolTable, method);
        String a_2 = value_2 + getIdentifierType(assigned.getKind(), symbolTable, method);
        return "putfield(" + obj + "," + a_1 + "," + a_2 + ").V;";
    }

    public static String getReturnTypeExpression(String expression) { //for example, parse a.i32: return .i32
        if(expression.equals("")) return "";
        String[] values = expression.split(".");
        if(values.length < 2) return "";
        if(values.length == 2) return "." + values[1].trim();
        return "." + values[1].trim() + "." + values[2].trim();
    }

    public static String getIdentifierExpression(String expression) { //for example, parse a.i32: return a
        String[] values = expression.split(".");
        return values[0].trim();
    }

    public static int countNumberOperations(String expression) {
        int counter = 0;
        List<String> values = Arrays.asList(expression.split(" "));

        for(String value: values) {
            if(value.contains("+") || value.contains("-") || value.contains("*") || value.contains("/") || value.contains("<") || value.contains("!") || value.contains("&&"))
                counter++;
        }

        return counter;
    }

    public static int countCalls(String expression) {
        int counter = 0;
        List<String> values = Arrays.asList(expression.split(" "));

        for(String value: values) {
            if(value.contains("length") || value.contains("(")) //não sei se é preciso contar os invokes; não sei se nesta parte já existe essa possibilidade
                counter++;
        }

        return counter;
    }

    public static String checkReturnTemporary(String expression, int var_temp) {
        int n_operations = countNumberOperations(expression);
        int n_calls = countCalls(expression);

        if(n_operations == 0 && n_calls == 0) return expression;

        StringBuilder result = new StringBuilder();

        // return this.a(1+2)
        // t1 = 1+2
        // t2 = this.a(t1)
        // return t2
        return result.toString();
    }



}
