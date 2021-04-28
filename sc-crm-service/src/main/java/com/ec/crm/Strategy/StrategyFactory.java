package com.ec.crm.Strategy;

import com.ec.crm.Data.StrategyEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class StrategyFactory {
    private Map<StrategyEnum, IStrategy> strategies;

    @Autowired
    public StrategyFactory(Set<IStrategy> strategySet) {
        createStrategy(strategySet);
    }

    public IStrategy findStrategy(StrategyEnum strategyName) {
        return strategies.get(strategyName);
    }
    private void createStrategy(Set<IStrategy> strategySet) {
        strategies = new HashMap<StrategyEnum, IStrategy>();
        strategySet.forEach(
                strategy ->strategies.put(strategy.getStrategyName(), strategy));
    }
}
