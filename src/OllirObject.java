import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class OllirObject {

    private final List<String> aboveTemp;
    private final List<String> belowTemp;
    private StringBuilder code;

    public OllirObject(String code) {
        this.code = new StringBuilder(code);
        this.aboveTemp = new ArrayList<>();
        this.belowTemp = new ArrayList<>();
    }

    public String getCode() {
        return code.toString();
    }

    public void setCode(String code) {
        this.code = new StringBuilder(code);
    }

    public String getAboveTemp() {
        return String.join("", aboveTemp);
    }

    public String getBelowTemp() {
        return String.join("", belowTemp);
    }

    public List<String> getAboveList() {
        return aboveTemp;
    }

    public List<String> getBelowList() {
        return belowTemp;
    }

    public String toString() {
        return getAboveTemp() + ", " + code + ", " + getBelowTemp();
    }

    public void appendToCode(OllirObject obj) {
        code.append(obj.getAboveTemp());
        code.append(obj.getCode());
        code.append(obj.getBelowTemp());
    }

    public void appendTemps(OllirObject obj) {
        aboveTemp.addAll(obj.getAboveList());
        belowTemp.addAll(obj.getBelowList());
    }

    public void append(OllirObject obj) {
        appendTemps(obj);
        code.append(obj.getCode());
    }

    public void appendCode(String code) {
        this.code.append(code);
    }

    public void addBelowTemp(String temp) {
        temp = temp.replaceAll(";", "").replaceAll("\n", "");
        temp += ";\n";
        this.belowTemp.add(temp);
    }

    public void addAboveTemp(String temp) {
        temp = temp.replaceAll(";", "").replaceAll("\n", "");
        temp += ";\n";
        this.aboveTemp.add(temp);
    }


    public void getReturn() {
        String type = OLLIRUtils.getReturnTypeExpression(this.code.toString());
        this.code.insert(0, "ret" + type + " ");
        this.code.append(";\n");
    }
}
