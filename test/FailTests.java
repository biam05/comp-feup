 import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class FailTests  {

    @Test
    public void BlowUp() {
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/BlowUp.jmm");
        TestUtils.parse(jmmCode);
    }

    @Test
    public void CompleteWhile() {
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/CompleteWhileTest.jmm");
        TestUtils.parse(jmmCode);
    }

    @Test
    public void LengthError() {
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/LengthError.jmm");
        TestUtils.parse(jmmCode);
    }

    @Test
    public void MissingRightPar() {
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/MissingRightPar.jmm");
        TestUtils.parse(jmmCode);
    }

    @Test
    public void MultipleSequential() {
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/MultipleSequential.jmm");
        TestUtils.parse(jmmCode);
    }

    @Test
    public void NestedLoop() {
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/syntactical/NestedLoop.jmm");
        TestUtils.parse(jmmCode);
    }
}
