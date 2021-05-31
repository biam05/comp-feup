import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.specs.util.SpecsIo;

public class FailSemanticTests{

    @Test
    public void ArrIndexNotInt() {
        System.out.println("--- Printing Arr Index Not Int Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/semantic/arr_index_not_int.jmm");
        var result = TestUtils.parse(jmmCode);
        var analysisresult = TestUtils.analyse(result);
        System.out.println("-----------------------------\n");
    }

    @Test
    public void ArrSizeNotInt() {
        System.out.println("--- Printing Arr Size Not Int Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/semantic/arr_size_not_int.jmm");
        var result = TestUtils.parse(jmmCode);
        var analysisresult = TestUtils.analyse(result);
        System.out.println("-----------------------------\n");
    }

    @Test
    public void BadArguments() {
        System.out.println("--- Printing Bad Arguments Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/semantic/badArguments.jmm");
        var result = TestUtils.parse(jmmCode);
        var analysisresult = TestUtils.analyse(result);
        System.out.println("-----------------------------\n");
    }

    @Test
    public void BinopIncomp() {
        System.out.println("--- Printing Binop Incomp Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/semantic/binop_incomp.jmm");
        var result = TestUtils.parse(jmmCode);
        var analysisresult = TestUtils.analyse(result);
        System.out.println("-----------------------------\n");
    }

    @Test
    public void FuncNotFound() {
        System.out.println("--- Printing Func Not Found Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/semantic/funcNotFound.jmm");
        var result = TestUtils.parse(jmmCode);
        var analysisresult = TestUtils.analyse(result);
        System.out.println("-----------------------------\n");
    }

    @Test
    public void SimpleLength() {
        System.out.println("--- Printing Simple Length Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/semantic/simple_length.jmm");
        var result = TestUtils.parse(jmmCode);
        var analysisresult = TestUtils.analyse(result);
        System.out.println("-----------------------------\n");
    }

    @Test
    public void VarExpIncomp() {
        System.out.println("--- Printing Var Exp Incomp Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/semantic/var_exp_incomp.jmm");
        var result = TestUtils.parse(jmmCode);
        var analysisresult = TestUtils.analyse(result);
        System.out.println("-----------------------------\n");
    }

    @Test
    public void VarLitIncomp() {
        System.out.println("--- Printing Var Lit Incomp Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/semantic/var_lit_incomp.jmm");
        var result = TestUtils.parse(jmmCode);
        var analysisresult = TestUtils.analyse(result);

        System.out.println("-----------------------------\n");
    }

    @Test
    public void VarUndef() {
        System.out.println("--- Printing Var Undef Test ---\n");
        String jmmCode = SpecsIo.getResource("fixtures/public/fail/semantic/var_undef.jmm");
        var result = TestUtils.parse(jmmCode);
        var analysisresult = TestUtils.analyse(result);
        System.out.println("-----------------------------\n");
    }
}
