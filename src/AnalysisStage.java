import semanticAnalysis.SemanticAnalysisVisitor;
import symbolTable.SymbolTableVisitor;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.Arrays;
import java.util.List;

public class AnalysisStage implements JmmAnalysis {

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {

        if (TestUtils.getNumReports(parserResult.getReports(), ReportType.ERROR) > 0) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but there are errors from previous stage");
            return new JmmSemanticsResult(parserResult, null, Arrays.asList(errorReport));
        }

        if (parserResult.getRootNode() == null) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but AST root node is null");
            return new JmmSemanticsResult(parserResult, null, Arrays.asList(errorReport));
        }

        JmmNode node = parserResult.getRootNode();

        SymbolTableVisitor visitor = new SymbolTableVisitor();
        visitor.visit(node);

        List<Report> reports = visitor.getReports();

        SemanticAnalysisVisitor semanticAnalysisVisitor = new SemanticAnalysisVisitor(visitor.getSymbolTable());
        semanticAnalysisVisitor.visit(node);
        reports.addAll(semanticAnalysisVisitor.getReports());

        if (reports.size() > 0) {
            System.out.println("\n\n------------ REPORTS (Semantic Analysis) ------------");
            for (Report report : reports)
                System.out.println(report);
            System.out.println("-----------------------------------------------------");
        }
        return new JmmSemanticsResult(parserResult, visitor.getSymbolTable(), reports);
    }

}