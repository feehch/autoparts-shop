package feehch.dev.autoparts.presentation.rest;

import feehch.dev.autoparts.application.stats.DashboardView;
import feehch.dev.autoparts.application.stats.StatsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@PreAuthorize("hasRole('MANAGER')")
@Tag(name = "Статистика")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/dashboard")
    public DashboardView dashboard() {
        return statsService.dashboard();
    }
}
