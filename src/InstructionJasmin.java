import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class InstructionJasmin {
    private final Instruction instruction;
    private final String jasminCode;
    private final List<Report> reports;

    public InstructionJasmin(Instruction instruction) {
        this.instruction = instruction;
        this.jasminCode = "";
        this.reports = new ArrayList<>();
    }

    public String getJasminCode() {
        return jasminCode;
    }

    public List<Report> getReports() {
        return reports;
    }

    public void generateJasminCode(){
        System.out.println("Instruction Name: " + instruction.getInstType());
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
                generateNOper((SingleOpInstruction) instruction); //????????
                break;
            default:
                break;
        }
    }

    // --------------- Deal With Instructions Functions ---------------
    private void generateAssign(AssignInstruction instruction) {
        // TODO
    }

    private void generateCall(CallInstruction instruction) {
        // TODO
    }

    private void generateGoTo(GotoInstruction instruction) {
        // TODO
    }

    private void generateBranch(CondBranchInstruction instruction) {
        // TODO
    }

    private void generateReturn(ReturnInstruction instruction) {
        // TODO
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
