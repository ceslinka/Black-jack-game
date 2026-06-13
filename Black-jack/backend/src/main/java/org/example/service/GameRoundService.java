package org.example.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.example.dto.BetPlacedEvent;
import org.example.dto.BetResponse;
import org.example.dto.CardDealtEvent;
import org.example.dto.DealerTurnEvent;
import org.example.dto.GameStateEvent;
import org.example.dto.PlayerResult;
import org.example.dto.PlayerTurnEvent;
import org.example.dto.RoundResultEvent;
import org.example.dto.RoundSettledEvent;
import org.example.dto.RoundStartedEvent;
import org.example.dto.SeatHandView;
import org.example.entity.Bet;
import org.example.entity.BetStatus;
import org.example.entity.GameTable;
import org.example.entity.Hand;
import org.example.entity.HandStatus;
import org.example.entity.Round;
import org.example.entity.RoundStatus;
import org.example.entity.TableSeat;
import org.example.entity.TableStatus;
import org.example.entity.TransactionType;
import org.example.entity.User;
import org.example.entity.WalletTransaction;
import org.example.exception.ApiException;
import org.example.game.CardCodec;
import org.example.game.DeckService;
import org.example.game.HandCalculator;
import org.example.model.Card;
import org.example.repository.BetRepository;
import org.example.repository.GameTableRepository;
import org.example.repository.HandRepository;
import org.example.repository.RoundRepository;
import org.example.repository.TableSeatRepository;
import org.example.repository.UserRepository;
import org.example.repository.WalletTransactionRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameRoundService {

    private static final double MAX_BET_PERCENT = 0.25;

    private final GameTableRepository gameTableRepository;
    private final RoundRepository roundRepository;
    private final BetRepository betRepository;
    private final HandRepository handRepository;
    private final TableSeatRepository tableSeatRepository;
    private final UserRepository userRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final DeckService deckService;
    private final TableEventPublisher eventPublisher;
    private final TableService tableService;
    private final GameHistoryService gameHistoryService;

    private final Map<UUID, List<Card>> roundDecks = new ConcurrentHashMap<>();

    public GameRoundService(
            GameTableRepository gameTableRepository,
            RoundRepository roundRepository,
            BetRepository betRepository,
            HandRepository handRepository,
            TableSeatRepository tableSeatRepository,
            UserRepository userRepository,
            WalletTransactionRepository walletTransactionRepository,
            DeckService deckService,
            TableEventPublisher eventPublisher,
            @Lazy TableService tableService,
            GameHistoryService gameHistoryService) {
        this.gameTableRepository = gameTableRepository;
        this.roundRepository = roundRepository;
        this.betRepository = betRepository;
        this.handRepository = handRepository;
        this.tableSeatRepository = tableSeatRepository;
        this.userRepository = userRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.deckService = deckService;
        this.eventPublisher = eventPublisher;
        this.tableService = tableService;
        this.gameHistoryService = gameHistoryService;
    }

    @Transactional
    public void startRound(UUID tableId) {
        GameTable table = gameTableRepository
                .findById(tableId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Table not found"));

        if (table.getStatus() != TableStatus.waiting) {
            return;
        }

        long players = tableSeatRepository.countOccupiedPlayerSeats(tableId);
        if (players < 1) {
            return;
        }

        table.setStatus(TableStatus.in_game);
        gameTableRepository.save(table);

        Round round = new Round();
        round.setGameTable(table);
        round.setStatus(RoundStatus.betting);
        roundRepository.save(round);

        roundDecks.put(round.getId(), deckService.createShuffledDeck());

        eventPublisher.publishToTable(
                tableId, new RoundStartedEvent("round.started", tableId, round.getId()));
        publishGameState(round, "betting");
    }

    @Transactional
    public BetResponse placeBet(User user, UUID tableId, int amount) {
        Round round = getBettingRound(tableId);
        GameTable table = round.getGameTable();

        validateBetAmount(user, table, amount);
        ensurePlayerSeated(tableId, user.getId());

        if (betRepository.findByRoundIdAndUserId(round.getId(), user.getId()).isPresent()) {
            throw new ApiException("CONFLICT", "Bet already placed this round");
        }

        User locked = userRepository
                .findByIdForUpdate(user.getId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "User not found"));

        int maxAllowed = (int) Math.floor(locked.getBalance() * MAX_BET_PERCENT);
        if (amount > maxAllowed) {
            throw new ApiException("VALIDATION_ERROR", "Bet exceeds 25% of balance");
        }
        if (amount > locked.getBalance()) {
            throw new ApiException("VALIDATION_ERROR", "Insufficient balance");
        }

        locked.setBalance(locked.getBalance() - amount);
        userRepository.save(locked);

        Bet bet = new Bet();
        bet.setRound(round);
        bet.setUser(locked);
        bet.setAmount(amount);
        betRepository.save(bet);

        recordTransaction(locked, -amount, TransactionType.bet, round.getId());

        int seatIndex = tableSeatRepository
                .findByGameTableIdAndUserId(tableId, user.getId())
                .map(TableSeat::getSeatIndex)
                .orElse(-1);

        eventPublisher.publishToTable(
                tableId, new BetPlacedEvent("bet.placed", tableId, user.getId(), amount, seatIndex));

        long expectedBets = tableSeatRepository.countOccupiedPlayerSeats(tableId);
        if (betRepository.countByRoundId(round.getId()) >= expectedBets) {
            Round lockedRound = roundRepository
                    .findByIdForUpdate(round.getId())
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Round not found"));
            if (lockedRound.getStatus() == RoundStatus.betting) {
                dealInitialCards(lockedRound);
            }
            round = lockedRound;
        }

        publishGameState(round, round.getStatus() == RoundStatus.playing ? "player_turn" : "betting");
        return new BetResponse(bet.getId(), locked.getBalance());
    }

    @Transactional
    public void hit(User user, UUID tableId, UUID handId) {
        Round round = getPlayingRound(tableId);
        Hand hand = getPlayerHand(round, handId, user.getId());
        assertPlayerTurn(round, hand);

        List<String> codes = CardCodec.fromJson(hand.getCardsJson());
        List<Card> cards = CardCodec.fromCodes(codes);
        cards.add(drawCard(round.getId()));
        updateHandCards(hand, cards);

        eventPublisher.publishToTable(
                tableId,
                new CardDealtEvent(
                        "card.dealt",
                        tableId,
                        "player-" + hand.getSeatIndex(),
                        CardCodec.toCodes(cards),
                        false));

        if (hand.getStatus() == HandStatus.bust) {
            advanceAfterHandFinished(round, hand);
        } else {
            publishPlayerTurn(round, hand);
            publishGameState(round, "player_turn");
        }
    }

    @Transactional
    public void stand(User user, UUID tableId, UUID handId) {
        Round round = getPlayingRound(tableId);
        Hand hand = getPlayerHand(round, handId, user.getId());
        assertPlayerTurn(round, hand);

        hand.setStatus(HandStatus.stand);
        handRepository.save(hand);
        advanceAfterHandFinished(round, hand);
        publishGameState(round, round.getStatus() == RoundStatus.playing ? "player_turn" : "dealer_turn");
    }

    private void dealInitialCards(Round round) {
        if (round.getStatus() != RoundStatus.betting) {
            return;
        }
        UUID tableId = round.getGameTable().getId();
        List<TableSeat> playerSeats = tableSeatRepository.findByGameTableIdOrderBySeatIndex(tableId).stream()
                .filter(s -> s.getUser() != null && !s.isDealer())
                .toList();

        List<String> dealerCodes = new ArrayList<>();

        for (TableSeat seat : playerSeats) {
            List<Card> cards = List.of(drawCard(round.getId()), drawCard(round.getId()));
            Hand hand = new Hand();
            hand.setRound(round);
            hand.setUser(seat.getUser());
            hand.setSeatIndex(seat.getSeatIndex());
            updateHandCards(hand, cards);

            Bet bet = betRepository
                    .findByRoundIdAndUserId(round.getId(), seat.getUser().getId())
                    .orElseThrow();
            bet.setHandId(hand.getId());
            betRepository.save(bet);

            eventPublisher.publishToTable(
                    tableId,
                    new CardDealtEvent(
                            "card.dealt",
                            tableId,
                            "player-" + seat.getSeatIndex(),
                            CardCodec.toCodes(cards),
                            false));
        }

        dealerCodes.add(CardCodec.encode(drawCard(round.getId())));
        dealerCodes.add(CardCodec.encode(drawCard(round.getId())));
        round.setDealerCardsJson(CardCodec.toJson(dealerCodes));
        round.setDealerHidden(true);
        round.setStatus(RoundStatus.playing);
        roundRepository.save(round);

        eventPublisher.publishToTable(
                tableId,
                new CardDealtEvent("card.dealt", tableId, "dealer", List.of(dealerCodes.get(0), "**"), true));

        Hand firstActive = findFirstActiveHand(round);
        if (firstActive == null) {
            runDealerAndSettle(round);
        } else {
            round.setCurrentSeatIndex(firstActive.getSeatIndex());
            roundRepository.save(round);
            publishPlayerTurn(round, firstActive);
        }
    }

    private void advanceAfterHandFinished(Round round, Hand finishedHand) {
        Hand next = handRepository.findByRoundIdOrderBySeatIndex(round.getId()).stream()
                .filter(h -> h.getStatus() == HandStatus.active || h.getStatus() == HandStatus.blackjack)
                .filter(h -> h.getSeatIndex() > finishedHand.getSeatIndex())
                .findFirst()
                .orElse(null);

        if (next != null) {
            round.setCurrentSeatIndex(next.getSeatIndex());
            roundRepository.save(round);
            publishPlayerTurn(round, next);
        } else {
            runDealerAndSettle(round);
        }
    }

    private void runDealerAndSettle(Round round) {
        UUID tableId = round.getGameTable().getId();
        GameTable table = round.getGameTable();
        List<String> dealerCodes = CardCodec.fromJson(round.getDealerCardsJson());
        List<Card> dealerCards = CardCodec.fromCodes(dealerCodes);

        round.setDealerHidden(false);
        roundRepository.save(round);

        eventPublisher.publishToTable(
                tableId, new DealerTurnEvent("dealer.turn", tableId, CardCodec.toCodes(dealerCards)));

        while (HandCalculator.calculateValue(dealerCards) < 17) {
            dealerCards.add(drawCard(round.getId()));
        }
        round.setDealerCardsJson(CardCodec.toJson(CardCodec.toCodes(dealerCards)));
        roundRepository.save(round);

        eventPublisher.publishToTable(
                tableId,
                new CardDealtEvent("card.dealt", tableId, "dealer", CardCodec.toCodes(dealerCards), false));

        int dealerValue = HandCalculator.calculateValue(dealerCards);
        List<PlayerResult> results = new ArrayList<>();

        for (Hand hand : handRepository.findByRoundIdOrderBySeatIndex(round.getId())) {
            Bet bet = betRepository
                    .findByRoundIdAndUserId(round.getId(), hand.getUser().getId())
                    .orElseThrow();
            int handValue = hand.getValue() != null ? hand.getValue() : 0;
            String outcome;
            int payout = 0;

            if (hand.getStatus() == HandStatus.bust) {
                outcome = "lose";
            } else if (hand.getStatus() == HandStatus.blackjack && dealerCards.size() == 2 && dealerValue == 21) {
                outcome = "push";
                payout = bet.getAmount();
            } else if (hand.getStatus() == HandStatus.blackjack) {
                outcome = "win";
                payout = (int) Math.floor(bet.getAmount() * 2.5);
            } else if (dealerValue > 21 || handValue > dealerValue) {
                outcome = "win";
                payout = bet.getAmount() * 2;
            } else if (handValue == dealerValue) {
                outcome = "push";
                payout = bet.getAmount();
            } else {
                outcome = "lose";
            }

            if (payout > 0) {
                User locked = userRepository
                        .findByIdForUpdate(hand.getUser().getId())
                        .orElseThrow();
                locked.setBalance(locked.getBalance() + payout);
                userRepository.save(locked);
                recordTransaction(locked, payout, TransactionType.payout, round.getId());
                gameHistoryService.recordPlayerRound(
                        locked, table, round.getId(), outcome, bet.getAmount(), payout);
            } else {
                User locked = userRepository
                        .findByIdForUpdate(hand.getUser().getId())
                        .orElseThrow();
                gameHistoryService.recordPlayerRound(
                        locked, table, round.getId(), outcome, bet.getAmount(), payout);
            }

            bet.setStatus(BetStatus.settled);
            betRepository.save(bet);

            results.add(new PlayerResult(hand.getUser().getId(), handValue, dealerValue, outcome, payout));
        }

        round.setStatus(RoundStatus.settled);
        round.setEndedAt(java.time.Instant.now());
        roundRepository.save(round);

        eventPublisher.publishToTable(
                tableId, new RoundResultEvent("round.result", tableId, round.getId(), results));
        eventPublisher.publishToTable(
                tableId, new RoundSettledEvent("round.settled", tableId, round.getId(), results));

        roundDecks.remove(round.getId());
        publishGameState(round, "settled");
        tableService.beginIntermission(tableId);
    }

    @Transactional
    public void abortRoundIfBetting(UUID tableId) {
        Round round = roundRepository
                .findFirstByGameTableIdAndStatusOrderByStartedAtDesc(tableId, RoundStatus.betting)
                .orElse(null);
        if (round == null) {
            throw new ApiException("FORBIDDEN", "Cannot leave during active game");
        }
        round.setStatus(RoundStatus.settled);
        round.setEndedAt(java.time.Instant.now());
        roundRepository.save(round);
        roundDecks.remove(round.getId());

        GameTable table = gameTableRepository.findById(tableId).orElseThrow();
        table.setStatus(TableStatus.waiting);
        gameTableRepository.save(table);
    }

    @Transactional(readOnly = true)
    public GameStateEvent getGameState(UUID tableId) {
        GameTable table = gameTableRepository
                .findById(tableId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Table not found"));

        Round round = roundRepository
                .findFirstByGameTableIdAndStatusOrderByStartedAtDesc(tableId, RoundStatus.betting)
                .or(() -> roundRepository.findFirstByGameTableIdAndStatusOrderByStartedAtDesc(tableId, RoundStatus.playing))
                .orElse(null);

        if (round == null && (table.getStatus() == TableStatus.waiting || table.getStatus() == TableStatus.settlement)) {
            round = roundRepository
                    .findFirstByGameTableIdAndStatusOrderByStartedAtDesc(tableId, RoundStatus.settled)
                    .orElse(null);
        }

        if (round == null) {
            return null;
        }
        String phase =
                switch (round.getStatus()) {
                    case betting -> "betting";
                    case playing -> "player_turn";
                    default -> "settled";
                };
        return buildGameStateEvent(round, phase);
    }

    private void publishGameState(Round round, String phase) {
        eventPublisher.publishToTable(round.getGameTable().getId(), buildGameStateEvent(round, phase));
    }

    private GameStateEvent buildGameStateEvent(Round round, String phase) {
        UUID tableId = round.getGameTable().getId();
        List<SeatHandView> seats = new ArrayList<>();

        for (TableSeat seat : tableSeatRepository.findByGameTableIdOrderBySeatIndex(tableId)) {
            if (seat.getUser() == null || seat.isDealer()) {
                continue;
            }
            Integer betAmount = null;
            UUID handId = null;
            List<String> cards = List.of();
            Integer handValue = null;
            String handStatus = null;

            if (round.getStatus() != RoundStatus.betting) {
                var handOpt = handRepository.findByRoundIdAndUserId(round.getId(), seat.getUser().getId());
                if (handOpt.isPresent()) {
                    Hand hand = handOpt.get();
                    handId = hand.getId();
                    cards = CardCodec.fromJson(hand.getCardsJson());
                    handValue = hand.getValue();
                    handStatus = hand.getStatus().name();
                }
            }
            var betOpt = betRepository.findByRoundIdAndUserId(round.getId(), seat.getUser().getId());
            if (betOpt.isPresent()) {
                betAmount = betOpt.get().getAmount();
            }

            seats.add(new SeatHandView(
                    seat.getSeatIndex(),
                    seat.getUser().getId(),
                    seat.getUser().getUsername(),
                    handId,
                    cards,
                    handValue,
                    handStatus,
                    betAmount));
        }

        List<String> dealerCards = round.getStatus() == RoundStatus.betting
                ? List.of()
                : maskDealerCards(round);

        return new GameStateEvent(
                "game.state",
                tableId,
                round.getId(),
                phase,
                round.getCurrentSeatIndex(),
                seats,
                dealerCards,
                round.isDealerHidden());
    }

    private List<String> maskDealerCards(Round round) {
        List<String> codes = CardCodec.fromJson(round.getDealerCardsJson());
        if (codes.isEmpty()) {
            return List.of();
        }
        if (round.isDealerHidden() && codes.size() >= 2) {
            List<String> masked = new ArrayList<>();
            masked.add(codes.get(0));
            masked.add("**");
            return masked;
        }
        return codes;
    }

    private void publishPlayerTurn(Round round, Hand hand) {
        UUID tableId = round.getGameTable().getId();
        List<String> actions = hand.getStatus() == HandStatus.active ? List.of("hit", "stand") : List.of();
        eventPublisher.publishToTable(
                tableId,
                new PlayerTurnEvent(
                        "player.turn",
                        tableId,
                        hand.getSeatIndex(),
                        hand.getId(),
                        hand.getUser().getId(),
                        actions));
    }

    private void updateHandCards(Hand hand, List<Card> cards) {
        HandStatus status = HandCalculator.resolveStatus(cards, hand.getStatus());
        if (status == HandStatus.active && cards.size() >= 2) {
            status = HandStatus.active;
        }
        if (HandCalculator.isBlackjack(cards)) {
            status = HandStatus.blackjack;
        }
        hand.setCardsJson(CardCodec.toJson(CardCodec.toCodes(cards)));
        hand.setValue(HandCalculator.calculateValue(cards));
        hand.setStatus(status);
        handRepository.save(hand);
    }

    private Hand findFirstActiveHand(Round round) {
        return handRepository.findByRoundIdOrderBySeatIndex(round.getId()).stream()
                .filter(h -> h.getStatus() == HandStatus.active)
                .findFirst()
                .orElse(null);
    }

    private Card drawCard(UUID roundId) {
        List<Card> deck = ensureRoundDeck(roundId);
        if (deck.isEmpty()) {
            deck.addAll(deckService.createShuffledDeck());
        }
        return deck.removeFirst();
    }

    private List<Card> ensureRoundDeck(UUID roundId) {
        return roundDecks.computeIfAbsent(roundId, id -> deckService.createShuffledDeck());
    }

    private void validateBetAmount(User user, GameTable table, int amount) {
        if (amount < table.getMinBet()) {
            throw new ApiException("VALIDATION_ERROR", "Bet below table minimum");
        }
        if (amount > table.getMaxBet()) {
            throw new ApiException("VALIDATION_ERROR", "Bet above table maximum");
        }
        if (amount <= 0) {
            throw new ApiException("VALIDATION_ERROR", "Bet must be positive");
        }
    }

    private void ensurePlayerSeated(UUID tableId, UUID userId) {
        tableSeatRepository
                .findByGameTableIdAndUserId(tableId, userId)
                .orElseThrow(() -> new ApiException("FORBIDDEN", "Not seated at this table"));
    }

    private Round getBettingRound(UUID tableId) {
        Round round = roundRepository
                .findFirstByGameTableIdAndStatusOrderByStartedAtDesc(tableId, RoundStatus.betting)
                .orElseThrow(() -> new ApiException("FORBIDDEN", "No betting round active"));
        if (round.getGameTable().getStatus() != TableStatus.in_game) {
            throw new ApiException("FORBIDDEN", "Table not in game");
        }
        return round;
    }

    private Round getPlayingRound(UUID tableId) {
        return roundRepository
                .findFirstByGameTableIdAndStatusOrderByStartedAtDesc(tableId, RoundStatus.playing)
                .orElseThrow(() -> new ApiException("FORBIDDEN", "No active playing round"));
    }

    private Hand getPlayerHand(Round round, UUID handId, UUID userId) {
        Hand hand = handRepository
                .findById(handId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Hand not found"));
        if (!hand.getRound().getId().equals(round.getId()) || !hand.getUser().getId().equals(userId)) {
            throw new ApiException("FORBIDDEN", "Not your hand");
        }
        return hand;
    }

    private void assertPlayerTurn(Round round, Hand hand) {
        if (hand.getStatus() != HandStatus.active) {
            throw new ApiException("FORBIDDEN", "Hand not active");
        }
        if (round.getCurrentSeatIndex() == null || round.getCurrentSeatIndex() != hand.getSeatIndex()) {
            throw new ApiException("FORBIDDEN", "Not your turn");
        }
    }

    private void recordTransaction(User user, int amount, TransactionType type, UUID roundId) {
        WalletTransaction tx = new WalletTransaction();
        tx.setUser(user);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setRoundId(roundId);
        walletTransactionRepository.save(tx);
    }
}
