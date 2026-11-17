package com.fatenexx.backend.product.dto;

import java.util.List;

public record ProductListResponse(List<MembershipProductResponse> membershipProducts,
                                  List<PtProductResponse> ptProducts) {}