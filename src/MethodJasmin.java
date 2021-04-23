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
        System.out.println("Method Name: " + method.getMethodName());
        for(var inst : method.getInstructions()){
            InstructionJasmin instructionJasmin = new InstructionJasmin(inst);
            instructionJasmin.generateJasminCode();
            this.jasminCode.append(instructionJasmin.getJasminCode());
            this.reports.addAll(instructionJasmin.getReports());
        }
    }
}
