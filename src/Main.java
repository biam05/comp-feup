import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class Main implements JmmParser {

    public static void main(String[] args) throws IOException {
        System.out.println("Executing with args: " + Arrays.toString(args));
        if (args[0].contains("fail")) {
            throw new RuntimeException("It's supposed to fail");
        }

        File jmmFile = new File(args[0]);

        boolean o_optimization = false;
        if (args.length > 1)
            if (args[1].equals("-o"))
                o_optimization = true;
        String jmm = Files.readString(jmmFile.toPath());

        JmmParserResult parserResult = new Main().parse(jmm);
        JmmSemanticsResult semanticsResult = new AnalysisStage().semanticAnalysis(parserResult);
        OptimizationStage optimization = new OptimizationStage();
        if (o_optimization)
            semanticsResult = optimization.optimize(semanticsResult);
        OllirResult ollirResult = optimization.toOllir(semanticsResult);
        JasminResult jasminResult = new BackendStage().toJasmin(ollirResult);

        Path p = Paths.get(ollirResult.getSymbolTable().getClassName() + "/");

        if (!Files.exists(p)) {
            Files.createDirectory(p);
        }

        try {
            FileWriter writer = new FileWriter(p + "/" + ollirResult.getSymbolTable().getClassName() + ".j");
            writer.write(jasminResult.getJasminCode());
            writer.close();
        } catch (IOException e) {
            System.out.println("Error writing jasmin code");
            e.printStackTrace();
        }

        //AST
        FileWriter ast  = new FileWriter(p + "/" + ollirResult.getSymbolTable().getClassName() + ".json");
        ast.write(parserResult.toJson());
        ast.close();

        // Symbol Table
        FileWriter symbolTable  = new FileWriter(p + "/" + ollirResult.getSymbolTable().getClassName() + ".symbols.txt");
        symbolTable.write(semanticsResult.getSymbolTable().print());
        symbolTable.close();

        // Ollir
        FileWriter ollir  = new FileWriter(p + "/" + ollirResult.getSymbolTable().getClassName() + ".ollir");
        ollir.write(ollirResult.getOllirCode());
        ollir.close();

        // Jasmin
        FileWriter jasmin = new FileWriter(p + "/" + ollirResult.getSymbolTable().getClassName() + ".j");
        jasmin.write(jasminResult.getJasminCode());
        jasmin.close();

        jasminResult.compile(p.toFile());
    }

    public JmmParserResult parse(String jmmCode) {

        try {
            Grammar myGrammar = new Grammar(new StringReader(jmmCode));
            SimpleNode root = myGrammar.Program(); // returns reference to root node

            root.dump(""); // prints the tree on the screen

            return new JmmParserResult(root, new ArrayList<>());
        } catch (ParseException e) {
            throw new RuntimeException("Error while parsing", e);
        }
    }


}