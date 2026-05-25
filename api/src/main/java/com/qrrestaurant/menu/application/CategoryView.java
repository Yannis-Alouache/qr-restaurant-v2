package com.qrrestaurant.menu.application;

public record CategoryView(
        String id,
        String name,
        String imagePath,
        Integer position,
        boolean hasMenu
) {}
