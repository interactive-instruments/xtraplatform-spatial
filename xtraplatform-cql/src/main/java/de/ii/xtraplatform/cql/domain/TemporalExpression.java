package de.ii.xtraplatform.cql.domain;

import java.util.Optional;

public interface TemporalExpression {

    //TODO: implement missing operations, see During

    //Optional<After> getAfter();

    //Optional<Before> getBefore();

    //Optional<Begins> getBegins();

    //Optional<BegunBy> getBegunBy();

    //Optional<TContains> getTContains();

    Optional<During> getDuring();

    //Optional<EndedBy> getEndedBy();

    //Optional<Ends> getEnds();

    //Optional<TEquals> getTEquals();

    //Optional<Meets> getMeets();

    //Optional<MetBy> getMetBy();

    //Optional<TOverlaps> getTOverlaps();

    //Optional<OverlappedBy> getOverlappedBy();

}
