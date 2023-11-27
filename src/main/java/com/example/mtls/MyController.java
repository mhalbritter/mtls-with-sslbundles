package com.example.mtls;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Moritz Halbritter
 */
@RestController
class MyController {
    @GetMapping(path = "/", produces = MediaType.TEXT_PLAIN_VALUE)
    String index() {
        return "index";
    }
}
