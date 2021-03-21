import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class PassingTests {
    @Test
    public void HelloWorld() {
        System.out.println("--- Printing Hello World Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/HelloWorld.jmm");
        TestUtils.parse(jmmCode);
        System.out.println("-----------------------------\n");
    }

    @Test
    public void FindMaximum() {
        System.out.println("--- Printing Find Maximum Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/FindMaximum.jmm");
        TestUtils.parse(jmmCode);
        System.out.println("----------------------------------\n");
    }

    @Test
    public void LazySort() {
        System.out.println("--- Printing Lazy Sort Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/Lazysort.jmm");
        TestUtils.parse(jmmCode);
        System.out.println("-------------------------------\n");
    }

    @Test
    public void Life() {
        System.out.println("--- Printing Life Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/Life.jmm");
        TestUtils.parse(jmmCode);
        System.out.println("--------------------------\n");
    }

    @Test
    public void MonteCarloPi() {
        System.out.println("--- Printing Monte Carlo Pi Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm");
        TestUtils.parse(jmmCode);
        System.out.println("------------------------------------\n");
    }

    @Test
    public void QuickSort() {
        System.out.println("--- Printing Quick Sort Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/QuickSort.jmm");
        TestUtils.parse(jmmCode);
        System.out.println("--------------------------------\n");
    }

    @Test
    public void Simple() {
        System.out.println("--- Printing Simple Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/Simple.jmm");
        TestUtils.parse(jmmCode);
        System.out.println("-----------------------------\n");
    }

    @Test
    public void TicTacToe() {
        System.out.println("--- Printing TicTacToe Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/TicTacToe.jmm");
        TestUtils.parse(jmmCode);
        System.out.println("-----------------------------\n");
    }

    @Test
    public void While() {
        System.out.println("--- Printing While Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/WhileAndIF.jmm");
        TestUtils.parse(jmmCode);
        System.out.println("-----------------------------\n");
    }
}


