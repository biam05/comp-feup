
/**
 * Copyright 2021 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.ollir.OllirUtils;
import pt.up.fe.specs.util.SpecsIo;

import java.io.File;
import java.util.ArrayList;

public class BackendTest {

    @Test
    public void testFindMaximum() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/FindMaximum.jmm"));
        TestUtils.noErrors(result.getReports());
        var output = result.run();
        assertEquals("Result: 28", output.trim());
    }

    @Test
    public void testHelloWorld() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));
        TestUtils.noErrors(result.getReports());
        var output = result.run();
        assertEquals("Hello, World!", output.trim());
    }

    @Test
    public void testHelloWorldAdd() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/HelloWorldAdd.jmm"));
        TestUtils.noErrors(result.getReports());
        var output = result.run();
        assertEquals("4", output.trim());
    }

    @Test
    public void testLazySort() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Lazysort.jmm"));
        TestUtils.noErrors(result.getReports());
        var output = result.run();
    }

    @Test
    public void testLife() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Life.jmm"));
        TestUtils.noErrors(result.getReports());
        var output = result.compile(new File("test/fixtures/libs/compiled"));
    }

    @Test
    public void testMonteCarloPi() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm"));
        TestUtils.noErrors(result.getReports());
        var output = result.compile(new File("test/fixtures/libs/compiled"));
    }

    @Test
    public void testQuickSort() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/QuickSort.jmm"));
        TestUtils.noErrors(result.getReports());
        var output = result.run();
        assertEquals("1\r\n2\r\n3\r\n4\r\n5\r\n6\r\n7\r\n8\r\n9\r\n10", output.trim());
    }

    @Test
    public void testSimple() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Simple.jmm"));
        TestUtils.noErrors(result.getReports());
        var output = result.run();
        assertEquals("30", output.trim());
    }

    @Test
    public void testTicTacToe() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/TicTacToe.jmm"));
        TestUtils.noErrors(result.getReports());
        var output = result.compile(new File("test/fixtures/libs/compiled"));
    }


    @Test
    public void testWhileAndIF() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/WhileAndIF.jmm"));
        TestUtils.noErrors(result.getReports());
        var output = result.run();
        assertEquals("10\r\n10\r\n10\r\n10\r\n10\r\n10\r\n10\r\n10\r\n10\r\n10", output.trim());
    }



    @Test
    public void testExtra1() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Extra1.jmm"));
        TestUtils.noErrors(result.getReports());
        var output = result.run();
        assertEquals("8", output.trim());
    }

    @Test
    public void testExtra2() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Extra2.jmm"));
        TestUtils.noErrors(result.getReports());
        var output = result.run();
    }

    @Test
    public void testExtra3() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Extra3.jmm"));
        TestUtils.noErrors(result.getReports());
        var output = result.run();
    }

    @Test
    public void testExtra4() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Extra4.jmm"));
        TestUtils.noErrors(result.getReports());
        var output = result.run();
    }
    @Test
    public void testExtra5() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Extra5.jmm"));
        TestUtils.noErrors(result.getReports());
        var output = result.run();
    }

}
