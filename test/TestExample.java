import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class TestExample {

    @Test
    public void testExpression() {
        String jmmCode = SpecsIo.getResource("fixtures/public/" + "Simple.jmm");
        TestUtils.analyse(jmmCode);
    }
}
