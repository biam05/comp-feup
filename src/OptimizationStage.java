import ollir.OLLIRVisitor;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import symbolTable.GrammarSymbolTable;

import java.util.List;

/**
 * Copyright 2021 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

public class OptimizationStage implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        JmmNode node = semanticsResult.getRootNode();

        // Convert the AST to a String containing the equivalent OLLIR code
        OLLIRVisitor ollirVisitor = new OLLIRVisitor((GrammarSymbolTable) semanticsResult.getSymbolTable());
        ollirVisitor.visit(node);
        String ollirCode = ollirVisitor.getCode() + "}";

        System.out.println("\n------------ OLLIR CODE ------------");
        System.out.println(ollirCode);
        System.out.println("------------------------------------");

        // More reports from this stage
        List<Report> reports = ollirVisitor.getReports();
        if (reports.size() > 0) {
            System.out.println("\n\n------------ REPORTS (Ollir) ------------");
            for (Report report : reports)
                System.out.println(report);
            System.out.println("-----------------------------------------");
        }
        return new OllirResult(semanticsResult, ollirCode, reports);
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        JmmNode node = semanticsResult.getRootNode();

        OptimizationVisitor optimizationVisitor = new OptimizationVisitor((GrammarSymbolTable) semanticsResult.getSymbolTable());
        optimizationVisitor.visit(node);

        return semanticsResult;
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        // THIS IS JUST FOR CHECKPOINT 3
        return ollirResult;
    }

}
