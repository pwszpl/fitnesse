// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.testsystems.slim.tables;

import static org.junit.Assert.assertEquals;
import static util.ListUtility.list;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import fitnesse.slim.SlimClient;
import fitnesse.testsystems.slim.HtmlTableScanner;
import fitnesse.testsystems.slim.MockSlimTestContext;
import fitnesse.testsystems.slim.Table;
import fitnesse.testsystems.slim.TableScanner;
import fitnesse.wiki.InMemoryPage;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageUtil;

public class ScenarioAndDecisionTableTest extends MockSlimTestContext {
  private WikiPage root;
  private List<Object> instructions;
  private ScenarioTable st;
  private DecisionTable dt;

  @Before
  public void setUp() throws Exception {
    root = InMemoryPage.makeRoot("root");
    instructions = new ArrayList<Object>();
  }

  private void makeTables(String tableText) throws Exception {
    WikiPageUtil.setPageContents(root, tableText);
    TableScanner ts = new HtmlTableScanner(root.getData().getHtml());
    Table t = ts.getTable(0);
    st = new ScenarioTable(t, "s_id", this);
    t = ts.getTable(1);
    dt = new DecisionTable(t, "did", this);
    instructions.addAll(st.getInstructions());
    instructions.addAll(dt.getInstructions());
  }

  @Test
  public void bracesArountArgumentInTable() throws Exception {
    makeTables(
      "!|scenario|echo|user|giving|user_old|\n" +
        "|check|echo|@{user}|@{user_old}|\n" +
        "\n" +
        "!|DT:EchoGiving|\n" +
        "|user|user_old|\n" +
        "|7|7|\n"
    );
    Map<String, Object> pseudoResults = SlimClient.resultToMap(
      list(
        list("decisionTable_did_0/scriptTable_s_id_0", "7")
      )
    );
    evaluateExpectations(pseudoResults);

    String scriptTable = dt.getChildren().get(0).getTable().toString();
    String expectedScript =
      "[[scenario, echo, user, giving, user_old], [check, echo, 7, pass(7)]]";
    assertEquals(expectedScript, scriptTable);
    String dtHtml = dt.getTable().toString();
    assertEquals(1, dt.getTestSummary().getRight());
    assertEquals(0, dt.getTestSummary().getWrong());
    assertEquals(0, dt.getTestSummary().getIgnores());
    assertEquals(0, dt.getTestSummary().getExceptions());
  }

  @Test
  public void oneInput() throws Exception {
    makeTables(
      "!|scenario|myScenario|input|\n" +
        "|function|@input|\n" +
        "\n" +
        "!|DT:myScenario|\n" +
        "|input|\n" +
        "|7|\n"
    );
    List<Object> expectedInstructions =
      list(
        list("decisionTable_did_0/scriptTable_s_id_0", "call", "scriptTableActor", "function", "7")
      );
    assertEquals(expectedInstructions, instructions);
  }

  @Test
  public void manyInputsAndRows() throws Exception {
    makeTables(
      "!|scenario|login|user name|password|password|pin|pin|\n" +
        "|login|@userName|with password|@password|and pin|@pin|\n" +
        "\n" +
        "!|DT:LoginPasswordPin|\n" +
        "|user name|password|pin|\n" +
        "|bob|xyzzy|7734|\n" +
        "|bill|yabba|8892|\n"
    );
    List<Object> expectedInstructions =
      list(
        list("decisionTable_did_0/scriptTable_s_id_0", "call", "scriptTableActor", "loginWithPasswordAndPin", "bob", "xyzzy", "7734"),
        list("decisionTable_did_1/scriptTable_s_id_0", "call", "scriptTableActor", "loginWithPasswordAndPin", "bill", "yabba", "8892")
      );
    assertEquals(expectedInstructions, instructions);
  }

  @Test
  public void simpleInputAndOutputPassing() throws Exception {
    makeTables(
      "!|scenario|echo|input|giving|output|\n" +
        "|check|echo|@input|@output|\n" +
        "\n" +
        "!|DT:EchoGiving|\n" +
        "|input|output|\n" +
        "|7|7|\n"
    );
    Map<String, Object> pseudoResults = SlimClient.resultToMap(
      list(
        list("decisionTable_did_0/scriptTable_s_id_0", "7")
      )
    );
    evaluateExpectations(pseudoResults);

    String scriptTable = dt.getChildren().get(0).getTable().toString();
    String expectedScript =
      "[[scenario, echo, input, giving, output], [check, echo, 7, pass(7)]]";
    assertEquals(expectedScript, scriptTable);
    String dtHtml = dt.getTable().toString();
    assertEquals(1, dt.getTestSummary().getRight());
    assertEquals(0, dt.getTestSummary().getWrong());
    assertEquals(0, dt.getTestSummary().getIgnores());
    assertEquals(0, dt.getTestSummary().getExceptions());
  }

  @Test
  public void simpleInputAndOutputFailing() throws Exception {
    makeTables(
      "!|scenario|echo|input|giving|output|\n" +
        "|check|echo|@input|@output|\n" +
        "\n" +
        "!|DT:EchoGiving|\n" +
        "|input|output|\n" +
        "|7|8|\n"
    );
    Map<String, Object> pseudoResults = SlimClient.resultToMap(
      list(
        list("decisionTable_did_0/scriptTable_s_id_0", "7")
      )
    );
    evaluateExpectations(pseudoResults);

    String scriptTable = dt.getChildren().get(0).getTable().toString();
    String expectedScript =
      "[[scenario, echo, input, giving, output], [check, echo, 7, [7] fail(expected [8])]]";
    assertEquals(expectedScript, scriptTable);
    String dtHtml = dt.getTable().toString();
    assertEquals(0, dt.getTestSummary().getRight());
    assertEquals(1, dt.getTestSummary().getWrong());
    assertEquals(0, dt.getTestSummary().getIgnores());
    assertEquals(0, dt.getTestSummary().getExceptions());
  }

  @Test(expected=SyntaxError.class)
  public void scenarioHasTooFewArguments() throws Exception {
    makeTables(
      "!|scenario|echo|input|giving|\n" +
        "|check|echo|@input|@output|\n" +
        "\n" +
        "!|DT:EchoGiving|\n" +
        "|input|output|\n" +
        "|7|8|\n"
    );
  }

  @Test
  public void scenarioHasExtraArgumentsThatAreIgnored() throws Exception {
    makeTables(
      "!|scenario|echo|input|giving|output||output2|\n" +
        "|check|echo|@input|@output|\n" +
        "\n" +
        "!|DT:EchoGiving|\n" +
        "|input|output|\n" +
        "|7|7|\n"
    );
    Map<String, Object> pseudoResults = SlimClient.resultToMap(
      list(
        list("decisionTable_did_0/scriptTable_s_id_0", "7")
      )
    );
    evaluateExpectations(pseudoResults);

    String scriptTable = dt.getChildren().get(0).getTable().toString();
    String expectedScript =
      "[[scenario, echo, input, giving, output, , output2], [check, echo, 7, pass(7)]]";
    assertEquals(expectedScript, scriptTable);
    String dtHtml = dt.getTable().toString();
  }
}