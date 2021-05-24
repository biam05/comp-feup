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

    public void addCode(String code){
        jasminCode.append(code);
    }

    public void generateJasminCode() {
        instruction.show();
        switch (instruction.getInstType()) {
            case ASSIGN:
                generateAssign((AssignInstruction) instruction);
                break;
            case CALL:
                generateCall((CallInstruction) instruction, false);
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
            case UNARYOPER:
                generateAssignNot(instruction);
                break;
        }
    }

    private void generateAssignNOper(AssignInstruction instruction){
        Instruction rhs = instruction.getRhs();

        Element singleOperand = ((SingleOpInstruction) rhs).getSingleOperand();
        Element dest = instruction.getDest();
        Descriptor localVariable = method.getLocalVariableByKey(dest, null);

        if (dest.getType().getTypeOfElement() == ElementType.INT32 && !dest.isLiteral()) {
            if (localVariable.getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                addCode("\n\t\ta" + JasminUtils.getLoadSize(method, dest, null));
                Element indexElem = ((ArrayOperand) dest).getIndexOperands().get(0);
                addCode("\n\t\ti" + JasminUtils.getLoadSize(method, indexElem, null));
                constOrLoad(singleOperand, null);
                addCode("\n\t\tiastore\n");
                method.decN_stack();
                return;
            }
        }
        constOrLoad(singleOperand, null);
        decideType(instruction.getDest());
        addCode(JasminUtils.getStoreSize(method, dest, VarScope.LOCAL) + "\n");
    }

    private void generateAssignNot(AssignInstruction instruction) {
        Instruction rhs = instruction.getRhs();

        Element singleOperand = ((UnaryOpInstruction) rhs).getRightOperand();
        Element dest = instruction.getDest();

        constOrLoad(singleOperand, null);
        addCode("\n\t\tineg");
        decideType(instruction.getDest());
        addCode(JasminUtils.getStoreSize(method, dest, VarScope.LOCAL) + "\n");
    }

    private void generateAssignBinaryOper(AssignInstruction instruction){
        Instruction rhs = instruction.getRhs();

        OperationType operation = ((BinaryOpInstruction) rhs).getUnaryOperation().getOpType();
        Element leftElement = ((BinaryOpInstruction) rhs).getLeftOperand();
        Element rightElement = ((BinaryOpInstruction) rhs).getRightOperand();

        if (iincInstruction(leftElement, rightElement, instruction.getDest(), operation)) return;

        Element element = instruction.getDest();
        if (element.getType().getTypeOfElement() == ElementType.INT32 && !element.isLiteral()) {
            if (method.getLocalVariableByKey(element, null).getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                addCode("\n\t\ta" + JasminUtils.getLoadSize(method, element, null));
                Element indexElem = ((ArrayOperand) element).getIndexOperands().get(0);
                addCode("\n\t\ti" + JasminUtils.getLoadSize(method, indexElem, null));

                constOrLoad(leftElement, VarScope.LOCAL);
                constOrLoad(rightElement, VarScope.LOCAL);

                method.decN_stack();
                method.decN_stack();
                decideType(leftElement);
                if (operation.toString().equals("ANDB"))
                    addCode("and");
                else
                    addCode(operation.toString().toLowerCase(Locale.ROOT));
                method.incN_stack();

                addCode("\n\t\tiastore\n");
                method.decN_stack();
                method.decN_stack();
                method.decN_stack();
                return;
            }
        }
        constOrLoad(leftElement, VarScope.LOCAL);

        constOrLoad(rightElement, VarScope.LOCAL);

        method.decN_stack();
        method.decN_stack();
        if (operation.toString().equals("LTH")) getLessThanOperation(instruction);
        else {
            decideType(leftElement);
            if (operation.toString().equals("ANDB"))
                addCode("and");
            else
                addCode(operation.toString().toLowerCase(Locale.ROOT));
            method.incN_stack();
            storeOrIastore(element);
        }
    }

    private void getLessThanOperation(AssignInstruction instruction){
        addCode("\n\n\t\tif_icmpge ElseLTH" + method.getN_branches() + JasminUtils.getConstSize(method, "1"));
        storeOrIastore(instruction.getDest());
        addCode("\n\t\tgoto AfterLTH" + method.getN_branches());

        addCode("\n\n\tElseLTH" + method.getN_branches() + ":" + JasminUtils.getConstSize(method, "0"));
        storeOrIastore(instruction.getDest());
        addCode("\n\n\tAfterLTH" + method.getN_branches() + ":");
        method.incN_branches();
    }

    private void generateAssignGetfield(AssignInstruction instruction){
        Instruction rhs = instruction.getRhs();
        generateGetField((GetFieldInstruction) rhs);
        storeOrIastore(instruction.getDest());
    }

    private void generateAssignCall(AssignInstruction instruction){
        Instruction rhs = instruction.getRhs();
        Element element = instruction.getDest();
        if (element.getType().getTypeOfElement() == ElementType.INT32 && !element.isLiteral()) {
            if (method.getLocalVariableByKey(element, null).getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                addCode("\n\t\ta" + JasminUtils.getLoadSize(method, element, null));
                Element indexElem = ((ArrayOperand) element).getIndexOperands().get(0);
                addCode("\n\t\ti" + JasminUtils.getLoadSize(method, indexElem, null));

                generateCall((CallInstruction) rhs, true);
                Element firstArg = ((CallInstruction) rhs).getFirstArg();
                Operand opFirstArg = (Operand) firstArg;
                if(firstArg.getType().getTypeOfElement() == ElementType.OBJECTREF &&
                        opFirstArg.getName().equals(method.getClassName())) {
                    addCode("\n\t\tinvokespecial " + method.getClassName() + ".<init>()V");
                }

                addCode("\n\t\tiastore\n");
                method.decN_stack();
                method.decN_stack();
                method.decN_stack();
                return;
            }
        }
        generateCall((CallInstruction) rhs, true);
        Element firstArg = ((CallInstruction) rhs).getFirstArg();
        Operand opFirstArg = (Operand) firstArg;
        if(firstArg.getType().getTypeOfElement() == ElementType.OBJECTREF &&
                opFirstArg.getName().equals(method.getClassName())) {
            addCode("\n\t\tinvokespecial " + method.getClassName() + ".<init>()V");
        }
        storeOrIastore(instruction.getDest());
    }

    // -------------- Call Instructions --------------
    private void generateCall(CallInstruction instruction, boolean assign) {
        if(method.getMethod().isConstructMethod()){
            addCode("\n\taload_0\n\tinvokespecial java/lang/Object.<init>()V\n\treturn");
        }
        else{
            Element firstArg = instruction.getFirstArg();
            Operand opFirstArg = (Operand)firstArg;

            if(firstArg.getType().getTypeOfElement() == ElementType.OBJECTREF) {
                generateCallObjectRef(instruction, assign);
                if (!JasminUtils.getReturnFromMethod(method, instruction.getReturnType()).equals("V") && !assign)
                    generatePop();
            }

            else if (opFirstArg.getType().getTypeOfElement() == ElementType.ARRAYREF){

                if (instruction.getInvocationType() == CallType.NEW) generateNewArray(instruction);

                else if (instruction.getInvocationType() == CallType.arraylength){
                    decideType(firstArg);
                    addCode(JasminUtils.getLoadSize(method, firstArg, null) + "\n\t\tarraylength");
                }
                if (!assign)
                    generatePop();
            }

            else if (opFirstArg.getType().getTypeOfElement() == ElementType.CLASS) {
                generateStaticMethod(instruction);
            }

            else if(opFirstArg.getName().equals("this")) {
                generateClassMethod(instruction);
                if (!assign)
                    generatePop();
            }
        }
    }

    private void generatePop() {
        addCode("\n\t\tpop");
        method.decN_stack();
    }

    private void generateCallObjectRef(CallInstruction instruction, boolean assign){
        Element firstArg = instruction.getFirstArg();
        Operand opFirstArg = (Operand)firstArg;

        if (opFirstArg.getName().equals(method.getClassName()))
        {
            addCode("\n\t\tnew " + method.getClassName() + "\n\t\tdup");
            method.incN_stack();
        }

        else {
            if (instruction.getNumOperands() > 1) {
                Element secondArg = instruction.getSecondArg();
                if (secondArg.isLiteral())
                    if (((LiteralElement) secondArg).getLiteral().replace("\"", "").equals("<init>"))
                    {
                        method.incN_stack();
                        return;
                    }
            }

            loadOrAload(opFirstArg, null);
            virtualParameters(instruction);
            method.decN_stack();
        }
    }

    private void generateNewArray(CallInstruction instruction){
        Element element = instruction.getListOfOperands().get(0);
        if (element.isLiteral())
            addCode(JasminUtils.getConstSize(method, ((LiteralElement) element).getLiteral()));
        else {
            loadOrAload(element, null);
        }
        addCode("\n\t\tnewarray int");
        //Increase for newarray, decrease for length of the array
    }

    private void generateStaticMethod(CallInstruction instruction){
        Element firstArg = instruction.getFirstArg();
        Operand opFirstArg = (Operand)firstArg;
        for (Element parameter : instruction.getListOfOperands()) {
            if (!parameter.isLiteral())
                loadOrAload(parameter, VarScope.LOCAL);
            else
                addCode(JasminUtils.getConstSize(method, ((LiteralElement) parameter).getLiteral()));
        }
        addCode("\n\t\tinvokestatic " + opFirstArg.getName());
        invokeParameters(instruction);
        for (Element element : instruction.getListOfOperands()) {
            method.decN_stack();
        }
    }

    private void generateClassMethod(CallInstruction instruction){
        addCode("\n\t\taload_0");
        virtualParameters(instruction);
    }

    private void virtualParameters(CallInstruction instruction) {
        for (Element parameter : instruction.getListOfOperands()) {
            if (!parameter.isLiteral())
                loadOrAload(parameter, VarScope.LOCAL);
            else
                addCode(JasminUtils.getConstSize(method, ((LiteralElement) parameter).getLiteral()));
        }
        addCode("\n\t\tinvokevirtual " + method.getClassName());
        invokeParameters(instruction);
        for (Element element : instruction.getListOfOperands()) {
            method.decN_stack();
        }
    }

    // -------------- Return Instruction --------------
    private void generateReturn(ReturnInstruction instruction) {
        Element e1 = instruction.getOperand();
        if (e1 != null) {
            if (!e1.isLiteral()) {
                boolean type = decideType(instruction.getOperand());
                addCode(JasminUtils.getLoadSize(method, e1, null) + "\n\t\t");
                if (!type) decideType(instruction.getOperand());
                addCode("return");
            }
            else {
                String literal = ((LiteralElement) e1).getLiteral();
                addCode(JasminUtils.getConstSize(method, literal) + "\n\t\tireturn");
            }
            method.decN_stack();
        } else {
            addCode("\n\t\treturn");
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

        method.decN_stack();
        method.decN_stack();
        addCode("\n\t\tputfield " + name + "/" + o2.getName() + " " + JasminUtils.getReturnFromMethod(method, e2.getType()) + "\n");

    }

    // -------------- Getfield Instructions --------------
    private void generateGetField(GetFieldInstruction instruction) {
        String firstName = "";
        Element e1 = instruction.getFirstOperand();
        if (!e1.isLiteral()) {
            firstName = ((Operand) e1).getName();
            addCode("\n\t\ta" + JasminUtils.getLoadSize(method, e1, VarScope.FIELD));
        }

        if (firstName.equals("this")) firstName = method.getClassName();
        addCode("\n\t\tgetfield " + firstName + "/");
        e1 = instruction.getSecondOperand();

        if (!e1.isLiteral()) {
            Operand o1 = (Operand) e1;
            addCode(o1.getName() + " " + JasminUtils.getReturnFromMethod(method, o1.getType()));
        }

    }

    // -------------- Branch Instructions --------------
    private void generateBranch(CondBranchInstruction instruction) {
        constOrLoad(instruction.getLeftOperand(),null);
        constOrLoad(instruction.getRightOperand(),null);

        OperationType conditionType = instruction.getCondOperation().getOpType();
        switch (conditionType) {
            case EQ:
                addCode("\n\t\tif_icmpeq ");
                break;
            case LTH:
                addCode("\n\t\tif_icmplt ");
                break;
            case ANDB:
                addCode("\n\t\tiandb\n\t\ticonst_1\n\t\tif_icmpeq ");
                break;
            default:
                break;
        }
        method.decN_stack();
        method.decN_stack();
        addCode(instruction.getLabel());
    }

    private void generateGoto(GotoInstruction instruction) {
        addCode("\n\t\tgoto " + instruction.getLabel() + "\n");
    }

    // -------------- Auxiliary Functions --------------
    private boolean decideType(Element element) {
        switch (element.getType().getTypeOfElement()) {
            case INT32:
            case BOOLEAN:
                if (!element.isLiteral())
                    if (method.getLocalVariableByKey(element, null).getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                        addCode("\n\t\ta" + JasminUtils.getLoadSize(method, element, null));
                        Element indexElem = ((ArrayOperand) element).getIndexOperands().get(0);
                        addCode("\n\t\ti" + JasminUtils.getLoadSize(method, indexElem, null));
                        return true;
                    }
                addCode("\n\t\ti");
                break;
            case ARRAYREF:
            case THIS:
            case OBJECTREF:
                addCode("\n\t\ta");
                break;
            default:
                addCode("\n\t\t");
                break;
        }
        return false;
    }

    private void constOrLoad(Element element, VarScope varScope){
        if (element.isLiteral()) {
            String value = ((LiteralElement) element).getLiteral();
            addCode(JasminUtils.getConstSize(method, value));
        }
        else loadOrAload(element, varScope);
    }

    private void storeOrIastore(Element element) {
        boolean type = decideType(element);
        if (!type)
            addCode(JasminUtils.getStoreSize(method, element, VarScope.LOCAL) + "\n");
        else {
            addCode("\n\t\tiastore\n");
            method.decN_stack();
            method.decN_stack();
            method.decN_stack();
        }
    }

    private void loadOrAload(Element element, VarScope varScope) {
        boolean type = decideType(element);
        if (!type)
            addCode(JasminUtils.getLoadSize(method, element, varScope));
        else {
            method.decN_stack();
            method.decN_stack();
            addCode("\n\t\tiaload");
            method.incN_stack();
        }
    }

    private void invokeParameters(CallInstruction instruction) {
        if(instruction.getNumOperands() > 1){
            Element secondArg = instruction.getSecondArg();
            if(secondArg.isLiteral()){
                addCode("." + ((LiteralElement) secondArg).getLiteral().replace("\"", "") + "(");
                for (Element parameter : instruction.getListOfOperands()){
                    addCode(JasminUtils.getReturnFromMethod(method, parameter.getType()));
                }
                String ret = JasminUtils.getReturnFromMethod(method, instruction.getReturnType());
                addCode(")" + ret);
                if (!ret.equals("V"))
                    method.incN_stack();
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
            if (sameOperand(dest, leftElement) && rightElement.isLiteral())
                literal = ((LiteralElement) rightElement).getLiteral();
            else if (sameOperand(dest, rightElement) && leftElement.isLiteral())
                literal = ((LiteralElement) leftElement).getLiteral();
            else return false;
            Descriptor var = method.getLocalVariableByKey(dest, null);
            if (var.getVarType().getTypeOfElement() != ElementType.ARRAYREF) {
                addCode("\n\t\tiinc " + var.getVirtualReg());
                if ((operation.toString().equals("ADD")))
                    addCode(" " + literal);
                else
                    addCode(" -" + literal);
                return true;
            }
        }
        return false;
    }
}
