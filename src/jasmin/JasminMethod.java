package jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JasminMethod {
    private final Method method;
    private final StringBuilder jasminCode;
    private final List<Report> reports;
    private final String className;
    private int n_locals;
    private int n_stack;
    private int n_branches;
    private Map<String, Descriptor> localVariables;

    public JasminMethod(Method method, String className) {
        this.method = method;
        this.jasminCode = new StringBuilder();
        this.reports = new ArrayList<>();
        this.n_locals = 0; // change
        this.n_stack = 99; // change
        this.n_branches = 0;
        this.localVariables = new HashMap<>();
        this.className = className;
        addLocalVariable("this", VarScope.FIELD, new Type(ElementType.CLASS));
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

    public Map<String, Descriptor> getLocalVariables() {
        return localVariables;
    }

    public Descriptor getLocalVariableByKey(String key, VarScope type, Type tp) {
        if(localVariables.get(key) == null){
            addLocalVariable(key, type, tp);
        }
        return localVariables.get(key);
    }

    public Boolean addLocalVariable(String variable, VarScope type, Type tp){
        if(!localVariables.containsKey(variable)){
            localVariables.put(variable, new Descriptor(type, n_locals, tp));
            n_locals++;
            return true;
        }
        return false;
    }

    public void getMethodDeclaration(){
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

        JasminUtils.getParametersFromMethod(this);

        jasminCode.append(")");
    }

    public void generateJasminCode(){

        getMethodDeclaration();

        jasminCode.append(JasminUtils.getReturnFromMethod(method));

        StringBuilder auxiliaryJasmin = new StringBuilder();
        String currentlabel = "";
        for(var inst : method.getInstructions()) {
            if (!method.getLabels(inst).isEmpty())
                if (!currentlabel.equals(method.getLabels(inst).get(0))) {
                    currentlabel = method.getLabels(inst).get(0);
                    for (String label: method.getLabels(inst)) {
                        auxiliaryJasmin.append("\n\t").append(label).append(":");
                    }
                }
            JasminInstruction jasminInstruction = new JasminInstruction(inst, this);
            jasminInstruction.generateJasminCode();
            auxiliaryJasmin.append(jasminInstruction.getJasminCode());
            this.reports.addAll(jasminInstruction.getReports());
        }
        if(!this.method.isConstructMethod()){
            this.jasminCode.append("\n\t\t.limit locals ").append(n_locals);
            this.jasminCode.append("\n\t\t.limit stack ").append(n_stack).append("\n");
        }
        this.jasminCode.append(auxiliaryJasmin);
        jasminCode.append("\n.end method");
    }
}
