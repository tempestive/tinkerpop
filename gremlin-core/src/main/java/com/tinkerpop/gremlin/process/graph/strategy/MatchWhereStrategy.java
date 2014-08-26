package com.tinkerpop.gremlin.process.graph.strategy;

import com.tinkerpop.gremlin.process.Step;
import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.TraversalStrategy;
import com.tinkerpop.gremlin.process.graph.step.filter.WhereStep;
import com.tinkerpop.gremlin.process.graph.step.map.SelectOneStep;
import com.tinkerpop.gremlin.process.graph.step.map.SelectStep;
import com.tinkerpop.gremlin.process.graph.step.map.match.MatchStep;
import com.tinkerpop.gremlin.process.graph.step.util.IdentityStep;
import com.tinkerpop.gremlin.process.util.TraversalHelper;

import java.util.List;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class MatchWhereStrategy implements TraversalStrategy {

    private static final MatchWhereStrategy INSTANCE = new MatchWhereStrategy();

    private MatchWhereStrategy() {
    }

    public void apply(final Traversal traversal) {
        final List<MatchStep> matchSteps = TraversalHelper.getStepsOfClass(MatchStep.class, traversal);
        for (final MatchStep matchStep : matchSteps) {
            Step currentStep = matchStep.getNextStep();
            while (currentStep instanceof WhereStep || currentStep instanceof IdentityStep || currentStep instanceof SelectStep || currentStep instanceof SelectOneStep) {
                if (currentStep instanceof WhereStep) {
                    if (!((WhereStep) currentStep).hasBiPredicate()) {
                        matchStep.addTraversal(((WhereStep) currentStep).constraint);
                        TraversalHelper.removeStep(currentStep, traversal);
                    }
                } else if (currentStep instanceof SelectStep) {
                    if (((SelectStep) currentStep).hasStepFunctions())
                        break;
                } else if (currentStep instanceof SelectOneStep) {
                    if (((SelectOneStep) currentStep).hasStepFunction())
                        break;
                } // else is the identity step

                currentStep = currentStep.getNextStep();
            }
        }
    }

    public static MatchWhereStrategy instance() {
        return INSTANCE;
    }

    public int compareTo(final TraversalStrategy traversalStrategy) {
        return traversalStrategy instanceof IdentityReductionStrategy ? 1 : -1;
    }
}
