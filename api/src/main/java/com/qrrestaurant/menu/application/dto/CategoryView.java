package com.qrrestaurant.menu.application.dto;

public record CategoryView(
        String id,
        String name,
        String imagePath,
        Integer position,
        boolean hasMenu
) {}
