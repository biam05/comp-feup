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

}
