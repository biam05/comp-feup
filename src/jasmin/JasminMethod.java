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
    private final String superName;
    private final Map<String, Descriptor> localVariables;
    private int n_locals;
    private int max_n_stack;
    private int current_n_stack;
    private int n_branches;

    public JasminMethod(Method method, String className, String superName) {
        this.method = method;
        this.jasminCode = new StringBuilder();
        this.reports = new ArrayList<>();
        this.n_locals = 0;
        this.max_n_stack = 0;
        this.current_n_stack = 0;
        this.n_branches = 0;
        this.localVariables = new HashMap<>();
        this.className = className;
        this.superName = superName;
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

    public void incN_branches() {
        n_branches++;
    }

    public void incN_stack() {
        current_n_stack++;
        if (current_n_stack > max_n_stack)
            max_n_stack = current_n_stack;
    }

    public void decN_stack() {
        current_n_stack--;
    }

    public String getClassName() {
        return className;
    }

    public Descriptor getLocalVariableByKey(Element dest, VarScope type) {
        String key = ((Operand) dest).getName();
        if (localVariables.get(key) == null) {
            addLocalVariable(key, type, dest.getType());
        }
        return localVariables.get(key);
    }

    public void addLocalVariable(String variable, VarScope type, Type tp) {
        if (!localVariables.containsKey(variable)) {
            localVariables.put(variable, new Descriptor(type, n_locals, tp));
            n_locals++;
        }
    }

    public void getMethodDeclaration() {
        jasminCode.append("\n\n.method public");

        if (method.isConstructMethod())
            jasminCode.append(" <init>");
        else {
            if (method.isStaticMethod()) jasminCode.append(" static");
            if (method.isFinalMethod()) jasminCode.append(" final");

            jasminCode.append(" ");
            jasminCode.append(method.getMethodName());
        }
        jasminCode.append("(");

        jasminCode.append(JasminUtils.getParametersFromMethod(this));

        jasminCode.append(")");
    }

    public void generateJasminCode() {

        getMethodDeclaration();

        jasminCode.append(JasminUtils.getReturnFromMethod(this, method.getReturnType()));

        StringBuilder auxiliaryJasmin = new StringBuilder();
        String currentlabel = "";
        for (var inst : method.getInstructions()) {
            if (!method.getLabels(inst).isEmpty())
                if (!currentlabel.equals(method.getLabels(inst).get(0))) {
                    currentlabel = method.getLabels(inst).get(0);
                    for (String label : method.getLabels(inst)) {
                        auxiliaryJasmin.append("\n\t").append(label).append(":");
                    }
                }
            JasminInstruction jasminInstruction = new JasminInstruction(inst, this);
            jasminInstruction.generateJasminCode();
            auxiliaryJasmin.append(jasminInstruction.getJasminCode());
            this.reports.addAll(jasminInstruction.getReports());
        }
        if (!this.method.isConstructMethod()) {
            this.jasminCode.append("\n\t\t.limit locals ").append(n_locals);
            this.jasminCode.append("\n\t\t.limit stack ").append(max_n_stack).append("\n");
        }
        this.jasminCode.append(auxiliaryJasmin);
        jasminCode.append("\n.end method");
    }

    public String getSuperName() {
        return superName;
    }
}
