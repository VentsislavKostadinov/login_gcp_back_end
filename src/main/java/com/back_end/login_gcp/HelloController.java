package com.back_end.login_gcp;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/api/users")
    public List<Map<String, Object>> getUsers() {
        return List.of(
                Map.of("id", 1, "name", "Alice", "email", "alice@example.com"),
                Map.of("id", 2, "name", "Bob", "email", "bob@example.com"),
                Map.of("id", 3, "name", "Charlie", "email", "charlie@example.com"));
    }
}
