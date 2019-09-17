package org.zalando.zmon.service.impl;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.zmon.domain.ActivityDiff;
import org.zalando.zmon.domain.AlertDefinition;
import org.zalando.zmon.domain.CheckDefinition;
import org.zalando.zmon.domain.CheckDefinitionImport;
import org.zalando.zmon.generator.AlertDefinitionGenerator;
import org.zalando.zmon.generator.CheckDefinitionImportGenerator;
import org.zalando.zmon.generator.DataGenerator;
import org.zalando.zmon.service.AlertService;
import org.zalando.zmon.service.HistoryService;
import org.zalando.zmon.service.ZMonService;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@DirtiesContext
public class HistoryServiceImplIT {

    @Autowired
    private HistoryService historyService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private ZMonService service;

    private DataGenerator<CheckDefinitionImport> checkImportGenerator;
    private DataGenerator<AlertDefinition> alertGenerator;

    private static final String USER_NAME ="default_user";
    private static final List<String> USER_TEAMS = Arrays.asList("Platform/Software");

    private CheckDefinition createNewCheckDefinition() {
        return service.createOrUpdateCheckDefinition(checkImportGenerator.generate(), USER_NAME, USER_TEAMS).getEntity();
    }

    @Before
    public void setup() {
        checkImportGenerator = new CheckDefinitionImportGenerator();
        alertGenerator = new AlertDefinitionGenerator();
    }

    @Test
    public void testGetCheckDefinitionHistory() throws Exception {

        final CheckDefinition newCheckDefinition = createNewCheckDefinition();

        // TODO test history pagination
        // TODO improve this test: check each field
        final List<ActivityDiff> history = historyService.getCheckDefinitionHistory(newCheckDefinition.getId(), 10,
                null, null);

        MatcherAssert.assertThat(history, Matchers.hasSize(1));
    }

    @Test
    public void testGetAlertDefinitionHistory() throws Exception {

        final CheckDefinition newCheckDefinition = createNewCheckDefinition();

        AlertDefinition newAlertDefinition = alertGenerator.generate();
        newAlertDefinition.setCheckDefinitionId(newCheckDefinition.getId());
        newAlertDefinition = alertService.createOrUpdateAlertDefinition(newAlertDefinition);

        // TODO test history pagination
        // TODO improve this test: check each field
        final List<ActivityDiff> history = historyService.getAlertDefinitionHistory(newAlertDefinition.getId(), 10,
                null, null);

        MatcherAssert.assertThat(history, Matchers.hasSize(1));
    }
}
