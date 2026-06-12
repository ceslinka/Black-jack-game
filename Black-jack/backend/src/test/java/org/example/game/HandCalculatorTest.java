package org.example.game;

import java.util.List;
import org.example.model.Card;
import org.example.model.Rank;
import org.example.model.Suit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HandCalculatorTest {

    @Test
    void calculatesBlackjack() {
        List<Card> cards = List.of(new Card(Rank.ace, Suit.spades), new Card(Rank.king, Suit.hearts));
        assertThat(HandCalculator.calculateValue(cards)).isEqualTo(21);
        assertThat(HandCalculator.isBlackjack(cards)).isTrue();
    }

    @Test
    void softAceAdjustsForBust() {
        List<Card> cards = List.of(
                new Card(Rank.ace, Suit.spades),
                new Card(Rank.seven, Suit.hearts),
                new Card(Rank.five, Suit.clubs));
        assertThat(HandCalculator.calculateValue(cards)).isEqualTo(13);
    }
}
