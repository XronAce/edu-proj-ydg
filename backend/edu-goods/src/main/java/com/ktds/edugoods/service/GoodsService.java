package com.ktds.edugoods.service;

import com.ktds.edugoods.dto.response.GetGoodsResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsService {

    @Value("${app.run.type}")
    private String env;

    public GetGoodsResDto getGoods(int goodsNo) {
        return GetGoodsResDto.builder()
                .goodsNo(goodsNo)
                .goodsName("goods-" + env + "-" + goodsNo)
                .build();
    }
}
