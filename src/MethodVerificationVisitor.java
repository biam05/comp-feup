import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class MethodVerificationVisitor extends PreorderJmmVisitor<Boolean, Boolean> {
    private final GrammarSymbolTable symbolTable;
    private final List<Report> reports;
    private final SymbolMethod method;

    public MethodVerificationVisitor(GrammarSymbolTable symbolTable, SymbolMethod method) {
        this.symbolTable = symbolTable;
        this.reports = new ArrayList<>();
        this.method = method;
        addVisit("Expression", this::visitExpression);
    }

    public GrammarSymbolTable getSymbolTable() {
        return symbolTable;
    }

    public List<Report> getReports() {
        return reports;
    }

    public Boolean visitExpression(JmmNode node, boolean dummy) {
        List<JmmNode> children = node.getChildren();
        for (int index = 0; index < children.size(); index++) {
            JmmNode child = children.get(index);
            if (child.getKind().equals("Period")) {
                index++;
                // Get method name after period
                String methodName;
                if (children.get(index).getKind().contains("Identifier"))
                    methodName = children.get(index).getKind().replaceAll("'", "").replace("Identifier ", "");
                else continue;
                index -= 2;
                // Get var name before period
                JmmNode aux = children.get(index).getChildren().get(0);
                String varName = aux.getKind().replaceAll("'", "").replace("Identifier ", "");
                index += 3;
                List<JmmNode> parameters = new ArrayList<>();
                // Get parameters passed on method call
                if (children.get(index).getKind().equals("LParenthesis")) {
                    while (true) {
                        index++;
                        aux = children.get(index);
                        if (aux.getKind().equals("RParenthesis")) break;
                        parameters.add(aux);
                    }
                }
                int line = Integer.parseInt(child.get("line"));
                int col = Integer.parseInt(child.get("col"));

                // Call function to check if var exists and has method methodName and check argument types and number
                if (!isValidMethodCall(method, varName, methodName, parameters, line, col)) {
                    Report report = new Report(ReportType.ERROR, Stage.SEMANTIC, line, col, "Invalid method call");
                    reports.add(report);
                    System.out.println(report);
                }
            }
        }
        return true;
    }

    public Boolean isValidMethodCall(SymbolMethod currentMethod, String varName, String methodName, List<JmmNode> parameters, int line, int col) {
        for (String importName: symbolTable.getImports()) {
            if(varName.equals(importName)) return true;
        }
        for (Symbol var: currentMethod.getLocalVariables()) {
            if ((var.getName().equals(varName) && var.getType().getName().equals(this.symbolTable.getClassName())) || varName.equals("this")) {
                for (SymbolMethod method: symbolTable.getMethodsAndParameters()) {
                    if (method.getName().equals(methodName)) {
                        if (method.getParameters().size() != parameters.size())
                            return false;
                        /*for (int i = 0; i < method.getParameters().size(); i++)
                            if (method.getParameters().get(i).getType() != getType(parameters.get(i), currentMethod))
                                return false;
                        */return true;
                    }
                }
                //TODO: confirm if "!= null" is right
                return symbolTable.getSuper() != null;
            }
        }
        return false;
        /*
        -> verificar se o "target" do método existe, e se este contém o método (e.g. a.foo, ver se 'a' existe e se tem um método 'foo')
            - caso seja do tipo da classe declarada (e.g. a usar o this), se não existir declaração na própria classe: se não tiver extends retorna erro, se tiver extends assumir que é da classe super.
        -> caso o método não seja da classe declarada, isto é uma classe importada, assumir como existente e assumir tipos esperados. (e.g. a = Foo.b(), se a é um inteiro, e Foo é uma classe importada, assumir que o método b é estático (pois estamos a aceder a uma método diretamente da classe), que não tem argumentos e que retorna um inteiro)
        -> verificar se o número de argumentos na invocação é igual ao número de parâmetros da declaração
        -> verificar se o tipo dos parâmetros coincide com o tipo dos argumentos

        ->  this.foo();, class FindMaximum {FindMaximum fm; fm.foo();}
        ->  class Lazysort extends Quicksort {Lazysort ls; ls.quicksort();} (quicksort belonging to class Quicksort)
        ->  import ioPlus; class FindMaximum {a = ioPlus.foo();}
        */
    }

    public Type getType(JmmNode node, SymbolMethod method) {
        System.out.println(node.getKind());
        return new Type("", false);
    }
}
