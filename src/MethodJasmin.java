import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class MethodJasmin {
    private final Method method;
    private final StringBuilder jasminCode;
    private final List<Report> reports;

    public MethodJasmin(Method method) {
        this.method = method;
        this.jasminCode = new StringBuilder();
        this.reports = new ArrayList<>();
    }

    public StringBuilder getJasminCode() {
        return jasminCode;
    }

    public List<Report> getReports() {
        return reports;
    }

    public void generateJasminCode(){
        System.out.println("------------------------------------------");
        System.out.println("Method Name: " + method.getMethodName());

        jasminCode.append("\n\n.method ");
        jasminCode.append(method.getMethodAccessModifier().toString().toLowerCase());

        if(method.isConstructMethod())
            jasminCode.append(" <init>");
        else{
            if(method.isStaticMethod()) jasminCode.append(" static");
            if(method.isFinalMethod())  jasminCode.append(" final");

            jasminCode.append(" ");
            jasminCode.append(method.getMethodName());
        }
        jasminCode.append("(");

        analyseParameters();

        jasminCode.append(")");

        String methodReturn = "";
        switch(method.getReturnType().getTypeOfElement()){
            // todo: verificar com o ollir como é o tipo quando retorna uma classe
            case INT32:
                methodReturn = "I";
                break;
            case BOOLEAN:
                methodReturn = "Z";
                break;
            case ARRAYREF:
                methodReturn = "[I";
                break;
            case OBJECTREF:
                methodReturn = method.getClass().getName(); //Todo: probably wrong
                break;
            case VOID:
                methodReturn = "V";
                break;
            default:
                break;
        }
        jasminCode.append(methodReturn);

        for(var inst : method.getInstructions()){
            InstructionJasmin instructionJasmin = new InstructionJasmin(inst);
            instructionJasmin.generateJasminCode();
            this.jasminCode.append(instructionJasmin.getJasminCode());
            this.reports.addAll(instructionJasmin.getReports());
        }

        jasminCode.append("\n.end method");
    }

    private void analyseParameters(){
        for(Element param : method.getParams()){
            if(param.isLiteral()) jasminCode.append("L");
            switch (param.getType().getTypeOfElement()){
                case INT32:
                    jasminCode.append("I");
                    break;
                case BOOLEAN:
                    jasminCode.append("Z");
                    break;
                case ARRAYREF:
                    jasminCode.append("[I");
                    break;
                case OBJECTREF:
                    jasminCode.append("OBJECTREF"); // Todo: Check with OLLIR
                    break;
                case STRING:
                    jasminCode.append("java/lang/String");
                    break;
                default:
                    break;
            }
        }
    }
}
