import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InstructionJasmin {
    private final Instruction instruction;
    private final StringBuilder jasminCode;
    private final List<Report> reports;
    private MethodJasmin method;

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

    public void generateJasminCode(){
        instruction.show();
        switch(instruction.getInstType()){
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
            case GETFIELD:
                generateGetField((GetFieldInstruction) instruction);
                break;
            default:
                break;
        }
    }

    private void generateAssign(AssignInstruction instruction) {
        String value, variable;
        Instruction rhs = instruction.getRhs();
        switch (rhs.getInstType()){
            case NOPER:
                variable = ((Operand)instruction.getDest()).getName();
                Element rhsElement = ((SingleOpInstruction) rhs).getSingleOperand();
                if (rhsElement.isLiteral()){
                    value = ((LiteralElement)rhsElement).getLiteral();
                    decideType(rhsElement);
                    jasminCode.append("const_");
                }
                else {
                    value = method.getLocalVariables().get(((Operand)rhsElement).getName()).toString();
                    decideType(rhsElement);
                    jasminCode.append("load_");
                }
                jasminCode.append(value);
                decideType(rhsElement);
                jasminCode.append("store_");
                if (method.addLocalVariable(variable,method.getN_locals())){
                    jasminCode.append(method.getN_locals());
                    method.incNLocals();
                }
                else{
                    jasminCode.append(method.getLocalVariables().get(variable));
                }
                jasminCode.append("\n");
                break;

            case BINARYOPER:
                variable = ((Operand)instruction.getDest()).getName();
                value = method.getLocalVariables().get(variable).toString();

                OperationType operation = ((BinaryOpInstruction) rhs).getUnaryOperation().getOpType();
                Element leftElement = ((BinaryOpInstruction) rhs).getLeftOperand();
                Element rightElement = ((BinaryOpInstruction) rhs).getRightOperand();

                decideType(leftElement);
                jasminCode.append("load_");
                jasminCode.append(method.getLocalVariables().get(((Operand)leftElement).getName()));

                decideType(instruction.getDest());
                jasminCode.append(operation.toString().toLowerCase(Locale.ROOT));

                decideType(rightElement);
                jasminCode.append("load_");
                jasminCode.append(method.getLocalVariables().get(((Operand)rightElement).getName()));

                decideType(instruction.getDest());
                jasminCode.append("store_");
                jasminCode.append(value);

                jasminCode.append("\n");
                break;
        }
    }

    // invokestatic, invokevirtual, invokespecial
    private void generateCall(CallInstruction instruction) {
        if(method.getMethod().isConstructMethod()){
            jasminCode.append("\n\taload0");
            jasminCode.append("\n\tinvokespecial java/lang/Object.<init>()V");
            jasminCode.append("\n\treturn");
        }
        else{
            Element firstArg = instruction.getFirstArg();
            if(firstArg.isLiteral()){

            } else{ // not literal -> it is a variable (CallInstruction)
                Operand operand = (Operand) firstArg;
                // invokestatic(ioPlus, "printHelloWorld").V;
                if(operand.getType().getTypeOfElement() == ElementType.CLASS){
                    jasminCode.append("\n\t\tinvokestatic(");
                    jasminCode.append(operand.getName());
                    if(instruction.getNumOperands() > 1){
                        if(instruction.getInvocationType() != CallType.NEW){
                            Element secondArg = instruction.getSecondArg();
                            if(secondArg.isLiteral()){
                                jasminCode.append(", ");
                                jasminCode.append(((LiteralElement) secondArg).getLiteral());

                            }
                        }
                    }
                    jasminCode.append(").");
                    jasminCode.append(decideInvokeReturns(instruction.getReturnType()));
                }
            }
        }
    }

    private void generateReturn(ReturnInstruction instruction) {
        if(instruction.getOperand() != null){
            decideType(instruction.getOperand());
            jasminCode.append("load_");
            String returnedVariable = ((Operand) instruction.getOperand()).getName();
            String value = method.getLocalVariables().get(returnedVariable).toString();
            jasminCode.append(value);

            decideType(instruction.getOperand());
            jasminCode.append("return");
        }
        else{
            jasminCode.append("\n\t\treturn");
        }
    }

    private void generatePutField(PutFieldInstruction instruction) {
        // TODO
    }

    private void generateGetField(GetFieldInstruction instruction) {
        // TODO
    }

    private void decideType(Element element){
        switch (element.getType().getTypeOfElement()){
            case INT32 -> jasminCode.append("\n\t\ti");
            case BOOLEAN -> jasminCode.append("\n\t\ti"); // weird... == int? confirm
            case ARRAYREF -> jasminCode.append("\n\t\ta");
            default -> jasminCode.append("\n\t\t");
        }
        // other types of variables
    }

    private String decideInvokeReturns(Type type){
        String returnType = null;
        switch(type.getTypeOfElement()){
            // todo: verificar com o ollir como é o tipo quando retorna uma classe
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
                returnType = method.getClass().getName(); //Todo: probably wrong
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
