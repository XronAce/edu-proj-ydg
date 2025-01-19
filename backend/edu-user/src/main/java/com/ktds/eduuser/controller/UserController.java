package com.ktds.eduuser.controller;

import com.ktds.eduuser.dto.response.GetUserResDto;
import com.ktds.eduuser.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
@RestController
public class UserController {
    private final UserService userService;

    @GetMapping("/{userNo}")
    public ResponseEntity<GetUserResDto> getUser(@PathVariable int userNo) {
        try {
            GetUserResDto getUserResDto = userService.getUser(userNo);
            return new ResponseEntity<>(getUserResDto, HttpStatus.OK);
        } catch (HttpServerErrorException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
