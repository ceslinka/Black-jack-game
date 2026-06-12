package org.example.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.example.model.Card;
import org.example.model.Rank;
import org.example.model.Suit;
import org.springframework.stereotype.Component;

@Component
public class DeckService {

    public List<Card> createShuffledDeck() {
        List<Card> deck = new ArrayList<>();
        for (int d = 0; d < 6; d++) {
            for (Suit suit : Suit.values()) {
                for (Rank rank : Rank.values()) {
                    deck.add(new Card(rank, suit));
                }
            }
        }
        Collections.shuffle(deck);
        return deck;
    }
}
