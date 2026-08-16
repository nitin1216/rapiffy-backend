package com.example.rapiffy.common;

public final class RapiffyBrand {

    private RapiffyBrand() {}

    /**
     * Single source of truth for the Rapiffy logo used in PDF invoices.
     * Matches the app's text logo: "Rap" + orange "i" + "ffy" with orange underline.
     */
    public static final String LOGO_HTML =
        "<span class=\"app-name\">Rap<span style=\"color:#D2691E;\">i</span>ffy</span>" +
        "<div style=\"margin-left:17px;width:10px;height:3px;background-color:#D2691E;margin-top:4px;\"></div>";
}
