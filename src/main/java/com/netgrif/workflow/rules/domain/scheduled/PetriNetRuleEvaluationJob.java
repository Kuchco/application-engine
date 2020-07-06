package com.netgrif.workflow.rules.domain.scheduled;

import com.netgrif.workflow.petrinet.domain.PetriNet;
import com.netgrif.workflow.petrinet.service.interfaces.IPetriNetService;
import com.netgrif.workflow.rules.domain.facts.ScheduledRuleFact;
import com.netgrif.workflow.rules.service.interfaces.IRuleEngine;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class PetriNetRuleEvaluationJob extends RuleJob {

    public static final String NET_ID = "netId";

    private static final Logger log = LoggerFactory.getLogger(PetriNetRuleEvaluationJob.class);

    @Autowired
    private IRuleEngine ruleEngine;

    @Autowired
    private IPetriNetService petriNetService;

    public void execute(JobExecutionContext context) throws JobExecutionException {
        String netId = getInstanceId(context);
        String ruleIdentifier = getRuleIdentifier(context);

        if (!validate(context)) {
            log.warn("Job does not have caseId or ruleIdentifier! " + netId + ", " + ruleIdentifier);
            return;
        }

        log.info("Executing PetriNetRuleEvaluationJob for net " + netId + " of rule " + ruleIdentifier);

        try {
            PetriNet net = petriNetService.getPetriNet(netId);
            ruleEngine.evaluateRules(net, new ScheduledRuleFact(netId, ruleIdentifier));
        } catch (Exception e) {
            log.error("Failed scheduled rule evaluation", e);
            throw new JobExecutionException(e);
        }

    }

    @Override
    public String getInstanceId(JobExecutionContext context) {
        return (String) context.getJobDetail().getJobDataMap().get(NET_ID);
    }

}