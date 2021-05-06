import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InstructionJasmin {
    private final Instruction instruction;
    private final StringBuilder jasminCode;
    private final List<Report> reports;
    private final MethodJasmin method;

    public InstructionJasmin(Instruction instruction, MethodJasmin method) {
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

            default:
                break;
        }
    }

    private void generateAssign(AssignInstruction instruction) {
        String value, variable;
        Instruction rhs = instruction.getRhs();

        switch (rhs.getInstType()) {
            case NOPER:
                variable = ((Operand) instruction.getDest()).getName();

                Element rhsElement = ((SingleOpInstruction) rhs).getSingleOperand();
                if (rhsElement.isLiteral()) {
                    value = ((LiteralElement) rhsElement).getLiteral();
                    decideType(rhsElement);
                    jasminCode.append("const_");
                } else {
                    value = method.getLocalVariableByKey(((Operand) rhsElement).getName()).toString();
                    decideType(rhsElement);
                    jasminCode.append("load_");
                }
                jasminCode.append(value);
                decideType(rhsElement);
                jasminCode.append("store_").append(method.getLocalVariableByKey(variable)).append("\n");

                break;

            case BINARYOPER:
                variable = ((Operand) instruction.getDest()).getName();
                value = method.getLocalVariableByKey(variable).toString();

                OperationType operation = ((BinaryOpInstruction) rhs).getUnaryOperation().getOpType();
                Element leftElement = ((BinaryOpInstruction) rhs).getLeftOperand();
                Element rightElement = ((BinaryOpInstruction) rhs).getRightOperand();

                if (leftElement.isLiteral()) {
                    value = ((LiteralElement) leftElement).getLiteral();
                    decideType(leftElement);
                    jasminCode.append("const_" + value);
                } else {
                    decideType(leftElement);
                    jasminCode.append("load_");
                    jasminCode.append(method.getLocalVariableByKey(((Operand) leftElement).getName()));
                }

                if (rightElement.isLiteral()) {
                    value = ((LiteralElement) rightElement).getLiteral();
                    decideType(rightElement);
                    jasminCode.append("const_" + value);
                } else {
                    decideType(rightElement);
                    jasminCode.append("load_");
                    jasminCode.append(method.getLocalVariableByKey(((Operand) rightElement).getName()));
                }

                decideType(instruction.getDest());
                jasminCode.append(operation.toString().toLowerCase(Locale.ROOT));

                decideType(instruction.getDest());
                jasminCode.append("store_");
                jasminCode.append(value);

                jasminCode.append("\n");
                break;
            case GETFIELD:
                variable = ((Operand) instruction.getDest()).getName();
                generateGetField((GetFieldInstruction) rhs);
                decideType(instruction.getDest());
                jasminCode.append("store_");

                jasminCode.append(method.getLocalVariableByKey(variable));
                break;
            case CALL:
                variable = ((Operand) instruction.getDest()).getName();
                generateCall((CallInstruction) rhs);
                decideType(instruction.getDest());
                jasminCode.append("store_");
                jasminCode.append(method.getLocalVariableByKey(variable)).append("\n");

                break;
        }
    }

    // invokestatic, invokevirtual, invokespecial
    private void generateCall(CallInstruction instruction) {
        if (method.getMethod().isConstructMethod()) {
            jasminCode.append("\n\taload_0");
            jasminCode.append("\n\tinvokespecial java/lang/Object.<init>()V");
            jasminCode.append("\n\treturn");
        } else {
            Element firstArg = instruction.getFirstArg();
            if (firstArg.isLiteral()) {

            } else { // not literal -> it is a variable (CallInstruction)
                Operand operand = (Operand) firstArg;
                // invokestatic(ioPlus, "printHelloWorld").V;
                if (operand.getType().getTypeOfElement() == ElementType.CLASS) {
                    jasminCode.append("\n\t\tinvokestatic ");
                    jasminCode.append(operand.getName());
                    if (instruction.getNumOperands() > 1) {
                        if (instruction.getInvocationType() != CallType.NEW) {
                            Element secondArg = instruction.getSecondArg();
                            if (secondArg.isLiteral()) {
                                jasminCode.append(".");
                                jasminCode.append(((LiteralElement) secondArg).getLiteral().replace("\"", ""));
                                jasminCode.append("(");
                                for (Element parameter : instruction.getListOfOperands()) {
                                    jasminCode.append(decideInvokeReturns(parameter.getType()));
                                }
                                jasminCode.append(")");
                                jasminCode.append(decideInvokeReturns(instruction.getReturnType()));
                            }
                        }
                    }
                } else if ((operand.getType().getTypeOfElement() == ElementType.THIS ||
                        operand.getType().getTypeOfElement() == ElementType.OBJECTREF)) {
                    if (instruction.getNumOperands() > 1) {
                        if (instruction.getInvocationType() == CallType.invokevirtual) {
                            jasminCode.append("\n\t\tinvokevirtual ");
                            Element secondArg = instruction.getSecondArg();
                            if (secondArg.isLiteral()) {
                                jasminCode.append(method.getClassName());
                                jasminCode.append(".");
                                jasminCode.append(((LiteralElement) secondArg).getLiteral().replace("\"", ""));
                                jasminCode.append("(");
                                for (Element parameter : instruction.getListOfOperands()) {
                                    jasminCode.append(decideInvokeReturns(parameter.getType()));
                                }
                                jasminCode.append(")");
                                jasminCode.append(decideInvokeReturns(instruction.getReturnType()));
                            }
                        } else {
                            jasminCode.append("\n\t\tnew ").append(method.getClassName());
                            jasminCode.append("\n\t\tdup");
                            jasminCode.append("\n\t\tinvokespecial <init>()V");
                        }
                    }
                }
            }
        }
    }

    private void generateReturn(ReturnInstruction instruction) {
        if (instruction.getOperand() != null) {
            decideType(instruction.getOperand());
            jasminCode.append("load_");
            String returnedVariable = ((Operand) instruction.getOperand()).getName();
            String value = method.getLocalVariableByKey(returnedVariable).toString();
            jasminCode.append(value);

            decideType(instruction.getOperand());
            jasminCode.append("return");
        } else {
            jasminCode.append("\n\t\treturn");
        }
    }

    private void generatePutField(PutFieldInstruction instruction) {

        Element e1 = instruction.getFirstOperand();
        Element e2 = instruction.getSecondOperand();
        Element e3 = instruction.getThirdOperand();
        Operand o1 = (Operand) e1;
        Operand o2 = (Operand) e2;

        if (e3.isLiteral()) { // if the e1 is not a literal, then it is a variable
            decideType(e3);
            jasminCode.append("const_").append(((LiteralElement) e3).getLiteral());
        } else {
            Operand o3 = (Operand) e3;
            decideType(e3);
            jasminCode.append("load_").append(method.getLocalVariableByKey(o3.getName()));
        }

        decideType(e2);
        jasminCode.append("store_").append(method.getLocalVariableByKey(o2.getName()));

        String name = o1.getName();

        decideType(e1);
        jasminCode.append("load_").append(method.getLocalVariableByKey(name));
        if (name.equals("this")) name = method.getClassName();

        decideType(e2);
        jasminCode.append("load_").append(method.getLocalVariableByKey(o2.getName()));

        jasminCode.append("\n\t\tputfield ").append(name).append("/").append(o2.getName()).append(" ").append(decideInvokeReturns(e2.getType()));

    }

    private void generateGetField(GetFieldInstruction instruction) {
        String firstName = "";
        Element e1 = instruction.getFirstOperand();
        if (!e1.isLiteral()) { // if the e1 is not a literal, then it is a variable
            Operand o1 = (Operand) e1;
            firstName = o1.getName();
            jasminCode.append("\n\t\taload_").append(method.getLocalVariableByKey(o1.getName()));
        }

        if (firstName.equals("this")) firstName = method.getClassName();
        jasminCode.append("\n\t\tgetfield ").append(firstName).append("/");
        e1 = instruction.getSecondOperand();

        if (!e1.isLiteral()) { // if the e1 is not a literal, then it is a variable
            Operand o1 = (Operand) e1;
            jasminCode.append(o1.getName()).append(" ").append(decideInvokeReturns(o1.getType()));
        }

    }

    private void decideType(Element element) {
        switch (element.getType().getTypeOfElement()) {
            case INT32:
                jasminCode.append("\n\t\ti");
                break;
            case BOOLEAN:// weird... == int? confirm
                jasminCode.append("\n\t\ti");
                break;
            case ARRAYREF:
                jasminCode.append("\n\t\ta");
                break;
            case THIS:
                jasminCode.append("\n\t\ta");
                break;
            case OBJECTREF:
                jasminCode.append("\n\t\ta");
                break;
            default:
                jasminCode.append("\n\t\t");
                break;
        }
        // other types of variables
    }

    public String decideInvokeReturns(Type type) {
        String returnType = null;
        switch (type.getTypeOfElement()) {
            case INT32:
                returnType = "I";
                break;
            case BOOLEAN:
                returnType = "Z";
                break;
            case ARRAYREF:
                returnType = "[I";
                break;
            case OBJECTREF:
                returnType = method.getClass().getName();
                break;
            case VOID:
                returnType = "V";
                break;
            default:
                break;
        }
        return returnType;
    }
}
