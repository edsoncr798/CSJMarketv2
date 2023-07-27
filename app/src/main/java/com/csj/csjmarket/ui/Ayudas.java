package com.csj.csjmarket.ui;

public class Ayudas {
    public static String capitalize(String string) {
        String[] palabras = string.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String palabra : palabras) {
            if (!palabra.isEmpty()) {
                sb.append(Character.toUpperCase(palabra.charAt(0)));
                if (palabra.length() > 1) {
                    sb.append(palabra.substring(1).toLowerCase());
                }
                sb.append(" ");
            }
        }

        return sb.toString();
    }
}
