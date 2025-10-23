package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardService cardService;

    private User user;
    private Card fromCard;
    private Card toCard;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("Иван Иванов");

        fromCard = new Card();
        fromCard.setId(1L);
        fromCard.setNumber("1111222233334444");
        fromCard.setStatus(CardStatus.ACTIVE);
        fromCard.setBalance(BigDecimal.valueOf(1000));
        fromCard.setUser(user);

        toCard = new Card();
        toCard.setId(2L);
        toCard.setNumber("5555666677778888");
        toCard.setStatus(CardStatus.ACTIVE);
        toCard.setBalance(BigDecimal.valueOf(500));
        toCard.setUser(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("Иван Иванов", "password")
        );
    }

    @Test
    void transferBetweenCards_validRequest_shouldUpdateBalances() {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromCard.getId());
        request.setToCardId(toCard.getId());
        request.setAmount(BigDecimal.valueOf(200));

        when(userRepository.findByUsername("Иван Иванов")).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        cardService.transferBetweenCards(request);

        assertThat(fromCard.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(800));
        assertThat(toCard.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(700));

        verify(cardRepository).save(fromCard);
        verify(cardRepository).save(toCard);
    }

    @Test
    void transferBetweenCards_insufficientFunds_shouldThrow() {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromCard.getId());
        request.setToCardId(toCard.getId());
        request.setAmount(BigDecimal.valueOf(2000));

        when(userRepository.findByUsername("Иван Иванов")).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        assertThrows(IllegalStateException.class, () -> cardService.transferBetweenCards(request));
    }

    @Test
    void transferBetweenCards_wrongUser_shouldThrow() {
        User otherUser = new User();
        otherUser.setId(2L);
        toCard.setUser(otherUser);

        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromCard.getId());
        request.setToCardId(toCard.getId());
        request.setAmount(BigDecimal.valueOf(100));

        when(userRepository.findByUsername("Иван Иванов")).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        assertThrows(AccessDeniedException.class, () -> cardService.transferBetweenCards(request));
    }

    @Test
    void createCard_existingUser_shouldReturnDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        CardDto dto = cardService.createCard(new CreateCardRequest(1L, BigDecimal.valueOf(500)));

        assertThat(dto.getOwnerUsername()).isEqualTo(user.getUsername());
        assertThat(dto.getNumber()).startsWith("****");
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void createCard_nonExistingUser_shouldThrow() {
        when(userRepository.findById(12245L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> cardService.createCard(new CreateCardRequest(12245L, BigDecimal.valueOf(500))));
    }

    @Test
    void blockCard_activeCard_shouldChangeStatus() {
        fromCard.setStatus(CardStatus.ACTIVE);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));

        cardService.blockCard(1L);

        assertThat(fromCard.getStatus()).isEqualTo(CardStatus.BLOCKED);
        verify(cardRepository).save(fromCard);
    }

    @Test
    void blockCard_alreadyBlocked_shouldThrow() {
        fromCard.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));

        assertThrows(IllegalStateException.class, () -> cardService.blockCard(1L));
    }
}
