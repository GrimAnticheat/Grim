package ac.grim.legacyac.prediction;

import java.util.Collections;
import java.util.List;

public final class PredictionEvaluation {
    private final List<CandidateVelocity> sortedCandidates;
    private final CandidateVelocity bestCandidate;
    private final double rawOffset;
    private final double reducedOffset;

    public PredictionEvaluation(List<CandidateVelocity> sortedCandidates, CandidateVelocity bestCandidate,
            double rawOffset, double reducedOffset) {
        this.sortedCandidates = sortedCandidates;
        this.bestCandidate = bestCandidate;
        this.rawOffset = rawOffset;
        this.reducedOffset = reducedOffset;
    }

    public List<CandidateVelocity> getSortedCandidates() {
        return Collections.unmodifiableList(sortedCandidates);
    }

    public CandidateVelocity getBestCandidate() {
        return bestCandidate;
    }

    public double getRawOffset() {
        return rawOffset;
    }

    public double getReducedOffset() {
        return reducedOffset;
    }
}
