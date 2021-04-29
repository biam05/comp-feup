import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;

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
        //falta putfiedls(??)

        return res.toString();
    }

    public static String invokeSpecial(String identifierClass) {
        return "invokespecial(" + identifierClass + ", \"<init>\").V;\n";
    }

    public static String returnVoid() {
        return "ret.V;\n";
    }

    public static String returnTemplate(String identifier, String type) {
        return "ret" + type + " " + identifier + ";\n";
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


        return String.join(",", param);
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

    public static String getIdentifier(String kind, GrammarSymbolTable symbolTable, SymbolMethod method){
        String value = kind.replaceAll("'", "").replace("Identifier ", "");
        String ret = SemanticAnalysisUtils.checkIfIdentifierExists(symbolTable, method, value).toOLLIR();
        return value + ret;
    }


}
