import jasmin.JasminMethod;
import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.OllirErrorException;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Copyright 2021 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

public class BackendStage implements JasminBackend {

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit ollirClass = ollirResult.getOllirClass();

        try {
            // Example of what you can do with the OLLIR class
            ollirClass.checkMethodLabels(); // check the use of labels in the OLLIR loaded
            ollirClass.buildCFGs(); // build the CFG of each method
            //ollirClass.outputCFGs(); // output to .dot files the CFGs, one per method
            ollirClass.buildVarTables(); // build the table of variables for each method
            //ollirClass.show(); // print to console main information about the input OLLIR

            // Convert the OLLIR to a String containing the equivalent Jasmin code
            StringBuilder jasminCode = new StringBuilder(); // Convert node ...
            // More reports from this stage
            List<Report> reports = new ArrayList<>();

            jasminCode.append(".class public ").append(ollirClass.getClassName());

            jasminCode.append("\n.super ");

            if (ollirClass.getSuperClass() != null)
                jasminCode.append(ollirClass.getSuperClass());
            else
                jasminCode.append("java/lang/Object");
            jasminCode.append("\n");

            for (var field : ollirClass.getFields()) {
                jasminCode.append("\n.field ");
                if (field.isFinalField())
                    jasminCode.append("final ");
                jasminCode.append("'").append(field.getFieldName()).append("' ");
                switch (field.getFieldType().toString()) {
                    case "INT32":
                        jasminCode.append("I");
                        break;
                    case "BOOLEAN":
                        jasminCode.append("Z");
                        break;
                    case "ARRAYREF":
                        jasminCode.append("[I");
                        break;
                    case "OBJECTREF":
                        jasminCode.append(ollirClass.getClassName());
                    default:
                        break;
                }
            }

            for (var method : ollirClass.getMethods()) {
                System.out.println("METHOD " + method.getMethodName());
                JasminMethod jasminMethod = new JasminMethod(method, ollirClass.getClassName(), ollirClass.getSuperClass());
                jasminMethod.generateJasminCode();
                jasminCode.append(jasminMethod.getJasminCode());
                reports.addAll(jasminMethod.getReports());
            }

            System.out.println("\n------------------- JASMIN CODE -------------------");
            System.out.println(jasminCode);
            System.out.println("---------------------------------------------------");

            return new JasminResult(ollirResult, jasminCode.toString(), reports);

        } catch (OllirErrorException e) {
            return new JasminResult(ollirClass.getClassName(), null,
                    Collections.singletonList(Report.newError(Stage.GENERATION, -1, -1, "Exception during Jasmin generation", e)));
        }

    }

}
