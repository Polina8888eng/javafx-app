package com.example.javafxapp;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

class EditorControllerUtilsTest {

    private EditorController controller;
    private Method escapeRegex;
    private Method escapeJS;
    private Method escapeHtml;

    @BeforeEach
    void setUp() throws Exception {
        controller = new EditorController();
        escapeRegex = EditorController.class.getDeclaredMethod("escapeRegex", String.class);
        escapeRegex.setAccessible(true);
        escapeJS = EditorController.class.getDeclaredMethod("escapeJS", String.class);
        escapeJS.setAccessible(true);
        escapeHtml = EditorController.class.getDeclaredMethod("escapeHtml", String.class);
        escapeHtml.setAccessible(true);
    }

    @Test
    void testEscapeRegex() throws Exception {
        assertEquals("2\\+2", escapeRegex.invoke(controller, "2+2"));
        assertEquals("\\.", escapeRegex.invoke(controller, "."));
        assertEquals("\\[\\]", escapeRegex.invoke(controller, "[]"));
        assertEquals("\\\\", escapeRegex.invoke(controller, "\\"));
        assertEquals("a\\*b", escapeRegex.invoke(controller, "a*b"));
        assertEquals("hello world", escapeRegex.invoke(controller, "hello world"));
        assertEquals("\\^\\$", escapeRegex.invoke(controller, "^$"));
        assertEquals("\\(test\\)", escapeRegex.invoke(controller, "(test)"));
        assertEquals("\\|", escapeRegex.invoke(controller, "|"));
    }

    @Test
    void testEscapeJS() throws Exception {
        assertEquals("It\\'s a pen", escapeJS.invoke(controller, "It's a pen"));
        assertEquals("Line1\\nLine2", escapeJS.invoke(controller, "Line1\nLine2"));
        assertEquals("C:\\\\path", escapeJS.invoke(controller, "C:\\path"));
        assertEquals("\\\"quotes\\\"", escapeJS.invoke(controller, "\"quotes\""));
        assertEquals("Hello\\r\\nWorld", escapeJS.invoke(controller, "Hello\r\nWorld"));
    }

    @Test
    void testEscapeHtml() throws Exception {
        assertEquals("&lt;div&gt;", escapeHtml.invoke(controller, "<div>"));
        assertEquals("&amp;", escapeHtml.invoke(controller, "&"));
        assertEquals("&quot;", escapeHtml.invoke(controller, "\""));
        assertEquals("&#39;", escapeHtml.invoke(controller, "'"));
        assertEquals("hello<br>world", escapeHtml.invoke(controller, "hello\nworld"));
        assertEquals("a &amp; b", escapeHtml.invoke(controller, "a & b"));
    }
}
