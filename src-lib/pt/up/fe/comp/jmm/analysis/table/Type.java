package pt.up.fe.comp.jmm.analysis.table;

import pt.up.fe.comp.jmm.JmmNode;

import java.awt.image.Kernel;
import java.util.List;

public class Type {
    private String name;
    private boolean isArray;

    public Type(String name, boolean isArray) {
        this.name = name;
        this.isArray = isArray;
    }

    public Type(JmmNode node) {
        if(node.getKind().equals("Type")) parseType(node);
        else {
            this.isArray = node.getKind().contains("[]");
            this.name = node.getKind().replace("[]", "");
        }
        this.name = this.name.replaceAll("'", "").replace("Identifier ", "");
    }

    public void parseType(JmmNode node){
        List<JmmNode> children = node.getChildren();
        this.name = children.get(0).getKind();
        this.isArray = (children.size() == 2) && (children.get(1).getKind().equals("[]"));
    }

    public String getName() {
        return name;
    }

    public boolean isArray() {
        return isArray;
    }

    @Override
    public String toString() {
        return "Type [name=" + name + ", isArray=" + isArray + "]";
    }

}
