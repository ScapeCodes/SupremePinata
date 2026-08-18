package net.scape.project.supremepinata.reward;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RewardPool {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final List<Reward> rewards = new ArrayList<>();
    private int totalWeight;

    public void add(Reward reward) {
        if (reward.weight() <= 0) return;
        rewards.add(reward);
        totalWeight += reward.weight();
    }

    public Optional<Reward> roll() {
        if (rewards.isEmpty() || totalWeight <= 0) return Optional.empty();
        int target = RANDOM.nextInt(totalWeight) + 1;
        int cursor = 0;
        for (Reward reward : rewards) {
            cursor += reward.weight();
            if (target <= cursor) return Optional.of(reward);
        }
        return Optional.empty();
    }

    public boolean isEmpty() {
        return rewards.isEmpty();
    }
}
