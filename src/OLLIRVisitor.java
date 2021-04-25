import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class OLLIRVisitor extends AJmmVisitor {
    private GrammarSymbolTable symbolTable;
    private List<Report> reports;

    public OLLIRVisitor(GrammarSymbolTable symbolTable){
        this.symbolTable = symbolTable;
        this.reports = new ArrayList<>();
    }

    public List<Report> getReports() {
        return reports;
    }

    @Override
    public String visit(JmmNode jmmNode) {



        return "";
    }
}
