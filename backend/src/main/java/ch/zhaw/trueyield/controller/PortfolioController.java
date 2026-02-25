package ch.zhaw.trueyield.controller;

import ch.zhaw.trueyield.model.Portfolio;
import ch.zhaw.trueyield.model.dto.PortfolioCreateDTO;
import ch.zhaw.trueyield.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @PostMapping
    public ResponseEntity<Portfolio> createPortfolio(@Valid @RequestBody PortfolioCreateDTO dto) {
        Portfolio created = portfolioService.createPortfolio(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }
}
