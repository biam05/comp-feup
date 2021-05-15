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

    public static String getInstructionConstSize(String value){
        int val = Integer.parseInt(value);
        String res, aux;
        if (val >= 0 && val <= 5) aux = "iconst_";
        else if (val > 5 && val <= 128) aux = "bipush ";
        else if (val > 128 && val <= 32768) aux = "sipush ";
        else aux = "ldc ";
        res = "\n\t\t" + aux + val;
        return res;
    }

    public static String constOrLoad(JasminMethod method, Element element) {
        String value;
        if (element.isLiteral()){
            value = ((LiteralElement) element).getLiteral();
            return JasminUtils.getInstructionConstSize(value);
            //jasminCode.append(JasminUtils.getInstructionConstSize(value));
        }
        else {
            //String type = decideType(element);
            String type = null;
            if (type == null) {
                return "load " + method.getLocalVariableByKey(((Operand) element).getName(), VarScope.LOCAL, element.getType()).getVirtualReg();
            }
            else {
                return type + "aload";
            }
        }
    }

    public static String decideType(JasminMethod method, Element element) {
        switch (element.getType().getTypeOfElement()) {
            case INT32:
            case BOOLEAN:
                if (!element.isLiteral())
                    if (method.getLocalVariableByKey(((Operand) element).getName(), null, element.getType()).getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                        /*jasminCode.append("\n\t\taload ").append(method.getLocalVariableByKey(((Operand) element).getName(), null, element.getType()).getVirtualReg());
                        Operand indexOp = (Operand) ((ArrayOperand) element).getIndexOperands().get(0);
                        jasminCode.append("\n\t\tiload ").append(method.getLocalVariableByKey(indexOp.getName(), null, indexOp.getType()).getVirtualReg());
                        */return "\n\t\ti";
                    }
                //jasminCode.append("\n\t\ti");
                break;
            case ARRAYREF:
            case THIS:
            case OBJECTREF:
                //jasminCode.append("\n\t\ta");
                break;
            default:
                //jasminCode.append("\n\t\t");
                break;
        }
        return null;
    }
}
