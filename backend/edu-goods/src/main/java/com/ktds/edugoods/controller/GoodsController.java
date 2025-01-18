package com.ktds.edugoods.controller;

import com.ktds.edugoods.dto.response.GetGoodsResDto;
import com.ktds.edugoods.service.GoodsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/goods")
@RestController
public class GoodsController {
    private final GoodsService goodsService;

    @GetMapping("/{goodsNo}")
    public ResponseEntity<GetGoodsResDto> getGoods(@PathVariable int goodsNo) {
        try {
            GetGoodsResDto getGoodsResDto = goodsService.getGoods(goodsNo);
            return new ResponseEntity<>(getGoodsResDto, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
