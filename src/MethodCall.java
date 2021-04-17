import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolMethod;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.List;

public class MethodCall {
    private SymbolMethod method;
    private String varName;
    private String methodName;
    private List<Type> parameters;
    private int line;
    private int col;

    public MethodCall(SymbolMethod method, String varName, String methodName, List<Type> parameters, int line, int col){
        this.method = method;
        this.varName = varName;
        this.methodName = methodName;
        this.parameters = parameters;
        this.line = line;
        this.col = col;
    }

    public SymbolMethod getMethod() {
        return method;
    }

    public String getVarName() {
        return varName;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<Type> getParameters() {
        return parameters;
    }

    public int getLine() {
        return line;
    }

    public int getCol() {
        return col;
    }
}
