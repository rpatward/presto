package com.facebook.presto.sql.planner.plan;

import com.facebook.presto.sql.planner.Symbol;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Immutable
public class UnionNode
        extends PlanNode
{
    private final List<PlanNode> sources;
    private final ImmutableListMultimap<Symbol, Symbol> symbolMapping; // Key is output symbol, value is a List of the input symbols supplying that output

    @JsonCreator
    public UnionNode(@JsonProperty("id") PlanNodeId id,
            @JsonProperty("sources") List<PlanNode> sources,
            @JsonProperty("symbolMapping") ListMultimap<Symbol, Symbol> symbolMapping)
    {
        super(id);

        checkNotNull(sources, "sources is null");
        checkArgument(!sources.isEmpty(), "Must have at least one source");
        checkNotNull(symbolMapping, "symbolMapping is null");

        this.sources = ImmutableList.copyOf(sources);
        this.symbolMapping = ImmutableListMultimap.copyOf(symbolMapping);

        for (Collection<Symbol> symbols : this.symbolMapping.asMap().values()) {
            checkArgument(symbols.size() == this.sources.size(), "Every source needs to map its symbols to an output UNION symbol");
        }

        // Make sure each source positionally corresponds to their Symbol values in the Multimap
        for (int i = 0; i < sources.size(); i++) {
            for (Collection<Symbol> symbols : this.symbolMapping.asMap().values()) {
                checkArgument(sources.get(i).getOutputSymbols().contains(Iterables.get(symbols, i)), "Source does not provide required symbols");
            }
        }
    }

    @Override
    @JsonProperty("sources")
    public List<PlanNode> getSources()
    {
        return sources;
    }

    @Override
    public List<Symbol> getOutputSymbols()
    {
        return ImmutableList.copyOf(symbolMapping.keySet());
    }

    @JsonProperty("symbolMapping")
    public ListMultimap<Symbol, Symbol> getSymbolMapping()
    {
        return symbolMapping;
    }

    public List<Symbol> sourceOutputLayout(int sourceIndex)
    {
        // Make sure the sourceOutputLayout symbols are listed in the same order as the corresponding output symbols
        ImmutableList.Builder<Symbol> builder = ImmutableList.builder();
        for (Symbol outputSymbol : getOutputSymbols()) {
            builder.add(Iterables.get(symbolMapping.get(outputSymbol), sourceIndex));
        }
        return builder.build();
    }

    @Override
    public <C, R> R accept(PlanVisitor<C, R> visitor, C context)
    {
        return visitor.visitUnion(this, context);
    }
}