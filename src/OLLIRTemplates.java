import pt.up.fe.comp.jmm.analysis.table.Type;

public class OLLIRTemplates {
    public static String init(String className){

        return className +
                " {\n.construct " +
                className + ".V" +
                "{\n" + "invokespecial(this, \"<init>\").V" + ";\n" + "}\n";
    }

    public static String methodDeclaration(String methodName, String returnType){

        return ".method public " + methodName + "()" + returnType;
    }

    public static String field(String fieldName, String fieldType){
        return ".field " + fieldName + fieldType + ";";
    }

    public static String type(Type type){
        StringBuilder res = new StringBuilder();

        if(type.isArray()) res.append(".array");

        switch (type.getName()) {
            case "Int" -> res.append(".i32");
            case "Boolean" -> res.append(".bool");
            case "Void" -> res.append(".V");
            default -> res.append(".").append(type.getName());
        }

        return res.toString();
    }
}
