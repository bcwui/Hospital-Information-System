package com.project.his.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillItemVO {

    private Long itemId;

    private String itemType;

    private String itemTypeDesc;

    private String itemName;

    private BigDecimal unitPrice;

    private Integer quantity;

    private BigDecimal amount;

    private Long relatedId;

}
