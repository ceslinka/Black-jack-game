package org.example.game;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.example.model.Card;
import org.example.model.Rank;
import org.example.model.Suit;

public final class CardCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CardCodec() {}

    public static String encode(Card card) {
        return rankCode(card.getRank()) + suitCode(card.getSuit());
    }

    public static Card decode(String code) {
        if (code == null || code.length() != 2 || code.equals("**")) {
            throw new IllegalArgumentException("Invalid card code: " + code);
        }
        return new Card(decodeRank(code.charAt(0)), decodeSuit(code.charAt(1)));
    }

    public static List<String> toCodes(List<Card> cards) {
        return cards.stream().map(CardCodec::encode).toList();
    }

    public static List<Card> fromCodes(List<String> codes) {
        List<Card> cards = new ArrayList<>();
        for (String code : codes) {
            if (!"**".equals(code)) {
                cards.add(decode(code));
            }
        }
        return cards;
    }

    public static String toJson(List<String> codes) {
        try {
            return MAPPER.writeValueAsString(codes);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static List<String> fromJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new ArrayList<>();
            }
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static char rankCode(Rank rank) {
        return switch (rank) {
            case two -> '2';
            case three -> '3';
            case four -> '4';
            case five -> '5';
            case six -> '6';
            case seven -> '7';
            case eight -> '8';
            case nine -> '9';
            case ten -> 'T';
            case jack -> 'J';
            case queen -> 'Q';
            case king -> 'K';
            case ace -> 'A';
        };
    }

    private static Rank decodeRank(char c) {
        return switch (c) {
            case '2' -> Rank.two;
            case '3' -> Rank.three;
            case '4' -> Rank.four;
            case '5' -> Rank.five;
            case '6' -> Rank.six;
            case '7' -> Rank.seven;
            case '8' -> Rank.eight;
            case '9' -> Rank.nine;
            case 'T' -> Rank.ten;
            case 'J' -> Rank.jack;
            case 'Q' -> Rank.queen;
            case 'K' -> Rank.king;
            case 'A' -> Rank.ace;
            default -> throw new IllegalArgumentException("Unknown rank: " + c);
        };
    }

    private static char suitCode(Suit suit) {
        return switch (suit) {
            case hearts -> 'H';
            case diamonds -> 'D';
            case spades -> 'S';
            case clubs -> 'C';
        };
    }

    private static Suit decodeSuit(char c) {
        return switch (c) {
            case 'H' -> Suit.hearts;
            case 'D' -> Suit.diamonds;
            case 'S' -> Suit.spades;
            case 'C' -> Suit.clubs;
            default -> throw new IllegalArgumentException("Unknown suit: " + c);
        };
    }
}
