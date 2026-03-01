package ch.zhaw.trueyield.controller;

import java.util.List;

import ch.zhaw.trueyield.model.Portfolio;
import ch.zhaw.trueyield.model.dto.PortfolioCreateDTO;
import ch.zhaw.trueyield.model.dto.PortfolioUpdateDTO;
import ch.zhaw.trueyield.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    @Autowired
    private PortfolioService portfolioService;

    @PostMapping
    public ResponseEntity<Portfolio> createPortfolio(
            @Valid @RequestBody PortfolioCreateDTO dto,
            @RequestHeader(value = "X-Fund-Manager-Id", required = false, defaultValue = "temp-user-123") String fundManagerId) {
        try {
            Portfolio created = portfolioService.createPortfolio(dto, fundManagerId);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<List<Portfolio>> getAllPortfolios(
            @RequestHeader(value = "X-Fund-Manager-Id", required = false, defaultValue = "temp-user-123") String fundManagerId) {
        List<Portfolio> portfolios = portfolioService.getAllPortfoliosByFundManager(fundManagerId);
        return new ResponseEntity<>(portfolios, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Portfolio> getPortfolioById(
            @PathVariable String id,
            @RequestHeader(value = "X-Fund-Manager-Id", required = false, defaultValue = "temp-user-123") String fundManagerId) {
        try {
            Portfolio portfolio = portfolioService.getPortfolioById(id, fundManagerId);
            return new ResponseEntity<>(portfolio, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getStatusCode());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Portfolio> updatePortfolio(
            @PathVariable String id,
            @RequestBody PortfolioUpdateDTO dto,
            @RequestHeader(value = "X-Fund-Manager-Id", required = false, defaultValue = "temp-user-123") String fundManagerId) {
        try {
            Portfolio updated = portfolioService.updatePortfolio(id, dto, fundManagerId);
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getStatusCode());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePortfolio(
            @PathVariable String id,
            @RequestHeader(value = "X-Fund-Manager-Id", required = false, defaultValue = "temp-user-123") String fundManagerId) {
        try {
            portfolioService.deletePortfolio(id, fundManagerId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getStatusCode());
        }
    }
}
