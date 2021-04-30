import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OLLIRVisitor extends AJmmVisitor<StringBuilder, String> {
    private final GrammarSymbolTable symbolTable;
    private final List<Report> reports;
    private String code = "";
    private int var_temp = 1;
    private SymbolMethod currentMethod;

    public OLLIRVisitor(GrammarSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.reports = new ArrayList<>();
        this.code = OLLIRTemplates.init(symbolTable.getClassName(), symbolTable.getFields());
        addVisit("MethodDeclaration", this::visitMethod);
        addVisit("Statement", this::visitStatement);
        addVisit("Return", this::visitReturn);
        addVisit("Assign", this::visitAssign);
        addVisit("MethodBody", this::visitMethodBody);
        addVisit("Expression", this::visitExpression);
        addVisit("FinalTerms", this::visitFinalTerms);
        addVisit("Call", this::visitCall);
        addVisit("MethodCall", this::visitMethodCall);
        addVisit("Add", this::visitOperation);
        addVisit("Sub", this::visitOperation);
        addVisit("Mult", this::visitOperation);
        addVisit("Div", this::visitOperation);
        addVisit("Not", this::visitOperation);
        addVisit("And", this::visitOperation);
        addVisit("ArrayAccess", this::visitArrayAccess);
        addVisit("Length", this::visitLength);
        setDefaultVisit(this::defaultVisit);
    }

    public List<Report> getReports() { return reports; }

    public String getCode() { return code; }

    private String defaultVisit(JmmNode node, StringBuilder stringBuilder) {
        List<JmmNode> nodes = node.getChildren();
        StringBuilder res = new StringBuilder();
        for(JmmNode child: nodes){
            System.out.println("default: " + child.getKind());
            String re = visit(child);
            res.append(re);
        }
        this.code += res.toString();
        return res.toString();
    }

    private String visitMethod(JmmNode node, StringBuilder stringBuilder) {
        StringBuilder res = new StringBuilder();
        List<JmmNode> children = node.getChildren();
        boolean ret = false;
        boolean alreadyInBody = false;

        StringBuilder methodInfo = new StringBuilder();

        for(int i = 0; i < children.size(); i++){
            JmmNode child = children.get(i);
             if (child.getKind().equals("LParenthesis")) { // parameters
                if (alreadyInBody) break;
                List<JmmNode> parameters = new ArrayList<>();
                while (true) {
                    i++;
                    JmmNode aux = children.get(i);
                    if (aux.getKind().equals("RParenthesis")) break;
                    parameters.add(children.get(i));
                }
                methodInfo.append(SemanticAnalysisUtils.getTypeParameters(parameters));

            } else if (child.getKind().equals("Main")) {
                methodInfo.append("Main(");
            } else if (child.getKind().contains("Identifier")) {
                String methodName = child.getKind().replaceAll("'", "").replace("Identifier ", "");
                methodInfo.append(methodName).append("(");
            }
            else if(child.getKind().equals("MethodBody")){
                methodInfo.append(")");
                this.currentMethod = symbolTable.getMethodByInfo(methodInfo.toString());
                System.out.println("method body 1: " + res);
                res.append(OLLIRTemplates.methodDeclaration(this.currentMethod));
                System.out.println("method body 2: " + res);
                res.append("{\n").append(visit(child)).append("\n");
            }
            else if(child.getKind().equals("Return")){
                ret = true;
                res.append(visit(child));
                res.append("\n}\n");
            }
        }
        if(!ret){
            res.append(OLLIRTemplates.returnVoid());
            res.append("\n}\n");
        }
        var_temp = 1;
        this.currentMethod = null;
        return res.toString();
    }


    private String visitReturn(JmmNode node, StringBuilder stringBuilder) {
        StringBuilder res = new StringBuilder();
        String exp = visit(node.getChildren().get(0));
        String result = OLLIRTemplates.checkReturnTemporary(exp, var_temp);
        return "";
        //return OLLIRTemplates.returnTemplate(values.get(0), method.getReturnType().toOLLIR());
    }


    // TO DO : ou apos receber o retorno dos filhos ele vai ver se necessita de criar variaveis temporarias ou ao correr o visit fazer logo isso (variaveis temporarias)
    private String visitAssign(JmmNode node, StringBuilder stringBuilder) {
        List<JmmNode> children = node.getChildren();
        String left = visit(children.get(0));
        String right = visit(children.get(1));
        String type = OLLIRTemplates.getReturnTypeExpression(left);

        return OLLIRTemplates.assign(left, type, right);
    }


    private String visitMethodBody(JmmNode node, StringBuilder stringBuilder) {
        StringBuilder res = new StringBuilder();
        List<JmmNode> children = node.getChildren();

        for (JmmNode child : children) res.append(visit(child));//only the statements matter

        return res.toString();
    }

    private String visitStatement(JmmNode node, StringBuilder stringBuilder) {
        StringBuilder res = new StringBuilder();
        List<JmmNode> children = node.getChildren();

        for(JmmNode child: children){
            if(!(child.getKind().equals("WhileStatement") || child.getKind().equals("IfExpression"))) res.append(visit(child)); //ifs and whiles are not for this checkpoint
        }
        return res.toString();
    }

    private String visitExpression(JmmNode node, StringBuilder stringBuilder) {
        return visit(node.getChildren().get(0));
    }

    // TODO: verificar tudoooo e limpar
    private String visitFinalTerms(JmmNode node, StringBuilder stringBuilder) {
        System.out.println("visit final term: " + node.getParent());
        StringBuilder result = new StringBuilder();
        List<JmmNode> children = node.getChildren();
        JmmNode child = children.get(0);
        String ret = "", res = "", value = "";
        if(child.getKind().contains("Number")){
            ret = ".i32";
            value = child.getKind().replace("Number", "").replaceAll("'", "").replaceAll(" ", "");
            res = value + ret;
        } else if (child.getKind().equals("NewIntArrayExpression")) { //not for this checkpoint
            ret = "";
            res = "";
        } else if (child.getKind().contains("NewIdentifier")) {
            value = child.getKind().replaceAll("'", "").replace("NewIdentifier ", "").trim();
            res = "new(" + value + ")." + value;
            ret = "." + value;
        } else if (child.getKind().contains("Identifier")){
            value = child.getKind().replaceAll("'", "").replace("Identifier ", "").trim();
            Optional<JmmNode> ancestor = child.getAncestor("MethodDeclaration");
            if(ancestor.isEmpty()) return "";
            ret = SemanticAnalysisUtils.checkIfIdentifierExists(symbolTable, this.currentMethod, value).toOLLIR();
            res = value + ret;
            res = "";
        } else if (child.getKind().equals("True") || child.getKind().equals("False")) {
            int bool = child.getKind().equals("True") ? 1 : 0;
            res = bool + ".bool";
            ret = ".bool";
        } else if(child.getKind().equals("Expression")){
            res = visit(child);
        }

        result.append(res);
        return "ola1";
    }

    //TODO
    private String visitCall(JmmNode node, StringBuilder stringBuilder) {
        StringBuilder res = new StringBuilder();
        List<JmmNode> children = node.getChildren();
        if(children.size() != 2) return "";
        JmmNode left = children.get(0);
        JmmNode right = children.get(1);
        if(left.getKind().equals("This") && right.getKind().equals("MethodCall")){

        }
        else if(left.getKind().contains("Identifier") && right.getKind().equals("FinalTerms")){
            //getfield ou putfield
        }
        else if(left.getKind().contains("Identifier") && right.getKind().contains("MethodCall")){
            //ver qual o tipo de call
        }
        else if(left.getKind().equals("Identifier") && right.getKind().equals("Length")){

        }
        return "ola2";
    }

    //TODO
    private String visitMethodCall(JmmNode node, StringBuilder stringBuilder) {
        StringBuilder res = new StringBuilder();
        return "ola3";
    }

    //TODO
    private String visitLength(JmmNode node, StringBuilder stringBuilder) {
        StringBuilder res = new StringBuilder();
        return "ola4";
    }

    // TODO: construir operação e adicionar à frente o retorno da operaçao
    private String visitOperation(JmmNode node, StringBuilder stringBuilder) {
        StringBuilder res = new StringBuilder();
        return "ola5";
    }

    private String visitArrayAccess(JmmNode node, StringBuilder stringBuilder) { //not for this checkpoint
        StringBuilder res = new StringBuilder();
        return "ola6";
    }

}
