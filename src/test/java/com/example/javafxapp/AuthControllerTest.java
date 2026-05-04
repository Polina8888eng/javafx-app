package com.example.javafxapp;

import com.example.App;
import com.example.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import javafx.stage.Stage;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testUserCreation() {
        User user = new User(1, "testuser", "hash");
        assertEquals(1, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("hash", user.getPasswordHash());
        assertNotNull(user.getRegisteredAt());
        assertEquals(User.SubscriptionPlan.FREE, user.getSubscriptionPlan());
        assertNull(user.getSubscriptionExpiryDate());
    }

    @Test
    void testIsSubscriptionActive() {
        User user = new User(1, "test", "hash");

        user.setSubscriptionPlan(User.SubscriptionPlan.FREE);
        assertTrue(user.isSubscriptionActive());

        user.setSubscriptionPlan(User.SubscriptionPlan.BASIC);
        user.setSubscriptionExpiryDate(LocalDate.now().plusDays(30));
        assertTrue(user.isSubscriptionActive());

        user.setSubscriptionExpiryDate(LocalDate.now().minusDays(1));
        assertFalse(user.isSubscriptionActive());

        user.setSubscriptionPlan(User.SubscriptionPlan.VIP);
        user.setSubscriptionExpiryDate(LocalDate.now().plusDays(30));
        assertTrue(user.isSubscriptionActive());

        user.setSubscriptionExpiryDate(LocalDate.now().minusDays(1));
        assertFalse(user.isSubscriptionActive());
    }

    @Test
    void testUpgradeAndDowngrade() {
        User user = new User(1, "test", "hash");

        user.upgradeToBasic();
        assertEquals(User.SubscriptionPlan.BASIC, user.getSubscriptionPlan());
        assertNotNull(user.getSubscriptionExpiryDate());
        assertTrue(user.getSubscriptionExpiryDate().isAfter(LocalDate.now()));

        user.upgradeToVip();
        assertEquals(User.SubscriptionPlan.VIP, user.getSubscriptionPlan());
        assertNotNull(user.getSubscriptionExpiryDate());
        assertTrue(user.getSubscriptionExpiryDate().isAfter(LocalDate.now()));

        user.downgradeToFree();
        assertEquals(User.SubscriptionPlan.FREE, user.getSubscriptionPlan());
        assertNull(user.getSubscriptionExpiryDate());
        assertTrue(user.isSubscriptionActive());
    }

    @Test
    void testGetDaysUntilExpiry() {
        User user = new User(1, "test", "hash");

        assertEquals(-1, user.getDaysUntilExpiry());

        user.upgradeToBasic();
        long daysFuture = user.getDaysUntilExpiry();
        System.out.println("Days future: " + daysFuture);
        assertTrue(daysFuture > 0);

        user.setSubscriptionExpiryDate(LocalDate.now().minusDays(5));
        System.out.println("Plan after set: " + user.getSubscriptionPlan());
        System.out.println("Expiry date: " + user.getSubscriptionExpiryDate());
        long daysPast = user.getDaysUntilExpiry();
        System.out.println("Days past: " + daysPast);

        assertTrue(daysPast < 0, "Expected negative days, but got: " + daysPast);

        user.setSubscriptionExpiryDate(null);
        assertEquals(-1, user.getDaysUntilExpiry());
    }




    @Test
    void testToString() {
        User user = new User(1, "test", "hash");
        user.upgradeToVip();
        String str = user.toString();
        assertTrue(str.contains("id=1"));
        assertTrue(str.contains("username='test'"));
        assertTrue(str.contains("subscriptionPlan=VIP"));
        assertTrue(str.contains("isActive=true"));
    }
}
