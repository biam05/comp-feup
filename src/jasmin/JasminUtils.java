package jasmin;

import org.specs.comp.ollir.*;

public class JasminUtils {

    public static String getParametersFromMethod(JasminMethod method){
        String res = "";
        if(method.getMethod().getMethodName().equals("main")){
            res = "[Ljava/lang/String;";
            method.addLocalVariable("args", VarScope.PARAMETER, new Type(ElementType.ARRAYREF));
        }
        else{
            for(Element param : method.getMethod().getParams()){
                if(param.isLiteral()) res = "L";
                switch (param.getType().getTypeOfElement()){
                    case INT32:
                        res += "I";
                        break;
                    case BOOLEAN:
                        res += "Z";
                        break;
                    case ARRAYREF:
                        res += "[I";
                        break;
                    case OBJECTREF:
                        res += "OBJECTREF";
                        break;
                    case STRING:
                        res += "java/lang/String";
                        break;
                    default:
                        break;
                }
                method.addLocalVariable(((Operand)param).getName(), VarScope.PARAMETER, param.getType());
            }
        }
        return res;
    }

    public static String getReturnFromMethod(Method method){
        String res = "";
        switch(method.getReturnType().getTypeOfElement()){
            case INT32:
                res = "I";
                break;
            case BOOLEAN:
                res = "Z";
                break;
            case ARRAYREF:
                res = "[I";
                break;
            case OBJECTREF:
                res = method.getClass().getName();
                break;
            case VOID:
                res = "V";
                break;
            default:
                break;
        }
        return res;
    }
}
