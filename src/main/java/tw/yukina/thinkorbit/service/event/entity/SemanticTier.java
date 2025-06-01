package tw.yukina.thinkorbit.service.event.entity;

import lombok.Getter;

@Getter
public enum SemanticTier {
    INTERNAL("internal"),
    PERIPHERAL("peripheral"),
    AMBIENT("ambient");

    private final String tier;

    SemanticTier(String tier) {
        this.tier = tier;
    }
}
