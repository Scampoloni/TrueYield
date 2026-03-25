package ch.zhaw.trueyield.controller;

import ch.zhaw.trueyield.model.Holding;
import ch.zhaw.trueyield.model.dto.HoldingCreateDTO;
import ch.zhaw.trueyield.service.HoldingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/holding")
public class HoldingController {

    @Autowired
    private HoldingService holdingService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Holding>> getHoldingsByPortfolioId(@RequestParam String portfolioId) {
        return new ResponseEntity<>(holdingService.getHoldingsByPortfolioId(portfolioId), HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasRole('fund-manager')")
    public ResponseEntity<Holding> createHolding(@Valid @RequestBody HoldingCreateDTO dto) {
        try {
            Holding created = holdingService.createHolding(dto);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getStatusCode());
        }
    }
}
