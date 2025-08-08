package com.astro.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyClickCount {
    private LocalDate date;
    private long count;

    public DailyClickCount(java.sql.Date date, Long count) {
        this.date = date.toLocalDate();
        this.count = (count != null) ? count : 0L;
    }
}