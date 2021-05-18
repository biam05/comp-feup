package jasmin;

import org.specs.comp.ollir.*;

public class JasminUtils {

    public static String getParametersFromMethod(JasminMethod method){
        StringBuilder res = new StringBuilder();
        if(method.getMethod().getMethodName().equals("main")){
            res = new StringBuilder("[Ljava/lang/String;");
            method.addLocalVariable("args", VarScope.PARAMETER, new Type(ElementType.ARRAYREF));
        }
        else{
            for(Element param : method.getMethod().getParams()){
                if(param.isLiteral()) res = new StringBuilder("L");
                switch (param.getType().getTypeOfElement()){
                    case INT32:
                        res.append("I");
                        break;
                    case BOOLEAN:
                        res.append("Z");
                        break;
                    case ARRAYREF:
                        res.append("[I");
                        break;
                    case OBJECTREF:
                        res.append("OBJECTREF");
                        break;
                    case STRING:
                        res.append("java/lang/String");
                        break;
                    default:
                        break;
                }
                method.addLocalVariable(((Operand)param).getName(), VarScope.PARAMETER, param.getType());
            }
        }
        return res.toString();
    }

    public static String getReturnFromMethod(Method method, Type type){
        String res = "";
        switch(type.getTypeOfElement()){
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

    public static String getConstSize(String value){
        int val = Integer.parseInt(value);
        String res, aux;
        if (val >= 0 && val <= 5) aux = "iconst_";
        else if (val > 5 && val <= 128) aux = "bipush ";
        else if (val > 128 && val <= 32768) aux = "sipush ";
        else aux = "ldc ";
        res = "\n\t\t" + aux + val;
        return res;
    }

    public static String getLoadSize(int num){
        String res, aux;
        if (num >= 0 && num <= 3) aux = "load_";
        else aux = "load ";
        res = aux + num;
        return res;
    }

    public static String getStoreSize(int num){
        String res, aux;
        if (num >= 0 && num <= 3) aux = "store_";
        else aux = "store ";
        res = aux + num;
        return res;
    }
}
