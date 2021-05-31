package semanticAnalysis;

import symbolTable.GrammarSymbolTable;
import symbolTable.SymbolMethod;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class SemanticAnalysisUtils {

    public static boolean evaluatesToBoolean(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, List<Report> reports) {
        Type type = new Type("Boolean", false);

        switch (node.getKind()) {
            case "Call":
                if (type.equals(evaluateCall(symbolTable, method, node, reports))) return true;
            case "Expression":
            case "IfExpression":
                if (type.equals(evaluateExpression(symbolTable, method, node, reports, true))) return true;
            case "Not":
                if (evaluateNotOperation(symbolTable, method, node, reports)) return true;
            case "And":
                if (evaluateOperationWithBooleans(symbolTable, method, node, reports)) return true;
            case "Less":
                if (evaluateOperationWithIntegers(symbolTable, method, node, reports)) return true;
            case "FinalTerms":
                if (type.equals(evaluateFinalTerms(symbolTable, method, node, reports, true))) return true;
            default:
                break;
        }

        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Expression should return boolean"));
        return false;
    }

    public static boolean evaluatesToInteger(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, List<Report> reports) {

        Type type = new Type("Int", false);
        switch (node.getKind()) {
            case "Call":
                if (type.equals(evaluateCall(symbolTable, method, node, reports))) return true;
            case "Expression":
                if (type.equals(evaluateExpression(symbolTable, method, node, reports, true))) return true;
            case "ArrayAccess":
                if (evaluateArrayAccess(symbolTable, method, node, reports)) return true;
            case "FinalTerms":
                if (type.equals(evaluateFinalTerms(symbolTable, method, node, reports, true))) return true;
            case "Add":
            case "Sub":
            case "Mult":
            case "Div":
                if (evaluateOperationWithIntegers(symbolTable, method, node, reports)) return true;
            default:
                break;
        }

        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Expression should return int"));
        return false;
    }

    private static boolean evaluateArrayAccess(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, List<Report> reports) {
        List<JmmNode> children = node.getChildren();
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

    public static boolean evaluateOperationWithBooleans(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, List<Report> reports) {
        List<JmmNode> children = node.getChildren();

        if (children.size() != 2) return false;

        boolean hasReport = false;

        if (!evaluatesToBoolean(symbolTable, method, children.get(0), reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(children.get(0).get("line")), Integer.parseInt(children.get(0).get("col")), "left operand for binary operator '&&' is not a boolean"));
            hasReport = true;
        }

        if (!evaluatesToBoolean(symbolTable, method, children.get(1), reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(children.get(1).get("line")), Integer.parseInt(children.get(1).get("col")), "right operand for binary operator '&&' is not a boolean"));
            hasReport = true;
        }
        return !hasReport;
    }

    public static boolean evaluateOperationWithIntegers(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, List<Report> reports) {
        List<JmmNode> children = node.getChildren();

        if (children.size() != 2) return false;

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

        boolean hasReport = false;
        if (!evaluatesToInteger(symbolTable, method, children.get(0), reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(children.get(0).get("line")), Integer.parseInt(children.get(0).get("col")), "left operand type for binary operator '" + operation + "' is not an integer"));
            hasReport = true;
        }
        if (!evaluatesToInteger(symbolTable, method, children.get(1), reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(children.get(1).get("line")), Integer.parseInt(children.get(1).get("col")), "right operand type for binary operator '" + operation + "' is not an integer"));
            hasReport = true;
        }

        return !hasReport;
    }

    public static boolean evaluateNotOperation(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, List<Report> reports) {
        List<JmmNode> children = node.getChildren();

        if (!evaluatesToBoolean(symbolTable, method, children.get(0), reports)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(children.get(0).get("line")), Integer.parseInt(children.get(0).get("col")), "bad operand type for binary operator '!': boolean expected"));
            return false;
        }
        return true;
    }

    private static Report checkTypeIdentifier(boolean isInt, boolean isArray, Type type, String line, String col) {
        if (!(type.getName().equals("Int") == isInt)) {
            if (isInt)
                return new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(line), Integer.parseInt(col), "Identifier is expected to be of type int");
            else
                return new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(line), Integer.parseInt(col), "Identifier is expected to be type different of int");
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
        String identifier = node.getKind().replaceAll("'", "").replace("Identifier ", "").trim();

        Type typeIdentifier = checkIfIdentifierExists(symbolTable, method, identifier);
        if (typeIdentifier == null)
            return new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "identifier '" + identifier + "' is not declared");

        return checkTypeIdentifier(isInt, isArray, typeIdentifier, node.get("line"), node.get("col"));
    }

    private static Type evaluateFinalTerms(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, List<Report> reports, boolean rightOperand) {

        List<JmmNode> children = node.getChildren();
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
            else if (method.isMain() && symbolTable.returnFieldTypeIfExists(identifier) != null)
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(firstChild.get("line")), Integer.parseInt(firstChild.get("col")), "non-static variable '" + identifier + "' cannot be referenced from a static context"));

            return type;
        } else if (rightOperand && (firstChild.getKind().equals("True") || firstChild.getKind().equals("False")))
            return new Type("Boolean", false);
        else if (rightOperand && firstChild.getKind().equals("Expression"))
            return evaluateExpression(symbolTable, method, firstChild, reports, rightOperand);

        return null;
    }


    private static boolean evaluateArray(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, List<Report> reports) {

        if (node.getKind().equals("Call")) {
            Type type = evaluateCall(symbolTable, method, node, reports);
            if (type == null) return false;
            if (type.equals(new Type("String", true)) || type.equals(new Type("Int", true))) return true;
        }

        List<JmmNode> children = node.getChildren();
        if (children.size() == 1) {
            JmmNode child = children.get(0);
            if (child.getKind().contains("Identifier")) {
                String identifier = child.getKind().replaceAll("'", "").replace("Identifier ", "");
                if (method.isMain() && symbolTable.returnFieldTypeIfExists(identifier) != null)
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.get("line")), Integer.parseInt(child.get("col")), "non-static variable '" + identifier + "' cannot be referenced from a static context"));
                if ((isIdentifier(symbolTable, method, child, true, true) == null) || (isIdentifier(symbolTable, method, child, false, true) == null))
                    return true;
            }

        }

        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(children.get(0).get("line")), Integer.parseInt(children.get(0).get("col")), "length can only be used for int[] or String[]"));
        return false;
    }

    public static Type evaluateExpression(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, List<Report> reports, boolean rightOperand) {
        List<JmmNode> children = node.getChildren();

        if (children.size() == 1) {
            JmmNode child = children.get(0);
            if (child.getKind().equals("And") && rightOperand) {
                if (evaluateOperationWithBooleans(symbolTable, method, child, reports))
                    return new Type("Boolean", false);
            } else if (child.getKind().equals("Less") && rightOperand) {
                if (evaluateOperationWithIntegers(symbolTable, method, child, reports))
                    return new Type("Boolean", false);
            } else if ((child.getKind().equals("Add") || child.getKind().equals("Sub") || child.getKind().equals("Div") || child.getKind().equals("Mult")) && rightOperand) {
                if (evaluateOperationWithIntegers(symbolTable, method, child, reports)) return new Type("Int", false);
            } else if (child.getKind().equals("Not") && rightOperand) {
                if (evaluateNotOperation(symbolTable, method, child, reports)) return new Type("Boolean", false);

            } else if (child.getKind().equals("FinalTerms")) {
                return evaluateFinalTerms(symbolTable, method, child, reports, rightOperand);
            } else if (child.getKind().equals("ArrayAccess")) {
                if (evaluateArrayAccess(symbolTable, method, child, reports))
                    return new Type("Int", false);
            } else if (child.getKind().equals("Length") && rightOperand) {
                if (evaluateArray(symbolTable, method, child, reports))
                    return new Type("Int", false);
            } else if (child.getKind().equals("Call")) {
                return evaluateCall(symbolTable, method, child, reports);
            }
        }

        return null;
    }

    private static Type evaluateCall(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, List<Report> reports) {
        List<JmmNode> children = node.getChildren();
        if (children.size() != 2) return null;
        if (children.get(0).getKind().equals("FinalTerms") || children.get(0).getKind().equals("Call")) {
            if (children.get(1).getKind().equals("MethodCall")) {
                return evaluateMethodCall(symbolTable, method, children, reports);
            } else if (children.get(1).getKind().equals("Length") && evaluateArray(symbolTable, method, children.get(0), reports))
                return new Type("Int", false);
        }

        return null;
    }

    private static Type evaluateMethodCall(GrammarSymbolTable symbolTable, SymbolMethod method, List<JmmNode> nodes, List<Report> reports) {

        JmmNode identifier = nodes.get(0);
        JmmNode methodNode = nodes.get(1);

        String identifierKind = identifier.getChildren().get(0).getKind();
        Boolean hasNestedCall = false;
        String identifierN = "this";

        if (identifier.getKind().equals("Call")) {
            hasNestedCall = true;
            Type t = evaluateCall(symbolTable, method, identifier, reports);
            if (t != null) {
                Type res = symbolTable.hasImport(t.getName());
                if (t.equals(res)) return t;
                else if (t.getName().equals(symbolTable.getSuper())) return new Type("Accepted", false);
                else if (t.getName().equals(symbolTable.getClassName())) identifierN = t.getName();
                else {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(identifier.getChildren().get(0).get("line")), Integer.parseInt(identifier.getChildren().get(0).get("col")), "method does not exist or is being invoked with the wrong arguments"));
                    return null;
                }
            }
        } else if (!identifierKind.contains("Identifier") && !identifierKind.equals("This")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(identifier.getChildren().get(0).get("line")), Integer.parseInt(identifier.getChildren().get(0).get("col")), "not a valid identifier"));
            return null;
        }


        if (!hasNestedCall) {
            Boolean isNew = false;
            if (!identifierKind.equals("This")) {
                if (identifierKind.contains("NewIdentifier")) isNew = true;
                String identifierName = identifierKind.replaceAll("'", "").replace("Identifier ", "").replace("NewIdentifier ", "");
                Type res = symbolTable.hasImport(identifierName);
                if (res != null) return res;

                if (!isNew) {
                    Type identifierType = checkIfIdentifierExists(symbolTable, method, identifierName);

                    if (identifierType == null) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(identifier.getChildren().get(0).get("line")), Integer.parseInt(identifier.getChildren().get(0).get("col")), "identifier '" + identifierName + "' is not declared"));
                        return null;
                    } else if (method.isMain() && symbolTable.returnFieldTypeIfExists(identifierName) != null) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(identifier.getChildren().get(0).get("line")), Integer.parseInt(identifier.getChildren().get(0).get("col")), "non-static variable '" + identifierName + "' cannot be referenced from a static context"));
                    }


                    identifierN = identifierType.getName();
                } else identifierN = identifierName;

                if (identifierN.equals(symbolTable.getSuper())) return new Type("Accepted", false);
                if (identifierN.equals("Int") || identifierN.equals("Boolean") || identifierN.equals("String")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(identifier.getChildren().get(0).get("line")), Integer.parseInt(identifier.getChildren().get(0).get("col")), "identifier cannot be string, int or boolean"));
                    return null;
                }
            }
        }

        String methodName = methodNode.getChildren().get(0).getKind().replaceAll("'", "").replace("Identifier ", "");

        List<JmmNode> parameters = new ArrayList<>(methodNode.getChildren());
        parameters.remove(0);

        List<String> p = new ArrayList<>();
        for (JmmNode parameter : parameters) {
            Type type = evaluateExpression(symbolTable, method, parameter, reports, true);

            if (type == null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(parameter.get("line")), Integer.parseInt(parameter.get("col")), "parameter is not valid"));
                return null;
            }
            p.add(type.printType());
        }

        String methodInfo = methodName + "(" + String.join(",", p) + ")";

        Type type = symbolTable.getReturnType(methodInfo);

        if (type != null) return type;

        if (identifierN.equals("this") || identifierN.equals(symbolTable.getClassName())) {
            if (!symbolTable.getSuper().equals("")) return new Type("Accepted", false);
        }

        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(identifier.getChildren().get(0).get("line")), Integer.parseInt(identifier.getChildren().get(0).get("col")), "method does not exist or is being invoked with the wrong arguments"));
        return null;
    }


    public static String getTypeParameters(List<JmmNode> parameters) {
        List<String> types = new ArrayList<>();

        if ((parameters.size() == 0) || (parameters.size() % 2 != 0)) return "";

        for (int i = 0; i < parameters.size(); i++) {
            JmmNode nodeType = parameters.get(i++);
            Type type = new Type(nodeType);
            types.add(type.printType());
        }

        return String.join(",", types);
    }
}