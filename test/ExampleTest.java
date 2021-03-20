import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class ExampleTest {
    @Test
    public void HelloWorld() {
        String jmmCode = SpecsIo.getResource("fixtures/public/HelloWorld.jmm");
        TestUtils.parse(jmmCode);
    }

    @Test
    public void FindMaximum() {
        String jmmCode = SpecsIo.getResource("fixtures/public/FindMaximum.jmm");
        TestUtils.parse(jmmCode);
    }

    @Test
    public void LazySort() {
        String jmmCode = SpecsIo.getResource("fixtures/public/Lazysort.jmm");
        TestUtils.parse(jmmCode);
    }

    @Test
    public void Life() {
        String jmmCode = SpecsIo.getResource("fixtures/public/Life.jmm");
        TestUtils.parse(jmmCode);
    }

    @Test
    public void MonteCarloPi() {
        String jmmCode = SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm");
        TestUtils.parse(jmmCode);
    }

    @Test
    public void QuickSort() {
        String jmmCode = SpecsIo.getResource("fixtures/public/QuickSort.jmm");
        TestUtils.parse(jmmCode);
    }

    @Test
    public void Simple() {
        String jmmCode = SpecsIo.getResource("fixtures/public/Simple.jmm");
        TestUtils.parse(jmmCode);
    }

    @Test
    public void TicTacToe() {
        String jmmCode = SpecsIo.getResource("fixtures/public/TicTacToe.jmm");
        TestUtils.parse(jmmCode);
    }

    /*@Test
    public void TicTacToe2() {
        String jmmCode = SpecsIo.getResource("fixtures/public/TicTacToe.input");
        TestUtils.parse(jmmCode);
    }*/
    @Test
    public void While() {
        String jmmCode = SpecsIo.getResource("fixtures/public/WhileAndIF.jmm");
        TestUtils.parse(jmmCode);
    }
    /*@Test
    public void While2() {
        String jmmCode = SpecsIo.getResource("fixtures/public/TicTacToe.input");
        TestUtils.parse(jmmCode);
    }*/


}


