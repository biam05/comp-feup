import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodJasmin {
    private final Method method;
    private final StringBuilder jasminCode;
    private final List<Report> reports;
    private int n_locals;
    private int n_stack;
    private Map<String, Integer> localVariables;

    public MethodJasmin(Method method) {
        this.method = method;
        this.jasminCode = new StringBuilder();
        this.reports = new ArrayList<>();
        this.n_locals = 0;
        this.n_stack = 0;
        this.localVariables = new HashMap<String, Integer>();
    }

    public StringBuilder getJasminCode() {
        return jasminCode;
    }

    public List<Report> getReports() {
        return reports;
    }

    public Method getMethod() {
        return method;
    }

    public void incNLocals(){
        this.n_locals++;
    }

    public void incNStack(){
        this.n_stack++;
    }

    public void decNLocals(){
        this.n_locals--;
    }

    public void decNStack(){
        this.n_stack--;
    }

    public int getN_locals() {
        return n_locals;
    }

    public int getN_stack() {
        return n_stack;
    }

    public Map<String, Integer> getLocalVariables() {
        return localVariables;
    }

    public Boolean addLocalVariable(String variable, int id){
        if(!localVariables.containsKey(variable)){
            localVariables.put(variable, id);
            return true;
        }
        return false;
    }

    public void generateJasminCode(){

        jasminCode.append("\n\n.method public");
        //jasminCode.append(method.getMethodAccessModifier().toString().toLowerCase());

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
        StringBuilder auxiliaryJasmin = new StringBuilder();
        for(var inst : method.getInstructions()){
            InstructionJasmin instructionJasmin = new InstructionJasmin(inst, this);
            instructionJasmin.generateJasminCode();
            auxiliaryJasmin.append(instructionJasmin.getJasminCode());
            this.reports.addAll(instructionJasmin.getReports());
        }
        if(!this.method.isConstructMethod()){
            this.jasminCode.append("\n\t\t.limit locals ").append(n_locals);
            this.jasminCode.append("\n\t\t.limit stack ").append(n_stack).append("\n");
        }
        this.jasminCode.append(auxiliaryJasmin);
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
            addLocalVariable(((Operand)param).getName(), n_locals);
            incNLocals();
        }
    }
}
