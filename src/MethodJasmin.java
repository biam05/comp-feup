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
    private final String className;
    private int n_locals;
    private int n_stack;
    private int n_branches;
    private Map<String, Integer> localVariables;

    public MethodJasmin(Method method, String className) {
        this.method = method;
        this.jasminCode = new StringBuilder();
        this.reports = new ArrayList<>();
        this.n_locals = 0; // change
        this.n_stack = 99; //change
        this.n_branches = 0;
        this.localVariables = new HashMap<>();
        this.className = className;
        addLocalVariable("this", n_locals);
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

    public int getN_branches() {
        return n_branches;
    }

    public int getN_locals() {
        return n_locals;
    }

    public void incN_branches() {
        n_branches++;
    }

    public String getClassName() {
        return className;
    }

    public Map<String, Integer> getLocalVariables() {
        return localVariables;
    }

    public Integer getLocalVariableByKey(String key) {
        if(localVariables.get(key) == null){
            addLocalVariable(key, n_locals);
        }
        return localVariables.get(key);
    }

    public Boolean addLocalVariable(String variable, int id){
        if(!localVariables.containsKey(variable)){
            localVariables.put(variable, id);
            n_locals++;
            return true;
        }

        return false;
    }

    public void generateJasminCode(){

        jasminCode.append("\n\n.method public");

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
        String currentlabel = "";
        for(var inst : method.getInstructions()) {
            if (!method.getLabels(inst).isEmpty())
                if (!currentlabel.equals(method.getLabels(inst).get(0))) {
                    currentlabel = method.getLabels(inst).get(0);
                    auxiliaryJasmin.append("\n\t").append(currentlabel).append(":");
                }
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
        if(method.getMethodName().equals("main")){
            jasminCode.append("[Ljava/lang/String;");
            addLocalVariable("args", n_locals);
        }
        else{
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
            }
        }
    }
}
