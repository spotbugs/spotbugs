package de.tobject.findbugs.quickfix.test;

import de.tobject.findbugs.test.AbstractPluginTest;
import de.tobject.findbugs.test.TestScenario;

public class QuickfixMulti extends AbstractPluginTest {

    @Override
    protected TestScenario getTestScenario() {
          return TestScenario.MULTIQUICKFIX;
    }

}
