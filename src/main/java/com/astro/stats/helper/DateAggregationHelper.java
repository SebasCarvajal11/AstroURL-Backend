package com.astro.stats.helper;

import com.astro.stats.dto.DailyClickCount;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DateAggregationHelper {

    private final Clock clock;

    public DateAggregationHelper(Clock clock) {
        this.clock = clock;
    }

    public List<DailyClickCount> aggregateClicksOverLast7Days(List<DailyClickCount> dbResults) {
        Map<LocalDate, Long> clicksByDate = dbResults.stream()
                .collect(Collectors.toMap(DailyClickCount::getDate, DailyClickCount::getCount));

        LocalDate today = LocalDate.now(clock);
        List<DailyClickCount> fullWeekData = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = today.minusDays(i);
            long count = clicksByDate.getOrDefault(date, 0L);
            fullWeekData.add(new DailyClickCount(date, count));
        }

        fullWeekData.sort(java.util.Comparator.comparing(DailyClickCount::getDate));
        return fullWeekData;
    }
}