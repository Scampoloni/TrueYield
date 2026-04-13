package ch.zhaw.trueyield.controller;

import java.util.List;
import java.util.stream.Collectors;

import ch.zhaw.trueyield.model.Portfolio;
import ch.zhaw.trueyield.model.dto.PagedResponseDTO;
import ch.zhaw.trueyield.model.dto.PortfolioCreateDTO;
import ch.zhaw.trueyield.model.dto.PortfolioResponseDTO;
import ch.zhaw.trueyield.model.dto.PortfolioUpdateDTO;
import ch.zhaw.trueyield.security.UserService;
import ch.zhaw.trueyield.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('fund-manager')")
    public ResponseEntity<PortfolioResponseDTO> createPortfolio(@Valid @RequestBody PortfolioCreateDTO dto) {
        try {
            String fundManagerId = userService.getCurrentUserId();
            Portfolio created = portfolioService.createPortfolio(dto, fundManagerId);
            return new ResponseEntity<>(PortfolioResponseDTO.fromEntity(created), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllPortfolios(
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize) {
        String fundManagerId = userService.getCurrentUserId();

        // Unpaginierter Pfad — bestehende Tests und andere Seiten nutzen diesen (kein Query-Param)
        if (pageNumber == null || pageSize == null) {
            List<Portfolio> portfolios = portfolioService.getAllPortfoliosByFundManager(fundManagerId);
            List<PortfolioResponseDTO> response = portfolios.stream()
                    .map(PortfolioResponseDTO::fromEntity)
                    .collect(Collectors.toList());
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        // Paginierter Pfad
        Page<Portfolio> page = portfolioService.getAllPortfoliosByFundManagerPaged(
                fundManagerId, pageNumber, pageSize);
        List<PortfolioResponseDTO> content = page.getContent().stream()
                .map(PortfolioResponseDTO::fromEntity)
                .collect(Collectors.toList());
        PagedResponseDTO<PortfolioResponseDTO> pagedResponse = new PagedResponseDTO<>(
                content,
                page.getTotalPages(),
                page.getTotalElements(),
                pageNumber,
                pageSize);
        return new ResponseEntity<>(pagedResponse, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PortfolioResponseDTO> getPortfolioById(@PathVariable String id) {
        try {
            String fundManagerId = userService.getCurrentUserId();
            Portfolio portfolio = portfolioService.getPortfolioById(id, fundManagerId);
            return new ResponseEntity<>(PortfolioResponseDTO.fromEntity(portfolio), HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getStatusCode());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('fund-manager')")
    public ResponseEntity<PortfolioResponseDTO> updatePortfolio(
            @PathVariable String id,
            @RequestBody PortfolioUpdateDTO dto) {
        try {
            String fundManagerId = userService.getCurrentUserId();
            Portfolio updated = portfolioService.updatePortfolio(id, dto, fundManagerId);
            return new ResponseEntity<>(PortfolioResponseDTO.fromEntity(updated), HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getStatusCode());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('fund-manager')")
    public ResponseEntity<Void> deletePortfolio(@PathVariable String id) {
        try {
            String fundManagerId = userService.getCurrentUserId();
            portfolioService.deletePortfolio(id, fundManagerId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getStatusCode());
        }
    }
}
