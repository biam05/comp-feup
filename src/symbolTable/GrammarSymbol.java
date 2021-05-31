package symbolTable;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;

public class GrammarSymbol extends Symbol {

    public GrammarSymbol(GrammarType type, String name) {
        super(type, name);
    }

    public GrammarSymbol(JmmNode type, JmmNode name) {
        super(new GrammarType(type), name.getKind().replaceAll("'", "").replace("Identifier ", ""));
    }

    public GrammarType getGrammarType() {
        return (GrammarType) getType();
    }

    public String toOLLIR() {
        return getName() + getGrammarType().toOLLIR();
    }
}
