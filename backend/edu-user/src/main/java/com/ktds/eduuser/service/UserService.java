package com.ktds.eduuser.service;

import com.ktds.eduuser.dto.response.GetGoodsResDto;
import com.ktds.eduuser.dto.response.GetUserResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    @Value("${goods-url}")
    private String goodsUrl;

    @Value("${app.run.type}")
    private String env;

    private final RestTemplate restTemplate;

    public GetUserResDto getUser(int userNo) throws HttpServerErrorException {
        ResponseEntity<GetGoodsResDto> response = restTemplate.getForEntity(goodsUrl + userNo, GetGoodsResDto.class);
        log.debug("goods api call response http code: {}", response.getStatusCode());

        if (response.getStatusCode() == HttpStatusCode.valueOf(200)) {
            log.debug("goods api call succeeded.");
            return GetUserResDto.builder()
                    .userNo(userNo)
                    .userName(env + "-" + userNo)
                    .goodsNo(Objects.requireNonNull(response.getBody()).getGoodsNo())
                    .goodsName(response.getBody().getGoodsName())
                    .build();
        } else {
            log.debug("goods api call failed with http status code: {}", response.getStatusCode());
            throw new HttpServerErrorException(response.getStatusCode());
        }
    }
}
