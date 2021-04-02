import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;


import java.util.List;

public class GrammarSymbolTable implements SymbolTable {

    private List<String> imports;
    private String className;
    private String superExtends;
    private List<Symbol> classFields;
    private List<SymbolMethod> methods;

    @Override
    public List<String> getImports() {
        return null;
    }

    @Override
    public String getClassName() {
        return null;
    }

    @Override
    public String getSuper() {
        return null;
    }

    @Override
    public List<Symbol> getFields() {
        return null;
    }

    @Override
    public List<String> getMethods() {
        return null;
    }

    @Override
    public Type getReturnType(String methodName) {
        return null;
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        return null;
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        return null;
    }
}
