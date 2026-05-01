package ch.zhaw.trueyield.service.provider;

import java.time.LocalDate;

public record NewsArticle(String title, String content, String url, LocalDate publishedAt, String sourceName) {}
