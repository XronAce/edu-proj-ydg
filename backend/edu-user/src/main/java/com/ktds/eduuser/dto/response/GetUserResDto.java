package com.ktds.eduuser.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetUserResDto {
    private int userNo;
    private String userName;
    private int goodsNo;
    private String goodsName;
}
