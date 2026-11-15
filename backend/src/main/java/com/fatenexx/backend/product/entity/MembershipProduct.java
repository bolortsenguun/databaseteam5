package com.fatenexx.backend.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "membership_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MembershipProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "months", nullable = false)
    private Integer months;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "is_active", nullable = false)
    private boolean active;             // 판매 중 / 판매 중지 (삭제 대신)

    public static MembershipProduct create(String name, int months, int price) {
        MembershipProduct p = new MembershipProduct();
        p.name = name;
        p.months = months;
        p.price = price;
        p.active = true;
        return p;
    }

    public void update(String name, int months, int price, boolean active) {
        this.name = name;
        this.months = months;
        this.price = price;
        this.active = active;
    }
}