package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardNumberUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final UserRepository userRepository;
    private final CardRepository  cardRepository;

    public Page<CardDto> getAllCards(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("balance").descending());
        return cardRepository.findAll(pageable)
                .map(card -> new CardDto(
                        card.getId(),
                        CardNumberUtil.maskCardNumber(card.getNumber()),
                        card.getExpirationDate(),
                        card.getStatus(),
                        card.getBalance(),
                        card.getCreatedAt(),
                        card.getUser().getUsername()
                ));
    }

    public Page<CardDto> getUserCards(int page, int size, String numberFilter) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Пользователь %s не найден", username)));
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<CardDto> cardsList = cardRepository.findByUser(user)
                .stream()
                .filter(card -> card.getNumber().contains(numberFilter))
                .map(card -> new CardDto(
                        card.getId(),
                        CardNumberUtil.maskCardNumber(card.getNumber()),
                        card.getExpirationDate(),
                        card.getStatus(),
                        card.getBalance(),
                        card.getCreatedAt(),
                        card.getUser().getUsername()
                ))
                .toList();
        return new PageImpl<>(cardsList, pageable, cardsList.size());
    }

    @Transactional
    public void transferBetweenCards(TransferRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Пользователь %s не найден", username)));
        Card fromCard = cardRepository.findById(request.getFromCardId())
                .orElseThrow(() -> new EntityNotFoundException(String.format("Карта-отправитель с id %d не найдена", request.getFromCardId())));
        Card toCard = cardRepository.findById(request.getToCardId())
                .orElseThrow(() -> new EntityNotFoundException(String.format("Карта-получатель с id %d не найдена", request.getToCardId())));

        if (!fromCard.getUser().getId().equals(user.getId()) || !toCard.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Перевод доступен только между своими картами");
        }

        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException(String.format("Карта-отправитель с id %d неактивна", fromCard.getId()));
        }
        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException(String.format("Карта-получатель с id %d неактивна", toCard.getId()));
        }

        BigDecimal amount = request.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма перевода должна быть положительной");
        }
        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Недостаточно средств на карте-источнике");
        }

        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);
    }

    @Transactional
    public CardDto createCard(CreateCardRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Пользователь с id %d не найден", request.getUserId())
                ));

        BigDecimal balance = request.getStartBalance();
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Начальный баланс карты не может быть отрицательным");
        }

        String generatedCardNumber = CardNumberUtil.generateCardNumber();

        Card card = new Card();
        card.setNumber(generatedCardNumber);
        card.setExpirationDate(LocalDateTime.now().plusYears(4));
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(balance);
        card.setCreatedAt(LocalDateTime.now());
        card.setUser(user);

        cardRepository.save(card);

        return new CardDto(
                card.getId(),
                CardNumberUtil.maskCardNumber(generatedCardNumber),
                card.getExpirationDate(),
                card.getStatus(),
                card.getBalance(),
                card.getCreatedAt(),
                user.getUsername()
        );
    }

    @Transactional
    public void blockCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Карта с id %d не найдена", id)
        ));
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new IllegalStateException("Карта уже заблокирована");
        }
        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
    }

    @Transactional
    public void requestCardBlock(Long cardId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException(
                String.format("Пользователь %s не найден", username)
        ));
        Card card = cardRepository.findById(cardId).orElseThrow(() -> new EntityNotFoundException(
                String.format("Карта с id %d не найдена", cardId)
        ));

        if (!card.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Запрос на блокировку чужой карты");
        }
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new IllegalStateException("Карта уже заблокирована");
        }
        if (card.getStatus() == CardStatus.BLOCK_REQUESTED) {
            throw new IllegalStateException("Запрос на блокировку уже отправлен");
        }

        card.setStatus(CardStatus.BLOCK_REQUESTED);
        cardRepository.save(card);


    }

    @Transactional
    public void activateCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Карта с id %d не найдена", id)
                ));
        if (card.getStatus() == CardStatus.ACTIVE) {
            throw new IllegalStateException("Карта уже активирована");
        }
        card.setStatus(CardStatus.ACTIVE);
        cardRepository.save(card);
    }

    @Transactional
    public void deleteCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Карта с id %d не найдена", id)
                ));
        cardRepository.delete(card);
    }

}
