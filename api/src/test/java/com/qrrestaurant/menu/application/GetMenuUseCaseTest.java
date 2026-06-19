package com.qrrestaurant.menu.application;
import com.qrrestaurant.menu.application.dto.MenuView;

import com.qrrestaurant.menu.domain.Category;
import com.qrrestaurant.menu.domain.MenuComposition;
import com.qrrestaurant.menu.domain.MenuItem;
import com.qrrestaurant.menu.infrastructure.persistence.category.InMemoryCategoryRepository;
import com.qrrestaurant.menu.infrastructure.persistence.composition.InMemoryMenuCompositionRepository;
import com.qrrestaurant.menu.infrastructure.persistence.item.InMemoryMenuItemRepository;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.infrastructure.persistence.restaurant.InMemoryRestaurantRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GetMenuUseCaseTest {

    @Test
    void shouldOnlyExposeAvailableCompositionItemsPresentInTheRestaurantMenu() {
        UUID restaurantId = UUID.randomUUID();
        UUID sidesCategoryId = UUID.randomUUID();
        UUID burgerId = UUID.randomUUID();

        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        InMemoryMenuItemRepository menuItemRepository = new InMemoryMenuItemRepository();
        InMemoryMenuCompositionRepository compositionRepository = new InMemoryMenuCompositionRepository();

        restaurantRepository.save(restaurant(restaurantId));
        categoryRepository.save(new Category(sidesCategoryId, restaurantId, "Sides", null, 0, false));

        MenuItem availableFries = menuItemRepository.save(MenuItem.from(
                UUID.randomUUID(), sidesCategoryId, "Frites", null, new BigDecimal("3.50"), null, true, null));
        MenuItem unavailableSalad = menuItemRepository.save(MenuItem.from(
                UUID.randomUUID(), sidesCategoryId, "Salade", null, new BigDecimal("4.00"), null, false, null));
        MenuItem menuVariantDrink = menuItemRepository.save(MenuItem.from(
                UUID.randomUUID(), sidesCategoryId, "Menu Coca-Cola", null, new BigDecimal("3.00"), null, true, burgerId));

        compositionRepository.save(MenuComposition.from(
                UUID.randomUUID(), restaurantId, MenuComposition.CompositionType.accompagnement,
                availableFries.getId(), BigDecimal.ZERO));
        compositionRepository.save(MenuComposition.from(
                UUID.randomUUID(), restaurantId, MenuComposition.CompositionType.accompagnement,
                unavailableSalad.getId(), new BigDecimal("1.00")));
        compositionRepository.save(MenuComposition.from(
                UUID.randomUUID(), restaurantId, MenuComposition.CompositionType.accompagnement,
                UUID.randomUUID(), new BigDecimal("2.00")));
        compositionRepository.save(MenuComposition.from(
                UUID.randomUUID(), restaurantId, MenuComposition.CompositionType.boisson,
                menuVariantDrink.getId(), BigDecimal.ZERO));

        GetMenuUseCase useCase = new GetMenuUseCase(
                restaurantRepository, categoryRepository, menuItemRepository, compositionRepository);

        MenuView menu = useCase.execute("naia-burger");

        assertEquals(1, menu.compositions().size());
        assertEquals(availableFries.getId(), menu.compositions().getFirst().menuItemId());
        assertEquals("Frites", menu.compositions().getFirst().menuItemName());
    }

    private Restaurant restaurant(UUID restaurantId) {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setSlug("naia-burger");
        restaurant.setName("Naia Burger");
        restaurant.setThemeId("chaud");
        return restaurant;
    }
}
