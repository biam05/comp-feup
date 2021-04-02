import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class SymbolTableVisitor extends PreorderJmmVisitor<Boolean, Boolean> {
    private GrammarSymbolTable symbolTable;
    private List<Report> reports;

    public SymbolTableVisitor() {
        this.symbolTable = new GrammarSymbolTable();
        this.reports = new ArrayList<>();
        addVisit("ClassDeclaration", this::visitClass);
        addVisit("ImportDeclaration", this::visitImport);
        addVisit("MethodDeclaration", this::visitMethod); //pensar nisto pq method declaration é filho de classDeclaration
    }

    public Boolean visitClass(JmmNode node, Boolean dummy) {
        System.out.println(node);

        return true;
    }

    public Boolean visitImport(JmmNode node, Boolean dummy) {
        System.out.println(node);

        return true;
    }

    public Boolean visitMethod(JmmNode node, Boolean dummy) {
        System.out.println(node);

        return true;
    }
}
