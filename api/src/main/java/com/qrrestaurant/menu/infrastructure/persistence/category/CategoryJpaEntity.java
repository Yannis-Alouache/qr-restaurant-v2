package com.qrrestaurant.menu.infrastructure.persistence.category;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "category")
public class CategoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "restaurant_id", nullable = false)
    private UUID restaurantId;

    @Column(nullable = false)
    private String name;

    @Column(name = "image_path")
    private String imagePath;

    @Column(nullable = false)
    private Integer position = 0;

    @Column(name = "has_menu", nullable = false)
    private boolean hasMenu = false;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRestaurantId() { return restaurantId; }
    public void setRestaurantId(UUID restaurantId) { this.restaurantId = restaurantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public boolean isHasMenu() { return hasMenu; }
    public void setHasMenu(boolean hasMenu) { this.hasMenu = hasMenu; }
}
