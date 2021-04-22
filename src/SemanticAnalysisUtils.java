import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class EvaluateUtils {

    public static boolean evaluatesToBoolean(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, List<Report> reports) {
        List<JmmNode> children = node.getChildren();
        System.out.println("Evaluating to boolean: " + node + ", " + children);
        for (JmmNode child : children) {

            List<Report> allReports;

            if (child.getKind().contains("Identifier")) {
                Report report = isIdentifier(symbolTable, method, child, false, false);
                if (report != null) {
                    reports.add(report);
                    return false;
                }
                return true;
            }
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

    public static boolean evaluatesToInteger(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, List<Report> reports) {
        List<JmmNode> children = node.getChildren();
        System.out.println("Evaluate to Integer: " + node + ", " + children);

        if (children.size() == 1) {
            JmmNode child = children.get(0);

            if (child.getKind().contains("Number")) return true;

            else if (child.getKind().contains("Identifier")) {
                Report report = isIdentifier(symbolTable, method, child, true, false);
                if (report != null) reports.add(report);
                else return true;
            } else if (child.getKind().equals("ArrayAccess")) {
                return evaluateArrayAccess(symbolTable, method, child, reports);
            } else if (child.getKind().equals("Expression") || child.getKind().equals("FinalTerms"))
                return evaluatesToInteger(symbolTable, method, child, reports);
        } else if (children.size() == 2) {
            List<Report> allReports = evaluateOperationWithIntegers(symbolTable, node, method);
            if (allReports.size() == 0) return true;
            reports.addAll(allReports);
        }
        // MISSING: checking if the expression or method evalutates to integer

        return false;
    }

    private static boolean evaluateArrayAccess(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, List<Report> reports) {
        List<JmmNode> children = node.getChildren();
        System.out.println("evaluate array access: " + node + ", " + children);
        if (!children.get(0).getKind().contains("FinalTerms") && !children.get(0).getChildren().get(0).getKind().contains("Identifier")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(children.get(0).get("line")), Integer.parseInt(children.get(0).get("col")), "non array access trying to be accessed like an array"));
            return false;
        } else {
            Report report = isIdentifier(symbolTable, method, children.get(0).getChildren().get(0), true, true);
            if (report != null) {
                reports.add(report);
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(children.get(0).get("line")), Integer.parseInt(children.get(0).get("col")), "non array access trying to be accessed like an array"));
                return false;
            }
        }
        if (!evaluatesToInteger(symbolTable, method, children.get(1), reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(children.get(1).get("line")), Integer.parseInt(children.get(1).get("col")), "bad array access: integer expected"));
            return false;
        }
        return true;
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

        switch (node.getKind()) {
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

    private static Report checkTypeIdentifier(boolean isInt, boolean isArray, Type type, String line, String col) {
        if (!(type.getName().equals("Int") == isInt)) {
            if (isInt)
                return new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(line), Integer.parseInt(col), "Identifier is expected to be of type int");
            else
                return new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(line), Integer.parseInt(col), "Identifier should be of type int");
        }

        if (type.isArray() != isArray) {
            if (isArray)
                return new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(line), Integer.parseInt(col), "Identifier is expected to be of type int array");
            return new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(line), Integer.parseInt(col), "Identifier is expected to be of type int, int array found");
        }
        return null;
    }

    public static Type checkIfIdentifierExists(GrammarSymbolTable symbolTable, SymbolMethod method, String identifier) {
        Type identifierTypeLocal = method.returnTypeIfExists(identifier);
        Type identifierTypeClass = symbolTable.returnFieldTypeIfExists(identifier);

        if (identifierTypeLocal == null && identifierTypeClass == null) return null;
        return (identifierTypeClass != null ? identifierTypeClass : identifierTypeLocal);
    }

    public static Report isIdentifier(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, boolean isInt, boolean isArray) {
        String identifier = node.getKind().replaceAll("'", "").replace("Identifier ", "");

        Type typeIdentifier = checkIfIdentifierExists(symbolTable, method, identifier);
        if (typeIdentifier == null) {
            return new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "identifier '" + identifier + "' is not declared");
        }

        return checkTypeIdentifier(isInt, isArray, typeIdentifier, node.get("line"), node.get("col"));
    }

    private static Type evaluateFinalTerms(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, List<Report> reports, boolean rightOperand) {
        List<JmmNode> children = node.getChildren();
        System.out.println("Evaluate Final Terms: " + node + ", " + children);

        JmmNode firstChild = children.get(0);

        if (firstChild.getKind().contains("Number")) return new Type("Int", false);
        else if (firstChild.getKind().equals("NewIntArrayExpression") && rightOperand) {
            if (evaluatesToInteger(symbolTable, method, firstChild.getChildren().get(0), reports))
                return new Type("Int", true);
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(firstChild.getChildren().get(0).get("line")), Integer.parseInt(firstChild.getChildren().get(0).get("col")), "bad array access: integer expected"));
        } else if (firstChild.getKind().contains("NewIdentifier") && rightOperand) {
            String newIdentifier = firstChild.getKind().replaceAll("'", "").replace("NewIdentifier ", "");
            return new Type(newIdentifier, false);
        } else if (firstChild.getKind().contains("Identifier")) {
            String identifier = firstChild.getKind().replaceAll("'", "").replace("Identifier ", "");
            Type type = checkIfIdentifierExists(symbolTable, method, identifier);
            if (type == null)
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(firstChild.get("line")), Integer.parseInt(firstChild.get("col")), "identifier '" + identifier + "' is not declared"));
            return type;
        } else if (rightOperand && (firstChild.getKind().equals("True") || firstChild.getKind().equals("False")))
            return new Type("Boolean", false);


        return null;
    }

    public static Type evaluateExpression(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, List<Report> reports, boolean rightOperand) {

        System.out.println("Evaluate Expression: " + node + ", " + node.getChildren());
        List<JmmNode> children = node.getChildren();

        if (children.size() == 1) {
            if (children.get(0).getKind().equals("Not") && rightOperand) {
                List<Report> r = evaluateNotOperation(symbolTable, children.get(0), method);
                if (r.size() == 0) return new Type("Boolean", false);
                reports.addAll(r);
            } else if (children.get(0).getKind().equals("FinalTerms")) {
                return evaluateFinalTerms(symbolTable, method, children.get(0), reports, rightOperand);
            } else if (children.get(0).getKind().equals("ArrayAccess")) {
                if (evaluateArrayAccess(symbolTable, method, children.get(0), reports))
                    return new Type("Int", true);
            }
        } else if (children.size() == 3) {
            if (children.get(0).getKind().equals("FinalTerms") && children.get(1).getKind().equals("Period") && children.get(2).getKind().equals("Length")) {
                JmmNode firstChild = children.get(0).getChildren().get(0);
                if (firstChild.getKind().contains("Identifier")) {
                    if ((isIdentifier(symbolTable, method, firstChild, true, true) == null) || (isIdentifier(symbolTable, method, firstChild, false, true) == null))
                        return new Type("Int", false);
                }
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(firstChild.get("line")), Integer.parseInt(firstChild.get("col")), "length can only be used for int[] or String[]"));
            }
        }
        return null;
    }

}