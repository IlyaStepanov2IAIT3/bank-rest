package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CardDto>> getAllCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(cardService.getAllCards(page, size));
    }

    @GetMapping()
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<CardDto>> getUserCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String numberFilter) {

        Page<CardDto> cardsPage = cardService.getUserCards(page, size, numberFilter);
        return ResponseEntity.ok(cardsPage);
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> transferBetweenCards(@RequestBody TransferRequest request) {
        cardService.transferBetweenCards(request);
        return ResponseEntity.ok("Перевод успешно выполнен");
    }

    @PostMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardDto> createCard(@RequestBody CreateCardRequest request) {
        return ResponseEntity.ok(cardService.createCard(request));
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> blockCard(@PathVariable Long id) {
        cardService.blockCard(id);
        return ResponseEntity.ok("Карта успешно заблокирована");
    }

    @PatchMapping("/{id}/block-request")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> requestCardBlock(@PathVariable Long id) {
        cardService.requestCardBlock(id);
        return ResponseEntity.ok("Запрос на блокировку карты отправлен");
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> activateCard(@PathVariable Long id) {
        cardService.activateCard(id);
        return ResponseEntity.ok("Карта успешно активирована");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
        return ResponseEntity.ok("Карта успешно удалена");
    }
}
