package com.affismart.mall.modules.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "Lightweight health check endpoint for uptime monitoring")
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    /**
     * Returns a simple OK status without touching the database.
     * Used by UptimeRobot to keep the Render free-tier service alive.
     * Must be publicly accessible (no authentication required).
     */
    @Operation(summary = "Health check (Public)")
    @SecurityRequirements
    @GetMapping
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
