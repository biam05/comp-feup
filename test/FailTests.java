 import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class FailTests  {

    @Test
    public void BlowUp() {
        System.out.println("--- Printing Blow Up Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/BlowUp.jmm");
        TestUtils.parse(jmmCode);
        System.out.println("-----------------------------\n");
    }

    @Test
    public void CompleteWhile() {
        System.out.println("--- Printing Complete While Test---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/CompleteWhileTest.jmm");
        TestUtils.parse(jmmCode);
        System.out.println("-------------------------------\n");
    }

    @Test
    public void LengthError() {
        System.out.println("--- Printing Length Error Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/LengthError.jmm");
        TestUtils.parse(jmmCode);
        System.out.println("----------------------------------\n");
    }

    @Test
    public void MissingRightPar() {
        System.out.println("--- Printing Missing Right Par Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/MissingRightPar.jmm");
        TestUtils.parse(jmmCode);
        System.out.println("---------------------------------------\n");
    }

    @Test
    public void MultipleSequential() {
        System.out.println("--- Printing Multiple Sequential Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/MultipleSequential.jmm");
        TestUtils.parse(jmmCode);
        System.out.println("-----------------------------------------\n");
    }

    @Test
    public void NestedLoop() {
        System.out.println("--- Printing Nested Loop Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/NestedLoop.jmm");
        TestUtils.parse(jmmCode);
        System.out.println("---------------------------------\n");
    }
}
