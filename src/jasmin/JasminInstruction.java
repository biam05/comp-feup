package jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.report.Report;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class JasminInstruction {
    private final Instruction instruction;
    private final StringBuilder jasminCode;
    private final List<Report> reports;
    private final JasminMethod method;

    public JasminInstruction(Instruction instruction, JasminMethod method) {
        this.instruction = instruction;
        this.jasminCode = new StringBuilder();
        this.reports = new ArrayList<>();
        this.method = method;
    }

    public StringBuilder getJasminCode() {
        return jasminCode;
    }

    public List<Report> getReports() {
        return reports;
    }

    public void generateJasminCode() {
        instruction.show();
        switch (instruction.getInstType()) {
            case ASSIGN:
                generateAssign((AssignInstruction) instruction);
                break;
            case CALL:
                generateCall((CallInstruction) instruction);
                break;
            case RETURN:
                generateReturn((ReturnInstruction) instruction);
                break;
            case PUTFIELD:
                generatePutField((PutFieldInstruction) instruction);
                break;
            case BRANCH:
                generateBranch((CondBranchInstruction) instruction);
                break;
            case GOTO:
                generateGoto((GotoInstruction) instruction);
                break;

            default:
                break;
        }
    }

    // -------------- Assign Instructions --------------
    private void generateAssign(AssignInstruction instruction) {
        Instruction rhs = instruction.getRhs();
        switch (rhs.getInstType()) {
            case NOPER:
                generateAssignNOper(instruction);
                break;
            case BINARYOPER:
                generateAssignBinaryOper(instruction);
                break;
            case GETFIELD:
                generateAssignGetfield(instruction);
                break;
            case CALL:
                generateAssignCall(instruction);
                break;
        }
    }

    private void generateAssignNOper(AssignInstruction instruction){
        String variable;
        Instruction rhs = instruction.getRhs();

        variable = ((Operand) instruction.getDest()).getName();

        Element singleOperand = ((SingleOpInstruction) rhs).getSingleOperand();
        Element destiny = instruction.getDest();
        Descriptor localVariable = method.getLocalVariableByKey(((Operand) destiny).getName(), null, destiny.getType());

        if (localVariable.getVarType().getTypeOfElement() != ElementType.ARRAYREF) {
            constOrLoad(singleOperand, null);
            decideType(instruction.getDest());
            int stored = method.getLocalVariableByKey(variable, VarScope.LOCAL, destiny.getType()).getVirtualReg();
            jasminCode.append(JasminUtils.getStoreSize(stored));

        } else {
            jasminCode.append("\n\t\ta").append(JasminUtils.getLoadSize(method.getLocalVariableByKey(variable, null, destiny.getType()).getVirtualReg()));
            Operand indexOp = (Operand) ((ArrayOperand) destiny).getIndexOperands().get(0);
            jasminCode.append("\n\t\ti").append(JasminUtils.getLoadSize(method.getLocalVariableByKey(indexOp.getName(), null, indexOp.getType()).getVirtualReg()));
            constOrLoad(singleOperand, null);
            jasminCode.append("\n\t\tiastore").append("\n");
        }
    }

    private void generateAssignBinaryOper(AssignInstruction instruction){
        Instruction rhs = instruction.getRhs();

        OperationType operation = ((BinaryOpInstruction) rhs).getUnaryOperation().getOpType();
        Element leftElement = ((BinaryOpInstruction) rhs).getLeftOperand();
        Element rightElement = ((BinaryOpInstruction) rhs).getRightOperand();

        if (iincInstruction(leftElement, rightElement, instruction.getDest(), operation)) return;

        constOrLoad(leftElement, VarScope.LOCAL);

        constOrLoad(rightElement, VarScope.LOCAL);

        if (operation.toString().equals("LTH")) getLessThanOperation(instruction);
        else {
            decideType(leftElement);
            jasminCode.append(operation.toString().toLowerCase(Locale.ROOT));
            storeOrIastore(instruction.getDest());
        }
    }

    private void getLessThanOperation(AssignInstruction instruction){
        jasminCode.append("\n\n\t\tif_icmpge ").append("ElseLTH").append(method.getN_branches());
        decideType(instruction.getDest());
        jasminCode.append("const_1");

        storeOrIastore(instruction.getDest());

        jasminCode.append("\n\t\tgoto ").append("AfterLTH").append(method.getN_branches());

        jasminCode.append("\n\n\tElseLTH").append(method.getN_branches()).append(":");
        decideType(instruction.getDest());
        jasminCode.append("const_0");

        storeOrIastore(instruction.getDest());

        jasminCode.append("\n\n\tAfterLTH").append(method.getN_branches()).append(":");
        method.incN_branches();
    }

    private void generateAssignGetfield(AssignInstruction instruction){
        Instruction rhs = instruction.getRhs();
        generateGetField((GetFieldInstruction) rhs);
        storeOrIastore(instruction.getDest());
    }

    private void generateAssignCall(AssignInstruction instruction){
        Instruction rhs = instruction.getRhs();
        generateCall((CallInstruction) rhs);
        Element firstArg = ((CallInstruction) rhs).getFirstArg();
        Operand opFirstArg = (Operand) firstArg;
        if(firstArg.getType().getTypeOfElement() == ElementType.OBJECTREF &&
                opFirstArg.getName().equals(method.getClassName())) {
            jasminCode.append("\n\t\tinvokespecial ").append(method.getClassName()).append(".<init>()V");
        }
        storeOrIastore(instruction.getDest());
    }

    // -------------- Call Instructions --------------
    private void generateCall(CallInstruction instruction) {
        if(method.getMethod().isConstructMethod()){
            jasminCode.append("\n\taload_0");
            jasminCode.append("\n\tinvokespecial java/lang/Object.<init>()V");
            jasminCode.append("\n\treturn");
        }
        else{
            Element firstArg = instruction.getFirstArg();
            Operand opFirstArg = (Operand)firstArg;

            // objectref
            if(firstArg.getType().getTypeOfElement() == ElementType.OBJECTREF)
                generateCallObjectRef(instruction);
            // new array
            else if (opFirstArg.getType().getTypeOfElement() == ElementType.ARRAYREF &&
                    instruction.getInvocationType() == CallType.NEW)
                generateNewArray(instruction);
            // arraylength
            else if (opFirstArg.getType().getTypeOfElement() == ElementType.ARRAYREF && instruction.getInvocationType() == CallType.arraylength) {
                decideType(opFirstArg);
                jasminCode.append(JasminUtils.getLoadSize(method.getLocalVariableByKey(opFirstArg.getName(), null, opFirstArg.getType()).getVirtualReg()));
                jasminCode.append("\n\t\tarraylength");
            }
            // Static call to a method
            else if (opFirstArg.getType().getTypeOfElement() == ElementType.CLASS)
                generateStaticMethod(instruction);
            // Case method is declared in the class
            else if(opFirstArg.getName().equals("this")) {
                generateClassMethod(instruction);
            }
        }
    }

    private void generateCallObjectRef(CallInstruction instruction){
        Element firstArg = instruction.getFirstArg();
        Operand opFirstArg = (Operand)firstArg;

        if (opFirstArg.getName().equals(method.getClassName())) {
            jasminCode.append("\n\t\tnew ").append(method.getClassName());
            jasminCode.append("\n\t\tdup");
        }
        else {
            if (instruction.getNumOperands() > 1) {
                Element secondArg = instruction.getSecondArg();
                if (secondArg.isLiteral()) {
                    if (((LiteralElement) secondArg).getLiteral().replace("\"", "").equals("<init>"))
                        return;
                }
            }

            loadOrAload(opFirstArg, null); // gimme 1 sec
            for (Element parameter : instruction.getListOfOperands()) {
                loadOrAload(parameter, VarScope.LOCAL);
            }
            jasminCode.append("\n\t\tinvokevirtual ");
            jasminCode.append(method.getClassName());
            invokeParameters(instruction);
        }
    }

    private void generateNewArray(CallInstruction instruction){
        Element element = instruction.getListOfOperands().get(0);
        if (element.isLiteral())
            jasminCode.append(JasminUtils.getConstSize(((LiteralElement) element).getLiteral()));
        else {
            decideType(element);
            loadOrAload(element, null);
        }
        jasminCode.append("\n\t\tnewarray int");
    }

    private void generateStaticMethod(CallInstruction instruction){
        Element firstArg = instruction.getFirstArg();
        Operand opFirstArg = (Operand)firstArg;

        for (Element parameter : instruction.getListOfOperands()) {
            loadOrAload(parameter, VarScope.LOCAL);
        }
        jasminCode.append("\n\t\tinvokestatic ");
        jasminCode.append(opFirstArg.getName());
        invokeParameters(instruction);
    }

    private void generateClassMethod(CallInstruction instruction){
        jasminCode.append("\n\t\taload_0");
        jasminCode.append("\n\t\tinvokevirtual ");
        jasminCode.append(method.getClassName());
        invokeParameters(instruction);
    }

    // -------------- Return Instruction --------------
    private void generateReturn(ReturnInstruction instruction) {
        Element e1 = instruction.getOperand();
        if (e1 != null) {
            if (!e1.isLiteral()) {
                String returnedVariable = ((Operand) instruction.getOperand()).getName();
                int value = method.getLocalVariableByKey(returnedVariable, null, instruction.getOperand().getType()).getVirtualReg();
                String type = decideType(instruction.getOperand());
                jasminCode.append(JasminUtils.getLoadSize(value)).append("\n\t\t");
                if (type == null) {
                    decideType(instruction.getOperand());
                    jasminCode.append("return");
                }
                else {
                    jasminCode.append(type).append("return");
                }
            }
            else {
                String literal = ((LiteralElement) e1).getLiteral();
                jasminCode.append(JasminUtils.getConstSize(literal));
                jasminCode.append("\n\t\tireturn");
            }

        } else {
            jasminCode.append("\n\t\treturn");
        }
    }

    // -------------- Putfield Instructions --------------
    private void generatePutField(PutFieldInstruction instruction) {

        Element e1 = instruction.getFirstOperand();
        Element e2 = instruction.getSecondOperand();
        Element e3 = instruction.getThirdOperand();
        Operand o1 = (Operand) e1;
        Operand o2 = (Operand) e2;

        String name = o1.getName();
        loadOrAload(e1, VarScope.FIELD);
        constOrLoad(e3, null);

        if(name.equals("this")) name = method.getClassName();

        jasminCode.append("\n\t\tputfield ").append(name).append("/").append(o2.getName()).append(" ").append(JasminUtils.getReturnFromMethod(method.getMethod(),e2.getType()));

    }

    // -------------- Getfield Instructions --------------
    private void generateGetField(GetFieldInstruction instruction) {
        String firstName = "";
        Element e1 = instruction.getFirstOperand();
        if (!e1.isLiteral()) { // if the e1 is not a literal, then it is a variable
            Operand o1 = (Operand) e1;
            firstName = o1.getName();
            jasminCode.append("\n\t\ta").append(JasminUtils.getLoadSize(method.getLocalVariableByKey(o1.getName(), VarScope.FIELD, o1.getType()).getVirtualReg()));
        }

        if (firstName.equals("this")) firstName = method.getClassName();
        jasminCode.append("\n\t\tgetfield ").append(firstName).append("/");
        e1 = instruction.getSecondOperand();

        if (!e1.isLiteral()) { // if the e1 is not a literal, then it is a variable
            Operand o1 = (Operand) e1;
            jasminCode.append(o1.getName()).append(" ").append(JasminUtils.getReturnFromMethod(method.getMethod(),o1.getType()));
        }

    }

    private void generateBranch(CondBranchInstruction instruction) {
        constOrLoad(instruction.getLeftOperand(),null);
        constOrLoad(instruction.getRightOperand(),null);

        OperationType conditionType = instruction.getCondOperation().getOpType();
        switch (conditionType) {
            case EQ:
                jasminCode.append("\n\t\tif_icmpeq ");
                break;
            case LTH:
                jasminCode.append("\n\t\tif_icmplt ");
                break;
            case ANDB:
                jasminCode.append("\n\t\tiandb");
                jasminCode.append("\n\t\ticonst_1");
                jasminCode.append("\n\t\tif_icmpeq ");
                break;
            default:
                break;
        }
        jasminCode.append(instruction.getLabel());
    }

    private void generateGoto(GotoInstruction instruction) {
        jasminCode.append("\n\t\tgoto ").append(instruction.getLabel()).append("\n");
    }

    // -------------- Auxiliary Functions --------------
    private String decideType(Element element) {
        switch (element.getType().getTypeOfElement()) {
            case INT32:
            case BOOLEAN:
                if (!element.isLiteral())
                    if (method.getLocalVariableByKey(((Operand) element).getName(), null, element.getType()).getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                        jasminCode.append("\n\t\ta").append(JasminUtils.getLoadSize(method.getLocalVariableByKey(((Operand) element).getName(), null, element.getType()).getVirtualReg()));
                        Operand indexOp = (Operand) ((ArrayOperand) element).getIndexOperands().get(0);
                        jasminCode.append("\n\t\ti").append(JasminUtils.getLoadSize(method.getLocalVariableByKey(indexOp.getName(), null, indexOp.getType()).getVirtualReg()));
                        return "\n\t\ti";
                    }
                jasminCode.append("\n\t\ti");
                break;
            case ARRAYREF:
            case THIS:
            case OBJECTREF:
                jasminCode.append("\n\t\ta");
                break;
            default:
                jasminCode.append("\n\t\t");
                break;
        }
        return null;
    }

    private void constOrLoad(Element element, VarScope varScope){
        if (element.isLiteral()) {
            String value = ((LiteralElement) element).getLiteral();
            jasminCode.append(JasminUtils.getConstSize(value));
        }
        else loadOrAload(element, varScope);
    }

    private void storeOrIastore(Element element) {
        String type = decideType(element);
        if (type == null)
            jasminCode.append(JasminUtils.getStoreSize(method.getLocalVariableByKey(((Operand) element).getName(), VarScope.LOCAL, element.getType()).getVirtualReg()));
        else {
            jasminCode.append(type).append(JasminUtils.getLoadSize(method.getLocalVariableByKey(((Operand) element).getName(), VarScope.LOCAL, element.getType()).getVirtualReg()));
            jasminCode.append(type).append("astore");
        }
        jasminCode.append("\n");
    }

    private void loadOrAload( Element element, VarScope varScope) {
        String type = decideType(element);
        if (type == null)
            jasminCode.append(JasminUtils.getLoadSize(method.getLocalVariableByKey(((Operand) element).getName(), varScope, element.getType()).getVirtualReg()));
        else
            jasminCode.append(type).append("aload");
    }

    private void invokeParameters(CallInstruction instruction) {
        if(instruction.getNumOperands() > 1){
            Element secondArg = instruction.getSecondArg();
            if(secondArg.isLiteral()){
                jasminCode.append(".");
                jasminCode.append(((LiteralElement) secondArg).getLiteral().replace("\"", ""));
                jasminCode.append("(");
                for (Element parameter : instruction.getListOfOperands()){
                    jasminCode.append(JasminUtils.getReturnFromMethod(method.getMethod(),parameter.getType()));
                }
                jasminCode.append(")");
                jasminCode.append(JasminUtils.getReturnFromMethod(method.getMethod(),instruction.getReturnType()));
            }
        }
    }

    private boolean sameOperand(Element first, Element second) {
        if (first.isLiteral() || second.isLiteral())
            return false;
        return (((Operand) first).getName().equals(((Operand) second).getName()));
    }

    private boolean iincInstruction(Element leftElement, Element rightElement, Element dest, OperationType operation) {
        String literal;
        if ((operation.toString().equals("ADD") || operation.toString().equals("SUB"))) {
            if (sameOperand(dest, leftElement) && rightElement.isLiteral()) literal = ((LiteralElement) rightElement).getLiteral();
            else if (sameOperand(dest, rightElement) && leftElement.isLiteral()) literal = ((LiteralElement) leftElement).getLiteral();
            else return false;
            Descriptor var = method.getLocalVariableByKey(((Operand) dest).getName(), null, dest.getType());
            if (var.getVarType().getTypeOfElement() != ElementType.ARRAYREF) {
                jasminCode.append("\n\t\tiinc ").append(var.getVirtualReg());
                if ((operation.toString().equals("ADD")))
                    jasminCode.append(" ").append(literal);
                else
                    jasminCode.append(" -").append(literal);
                return true;
            }
        }
        return false;
    }
}
