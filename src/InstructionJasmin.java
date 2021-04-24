import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class InstructionJasmin {
    private final Instruction instruction;
    private final StringBuilder jasminCode;
    private final List<Report> reports;

    public InstructionJasmin(Instruction instruction) {
        this.instruction = instruction;
        this.jasminCode = new StringBuilder();
        this.reports = new ArrayList<>();
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
            case GOTO:
                generateGoTo((GotoInstruction) instruction);
                break;
            case BRANCH:
                generateBranch((CondBranchInstruction) instruction);
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
            case UNARYOPER:
                generateUnaryOp((UnaryOpInstruction) instruction);
                break;
            case BINARYOPER:
                generateBinaryOp((BinaryOpInstruction) instruction);
                break;
            case NOPER:
                generateNOper((SingleOpInstruction) instruction); //TODO: ??? not sure
                break;
            default:
                break;
        }

        System.out.println("------------------------------------------");
    }

    // --------------- Deal With Instructions Functions ---------------
    private void generateAssign(AssignInstruction instruction) {
        // TODO
    }

    private void generateCall(CallInstruction instruction) {
        System.out.println("\t* Num Operands: " + instruction.getNumOperands());
        System.out.println("\t* Return Type: " + instruction.getReturnType().getTypeOfElement());
        System.out.println("\t* First Arg: " + instruction.getFirstArg());
        System.out.println("\t* Second Arg: " + instruction.getSecondArg());

        jasminCode.append("\n\taload_0");
        jasminCode.append("\n\tinvokespecial java/lang/Object.<init>()V;");
        switch (instruction.getReturnType().getTypeOfElement()){
            default:
                jasminCode.append("\n\treturn");
                break;
        }
        // TODO: Can a Call Instruction be something different from a Constructor?
    }

    private void generateGoTo(GotoInstruction instruction) {
        // TODO
        jasminCode.append("\n\tLoop:");
        jasminCode.append("\n\t\tgoto Loop");
    }

    private void generateBranch(CondBranchInstruction instruction) {
        // TODO
    }

    private void generateReturn(ReturnInstruction instruction) {
        // TODO: falta a variavel q é retornada. Onde arranjo isso?
        System.out.println("\t* Operand: " + instruction.getOperand());
        System.out.println(instruction.getOperand().isLiteral());
        jasminCode.append("\n\tEnd:");
        switch (instruction.getOperand().getType().getTypeOfElement()) {
            case INT32 -> jasminCode.append("\n\t\tireturn");
            case BOOLEAN -> jasminCode.append("\n\t\tireturn"); // weird... == int? confirm
            case ARRAYREF -> jasminCode.append("\n\t\tareturn");
            default -> jasminCode.append("\n\t\treturn");
        }
    }

    private void generatePutField(PutFieldInstruction instruction) {
        // TODO
    }

    private void generateGetField(GetFieldInstruction instruction) {
        // TODO
    }

    private void generateUnaryOp(UnaryOpInstruction instruction) {
        // TODO
    }

    private void generateBinaryOp(BinaryOpInstruction instruction) {
        // TODO
    }

    private void generateNOper(SingleOpInstruction instruction) {
        // TODO
    }
}
