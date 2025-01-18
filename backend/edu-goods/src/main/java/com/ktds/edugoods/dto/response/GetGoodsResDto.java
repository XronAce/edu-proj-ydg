package com.ktds.edugoods.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetGoodsResDto {
    private int goodsNo;
    private String goodsName;
}
