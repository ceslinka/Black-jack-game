package org.example.game;

import java.util.List;
import org.example.entity.HandStatus;
import org.example.model.Card;
import org.example.model.Rank;

public final class HandCalculator {

    private HandCalculator() {}

    public static int calculateValue(List<Card> cards) {
        int sum = 0;
        int aces = 0;
        for (Card card : cards) {
            Rank rank = card.getRank();
            if (rank == Rank.ace) {
                aces++;
                sum += 11;
            } else if (rank.getValue() >= 10) {
                sum += 10;
            } else {
                sum += rank.getValue();
            }
        }
        while (sum > 21 && aces > 0) {
            sum -= 10;
            aces--;
        }
        return sum;
    }

    public static boolean isBlackjack(List<Card> cards) {
        return cards.size() == 2 && calculateValue(cards) == 21;
    }

    public static HandStatus resolveStatus(List<Card> cards, HandStatus current) {
        int value = calculateValue(cards);
        if (value > 21) {
            return HandStatus.bust;
        }
        if (isBlackjack(cards)) {
            return HandStatus.blackjack;
        }
        return current;
    }
}
