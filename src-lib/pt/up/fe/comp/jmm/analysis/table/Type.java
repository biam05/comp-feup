package pt.up.fe.comp.jmm.analysis.table;

import pt.up.fe.comp.jmm.JmmNode;

import java.util.List;

public class Type {
    private String name;
    private boolean isArray;

    public Type(String name, boolean isArray) {
        this.name = name;
        this.isArray = isArray;
    }

    public Type(JmmNode node) {
        if (node.getKind().equals("Type")) parseType(node);
        else {
            this.isArray = node.getKind().contains("Array");
            this.name = node.getKind().replace("Array", "");
        }
        this.name = this.name.replaceAll("'", "").replace("Identifier ", "");
    }

    public void parseType(JmmNode node) {
        List<JmmNode> children = node.getChildren();
        this.name = children.get(0).getKind();
        this.isArray = (children.size() == 2) && (children.get(1).getKind().equals("Array"));
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

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (isArray ? 1231 : 1237);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Type other = (Type) obj;
        if (isArray != other.isArray)
            return false;
        if (name == null) {
            return other.name == null;
        } else return name.equals(other.name);
    }

}
