import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class SemanticAnalysisUtils {

    public static boolean evaluatesToBoolean(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, List<Report> reports) {
        List<JmmNode> children = node.getChildren();

        for (JmmNode child : children) {

            List<Report> allReports = new ArrayList<>();

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
                    allReports = SemanticAnalysisUtils.evaluateOperationWithBooleans(symbolTable, child, method);
                    break;

                case "Less":
                    allReports = SemanticAnalysisUtils.evaluateOperationWithIntegers(symbolTable, child, method);
                    break;

                case "Not":
                    allReports = SemanticAnalysisUtils.evaluateNotOperation(symbolTable, child, method);
                    break;

                case "FinalTerms":
                    return evaluatesToBoolean(symbolTable, method, child, reports);
                //if(child.getChildren().size)
                case "True":
                case "False":
                    return true;

                default:
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.get("line")), Integer.parseInt(child.get("col")), "Expression should return boolean"));
                    return false;
            }
            reports.addAll(allReports);
            if (!allReports.isEmpty())
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.get("line")), Integer.parseInt(child.get("col")), "Expression should return boolean"));
            else return true;
        }

        return false;
    }

    public static boolean evaluatesToInteger(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, List<Report> reports) {

        if (node.getKind().equals("Length")) {
            return evaluateArray(symbolTable, method, node, reports);
        }

        List<JmmNode> children = node.getChildren();
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

    public static List<Report> evaluateOperationWithBooleans(GrammarSymbolTable symbolTable, JmmNode node, SymbolMethod method) {
        List<JmmNode> children = node.getChildren();
        List<Report> reports = new ArrayList<>();

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

        if (!evaluatesToBoolean(symbolTable, method, children.get(0), reports))
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(children.get(0).get("line")), Integer.parseInt(children.get(0).get("col")), "bad operand type for binary operator '!': boolean expected"));

        return reports;
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
        String identifier = node.getKind().replaceAll("'", "").replace("Identifier ", "");

        Type typeIdentifier = checkIfIdentifierExists(symbolTable, method, identifier);
        if (typeIdentifier == null) {
            return new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "identifier '" + identifier + "' is not declared");
        }

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
            return type;
        } else if (rightOperand && (firstChild.getKind().equals("True") || firstChild.getKind().equals("False")))
            return new Type("Boolean", false);


        return null;
    }

    private static boolean evaluateArray(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, List<Report> reports) {
        JmmNode firstChild = node.getChildren().get(0);

        if (firstChild.getKind().equals("FinalTerms")) {
            JmmNode child = firstChild.getChildren().get(0);
            if (child.getKind().contains("Identifier")) {
                if ((isIdentifier(symbolTable, method, child, true, true) == null) || (isIdentifier(symbolTable, method, child, false, true) == null))
                    return true;
            }
        }
        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(firstChild.get("line")), Integer.parseInt(firstChild.get("col")), "length can only be used for int[] or String[]"));
        return false;
    }

    public static Type evaluateExpression(GrammarSymbolTable symbolTable, SymbolMethod method, JmmNode node, List<Report> reports, boolean rightOperand) {
        List<JmmNode> children = node.getChildren();

        if (children.size() == 1) {
            JmmNode child = children.get(0);
            if (child.getKind().equals("And") && rightOperand) {
                List<Report> r = evaluateOperationWithBooleans(symbolTable, child, method);
                if (r.size() == 0) return new Type("Boolean", false);
                reports.addAll(r);
            } else if (child.getKind().equals("Less") && rightOperand) {
                List<Report> r = evaluateOperationWithIntegers(symbolTable, child, method);
                if (r.size() == 0) return new Type("Boolean", false);
                reports.addAll(r);
            } else if ((child.getKind().equals("Add") || child.getKind().equals("Sub") || child.getKind().equals("Div") || child.getKind().equals("Mult")) && rightOperand) {
                List<Report> r = evaluateOperationWithIntegers(symbolTable, child, method);
                if (r.size() == 0) return new Type("Int", false);
                reports.addAll(r);
            } else if (child.getKind().equals("Not") && rightOperand) {
                List<Report> r = evaluateNotOperation(symbolTable, child, method);
                if (r.size() == 0) return new Type("Boolean", false);
                reports.addAll(r);
            } else if (child.getKind().equals("FinalTerms")) {
                return evaluateFinalTerms(symbolTable, method, child, reports, rightOperand);
            } else if (child.getKind().equals("ArrayAccess")) {
                if (evaluateArrayAccess(symbolTable, method, child, reports))
                    return new Type("Int", false);
            } else if (child.getKind().equals("Length") && rightOperand) {
                if (evaluateArray(symbolTable, method, child, reports))
                    return new Type("Int", false);
            }
        } else if (children.size() == 2 && rightOperand) {
            if (children.get(0).getKind().equals("FinalTerms") && children.get(1).getKind().equals("MethodCall")) {
                return evaluateMethodCall(symbolTable, method, children, reports);
            }
        }
        return null;
    }

    private static Type evaluateMethodCall(GrammarSymbolTable symbolTable, SymbolMethod method, List<JmmNode> nodes, List<Report> reports) {

        JmmNode identifier = nodes.get(0);
        JmmNode methodNode = nodes.get(1);

        String identifierKind = identifier.getChildren().get(0).getKind();

        if (!identifierKind.contains("Identifier") && !identifierKind.equals("This")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(identifier.getChildren().get(0).get("line")), Integer.parseInt(identifier.getChildren().get(0).get("col")), "not a valid identifier"));
            return null;
        }

        String identifierN = "this";
        if (!identifierKind.equals("This")) {
            String identifierName = identifierKind.replaceAll("'", "").replace("Identifier ", "");

            for (String importName : symbolTable.getImports()) {
                String[] imports = importName.split("\\.");
                if (imports[imports.length - 1].equals(identifierName)) return new Type("Accepted", false);
            }

            Type identifierType = checkIfIdentifierExists(symbolTable, method, identifierName);

            if (identifierType == null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(identifier.getChildren().get(0).get("line")), Integer.parseInt(identifier.getChildren().get(0).get("col")), "identifier '" + identifierName + "' is not declared"));
                return null;
            }

            identifierN = identifierType.getName();
            if (identifierN.equals(symbolTable.getSuper())) return new Type("Accepted", false);
            if (identifierN.equals("Int") || identifierN.equals("Boolean") || identifierN.equals("String")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(identifier.getChildren().get(0).get("line")), Integer.parseInt(identifier.getChildren().get(0).get("col")), "identifier cannot be string, int or boolean"));
                return null;
            }
        }

        String methodName = methodNode.getChildren().get(0).getKind().replaceAll("'", "").replace("Identifier ", "");
        List<JmmNode> parameters = methodNode.getChildren();
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