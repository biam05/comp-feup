import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class EvaluateUtils {

    public static boolean evaluatesToBoolean(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, List<Report> reports){
        List <JmmNode> children = node.getChildren();
        System.out.println("Evaluating to boolean: " + node + ", " + children);
        for (JmmNode child : children) {

            List<Report> allReports;

            switch (child.getKind()) {
                case "And":
                    allReports = EvaluateUtils.evaluateOperationWithBooleans(symbolTable, child, method);
                    break;

                case "Less":
                    allReports = EvaluateUtils.evaluateOperationWithIntegers(symbolTable, child, method);
                    break;

                case "Not":
                    allReports = EvaluateUtils.evaluateNotOperation(symbolTable, child, method);
                    break;

                case "FinalTerms":
                    //if(child.getChildren().size)
                case "True":
                case "False":
                    return true;

                default:
                    return false;
            }

            reports.addAll(allReports);
        }
        //MISSING: checking if the expression or method evaluates to boolean
        return false;
    }

    public static boolean evaluatesToInteger(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, List<Report> reports){
        List <JmmNode> children = node.getChildren();
        System.out.println("Evaluate to Integer: " + node + ", " + children);

        if(children.size() == 1) {
            JmmNode child = children.get(0);

            if(child.getKind().contains("Number")) return true;

            else if(child.getKind().contains("Identifier")) {
                Report report = isIdentifier(symbolTable, method, child, true, false);
                if(report != null) reports.add(report);
                else return true;
            }
            else if(child.getKind().equals("ArrayAccess")){
                if(!child.getChildren().get(0).getKind().contains("Identifier")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.getChildren().get(0).get("line")), Integer.parseInt(child.getChildren().get(0).get("col")), "non array access trying to be accessed like an array"));
                    return false;
                }
                else {
                    Report report = isIdentifier(symbolTable, method, child.getChildren().get(0), true, true);
                    if(report != null) {
                        reports.add(report);
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.getChildren().get(0).get("line")), Integer.parseInt(child.getChildren().get(0).get("col")), "non array access trying to be accessed like an array"));
                        return false;
                    }
                }
                if(!evaluatesToInteger(symbolTable, method, child.getChildren().get(1), reports)){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.getChildren().get(1).get("line")), Integer.parseInt(child.getChildren().get(1).get("col")), "bad array access: integer expected"));
                    return false;
                }
                return true;
            }
        }
        else if(children.size() == 2) {
            List<Report> allReports = evaluateOperationWithIntegers(symbolTable, node, method);
            if(allReports.size() == 0) return true;
            reports.addAll(allReports);
        }
        // MISSING: checking if the expression or method evalutates to integer

        return false;
    }

    public static List<Report> evaluateOperationWithBooleans(GrammarSymbolTable symbolTable, JmmNode node, SymbolMethod method) {
        List<JmmNode> children = node.getChildren();
        List<Report> reports = new ArrayList<>();

        System.out.println("Evaluate Operation With Booleans: " + node + ", " + children);
        if (!evaluatesToBoolean(symbolTable, method, children.get(0), null)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(children.get(0).get("line")), Integer.parseInt(children.get(0).get("col")), "left operand for binary operator '&&' is not a boolean"));
        }
        if (!evaluatesToBoolean(symbolTable, method, children.get(1), null)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(children.get(1).get("line")), Integer.parseInt(children.get(1).get("col")), "right operand for binary operator '&&' is not a boolean"));
        }

        return reports;
    }

    public static List<Report> evaluateOperationWithIntegers(GrammarSymbolTable symbolTable, JmmNode node, SymbolMethod method) {
        List<JmmNode> children = node.getChildren();
        List<Report> reports = new ArrayList<>();

        char operation = ' ';

        switch(node.getKind()) {
            case "Add":
                operation = '+';
                break;

            case "Sub":
                operation = '-';
                break;

            case "Mult":
                operation = '*';
                break;

            case "Div":
                operation = '/';
                break;
            case "Less":
                operation = '<';
                break;
        }

        System.out.println("Evaluate Operation With Integers: " + node + ", " + children);
        if (!evaluatesToInteger(symbolTable, method, children.get(0), reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(children.get(0).get("line")), Integer.parseInt(children.get(0).get("col")), "left operand type for binary operator '" + operation + "' is not an integer"));
        }
        if (!evaluatesToInteger(symbolTable, method, children.get(1), reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(children.get(1).get("line")), Integer.parseInt(children.get(1).get("col")), "right operand type for binary operator '" + operation + "' is not an integer"));
        }

        return reports;
    }

    public static List<Report> evaluateNotOperation(GrammarSymbolTable symbolTable, JmmNode node, SymbolMethod method) {
        List<JmmNode> children = node.getChildren();
        List<Report> reports = new ArrayList<>();

        System.out.println("Evaluate Not Operation: " + node + ", " + children);
        if (!evaluatesToBoolean(symbolTable, method, children.get(0), reports))
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(children.get(0).get("line")), Integer.parseInt(children.get(0).get("col")), "bad operand type for binary operator '!': boolean expected"));

        return reports;
    }

    public static Report isIdentifier(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, boolean isInt, boolean isArray) {

        String identifier = node.getKind().replaceAll("'", "").replace("Identifier ", "");
        Type identifierTypeLocal = method.returnTypeIfExists(identifier);
        Type identifierTypeClass = symbolTable.returnFieldTypeIfExists(identifier);

        if (identifierTypeLocal == null && identifierTypeClass == null) {
            return new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "identifier '" + identifier + "' is not declared");
        }

        boolean local = (identifierTypeLocal != null);

        if ((local && !(identifierTypeLocal.getName().equals("Int") == isInt)) || (!local && !(identifierTypeClass.getName().equals("Int") == isInt))) {
            if (isInt)
                return new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Identifier is expected to be of type int");
            else
                return new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Identifier should be of type int");
        }

        if ((local && !(identifierTypeLocal.isArray() == isArray)) || (!local && !(identifierTypeClass.isArray() == isArray))) {
            if (isArray)
                return new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Identifier is expected to be of type int array");
            return new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Identifier is expected to be of type int, int array found");
        }
        return null;
    }

}