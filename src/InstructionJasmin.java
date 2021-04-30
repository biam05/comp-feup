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
        System.out.println("Instruction Name: " + instruction.getInstType());
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

        System.out.println("------------------------------------------");
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

    private void generateCall(CallInstruction instruction) {
        System.out.println("\t* Num Operands: " + instruction.getNumOperands());
        System.out.println("\t* Return Type: " + instruction.getReturnType().getTypeOfElement());
        System.out.println("\t* First Arg: " + instruction.getFirstArg());
        System.out.println("\t* Second Arg: " + instruction.getSecondArg());

        if(method.getMethod().isConstructMethod()){
            jasminCode.append("\n\taload_0");
            jasminCode.append("\n\tinvokespecial java/lang/Object.<init>()V;");
        }
        switch (instruction.getReturnType().getTypeOfElement()){
            default:
                jasminCode.append("\n\treturn");
                break;
        }
    }

    private void generateReturn(ReturnInstruction instruction) {
        decideType(instruction.getOperand());
        jasminCode.append("load_");
        String returnedVariable = ((Operand) instruction.getOperand()).getName();
        String value = method.getLocalVariables().get(returnedVariable).toString();
        jasminCode.append(value);

        decideType(instruction.getOperand());
        jasminCode.append("return");
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
}
