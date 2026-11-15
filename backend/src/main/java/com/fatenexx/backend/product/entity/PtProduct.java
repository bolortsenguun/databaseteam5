package com.fatenexx.backend.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pt_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PtProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Convert(converter = PtTypeConverter.class)
    @Column(name = "pt_type", nullable = false, length = 10)
    private PtType ptType;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount;         // 10, 20, 30회

    @Column(name = "valid_weeks", nullable = false)
    private Integer validWeeks;         // 12, 24, 36주

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public static PtProduct create(PtType ptType, String name, int totalCount, int validWeeks, int price) {
        PtProduct p = new PtProduct();
        p.ptType = ptType;
        p.name = name;
        p.totalCount = totalCount;
        p.validWeeks = validWeeks;
        p.price = price;
        p.active = true;
        return p;
    }

    public void update(String name, int totalCount, int validWeeks, int price, boolean active) {
        this.name = name;
        this.totalCount = totalCount;
        this.validWeeks = validWeeks;
        this.price = price;
        this.active = active;
    }
}