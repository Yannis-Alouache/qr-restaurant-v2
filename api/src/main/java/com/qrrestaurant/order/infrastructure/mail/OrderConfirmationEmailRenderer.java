package com.qrrestaurant.order.infrastructure.mail;

import com.qrrestaurant.order.domain.OrderConfirmationEmail;
import com.qrrestaurant.restaurant.domain.RestaurantTheme;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Rendu HTML du reçu « ticket de caisse » (maquette validée) : 440 px,
 * monospace, sans image critique, dark mode. Le papier et les neutres sont
 * fixes ; seule la palette d'accent suit le thème du restaurateur (mêmes
 * tokens que l'app client, convertis oklch → sRGB — les clients mail ne
 * supportent pas oklch).
 */
@Component
public class OrderConfirmationEmailRenderer {

    private static final ZoneId RESTAURANT_ZONE = ZoneId.of("Europe/Paris");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH'h'mm", Locale.FRENCH).withZone(RESTAURANT_ZONE);
    private static final String TICKET_FONT = "'Courier New',Courier,monospace";

    // Neutres du ticket, identiques quel que soit le thème.
    private static final String BG = "#F9FAFB";
    private static final String INK = "#1F272E";
    private static final String MUTED = "#6E767D";
    private static final String BORDER = "#E4E7E9";
    private static final String SUCCESS_INK = "#26663E";

    // accent = fonds décoratifs (monogramme, focus) ; strong = textes colorés
    // et bouton (contraste AA sur blanc) ; strongHover = survol du bouton.
    private static final Map<String, Palette> PALETTES = Map.of(
            "classique", new Palette("#008A6A", "#007650", "#006244"),
            "chaud", new Palette("#DD503F", "#C21725", "#9E121E"),
            "nature", new Palette("#48773E", "#1A6323", "#12491B"),
            "elegant", new Palette("#643185", "#580C72", "#430859"));

    private record Palette(String accent, String strong, String strongHover) {}

    public String render(OrderConfirmationEmail email) {
        Palette palette = paletteOf(email.themeId());
        String ref = email.orderReference();
        return TEMPLATE
                .replace("__ACCENT__", palette.accent())
                .replace("__STRONG_HOVER__", palette.strongHover())
                .replace("__STRONG__", palette.strong())
                .replace("__BG__", BG)
                .replace("__INK__", INK)
                .replace("__MUTED__", MUTED)
                .replace("__BORDER__", BORDER)
                .replace("__SUCCESS_INK__", SUCCESS_INK)
                .replace("__PREHEADER__", escapeHtml(preheader(email)))
                .replace("__MONOGRAM__", escapeHtml(monogram(email.restaurantName())))
                .replace("__NAME__", escapeHtml(displayName(email.restaurantName())))
                .replace("__TITLE__", escapeHtml("Votre commande est confirmée · " + email.restaurantName()))
                .replace("__META_ROWS__", metaRows(email, ref, palette))
                .replace("__ITEM_ROWS__", itemRows(email))
                .replace("__TOTAL__", money(email.total()))
                .replace("__TRACKING_URL__", escapeHtml(email.trackingUrl()))
                .replace("__ADDRESS_LINE__", addressLine(email))
                .replace("__YEAR__", String.valueOf(LocalDate.now(RESTAURANT_ZONE).getYear()))
                .replace("__REF__", escapeHtml(ref));
    }

    private static Palette paletteOf(String themeId) {
        try {
            return PALETTES.getOrDefault(RestaurantTheme.normalizeOrDefault(themeId), PALETTES.get(RestaurantTheme.DEFAULT));
        } catch (RuntimeException e) {
            // Valeur inattendue en base : on reste lisible plutôt qu'illisible.
            return PALETTES.get(RestaurantTheme.DEFAULT);
        }
    }

    private String preheader(OrderConfirmationEmail email) {
        String base = "Merci ! Votre commande " + email.orderReference();
        if (email.tableNumber() != null) {
            base += " (Table " + email.tableNumber() + ")";
        }
        return base + " est confirmée et transmise en cuisine.";
    }

    private String metaRows(OrderConfirmationEmail email, String ref, Palette palette) {
        StringJoiner rows = new StringJoiner("\n");
        rows.add(metaRow("COMMANDE", ref, palette.strong()));
        if (email.tableNumber() != null) {
            rows.add(metaRow("TABLE", String.valueOf(email.tableNumber()), INK));
        }
        rows.add(metaRow("DATE", DATE_FORMAT.format(email.paidAt()), INK));
        return rows.toString();
    }

    private String metaRow(String label, String value, String valueColor) {
        return """
                <tr>
                  <td style="padding:3px 0;color:%s;letter-spacing:1px;white-space:nowrap;font-family:%s;font-size:13px;">%s</td>
                  <td width="100%%" style="padding:3px 5px;border-bottom:1px dotted %s;">&nbsp;</td>
                  <td align="right" style="padding:3px 0;font-weight:700;color:%s;white-space:nowrap;font-family:%s;font-size:13px;">%s</td>
                </tr>
                """.formatted(MUTED, TICKET_FONT, label, BORDER, valueColor, TICKET_FONT, escapeHtml(value));
    }

    private String itemRows(OrderConfirmationEmail email) {
        StringJoiner rows = new StringJoiner("\n");
        for (OrderConfirmationEmail.Line line : email.items()) {
            String unitSubtitle = line.quantity() > 1
                    ? "<div style=\"font-size:12px;font-weight:400;color:" + MUTED + ";padding-top:2px;\">"
                            + money(line.unitPrice()) + " l'unité</div>"
                    : "";
            BigDecimal lineAmount = line.unitPrice()
                    .multiply(BigDecimal.valueOf(line.quantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            rows.add("""
                    <tr>
                      <td width="38" valign="top" style="padding:10px 0;font-size:15px;color:%s;font-family:%s;">%d&times;</td>
                      <td valign="top" style="padding:10px 0;font-size:15px;font-weight:700;color:%s;font-family:%s;">%s%s</td>
                      <td align="right" valign="top" style="padding:10px 0;font-size:15px;color:%s;white-space:nowrap;font-family:%s;">%s</td>
                    </tr>
                    """.formatted(INK, TICKET_FONT, line.quantity(),
                    INK, TICKET_FONT, escapeHtml(line.name()), unitSubtitle,
                    INK, TICKET_FONT, money(lineAmount)));
        }
        return rows.toString();
    }

    private String addressLine(OrderConfirmationEmail email) {
        if (email.restaurantAddress() == null || email.restaurantAddress().isBlank()) {
            return "";
        }
        return "<br>" + escapeHtml(email.restaurantAddress());
    }

    private static final Set<String> FRENCH_ARTICLES =
            Set.of("le", "la", "les", "l", "du", "de", "des", "d", "au", "aux", "a", "en");

    /** Initiales du monogramme, articles français ignorés : « Le Comptoir du Marché » → CM. */
    static String monogram(String restaurantName) {
        if (restaurantName == null || restaurantName.isBlank()) {
            return "QR";
        }
        StringJoiner initials = new StringJoiner("");
        for (String word : restaurantName.trim().toLowerCase(Locale.FRENCH).split("[\\s'’-]+")) {
            if (initials.length() >= 2) {
                break;
            }
            if (word.isEmpty() || FRENCH_ARTICLES.contains(word)) {
                continue;
            }
            initials.add(word.substring(0, 1).toUpperCase(Locale.FRENCH));
        }
        if (initials.length() == 0) {
            return restaurantName.trim().substring(0, 1).toUpperCase(Locale.FRENCH);
        }
        return initials.toString();
    }

    private static String displayName(String restaurantName) {
        return restaurantName == null ? "" : restaurantName.trim().toUpperCase(Locale.FRENCH);
    }

    private static String money(BigDecimal amount) {
        return String.format(Locale.FRANCE, "%,.2f\u00A0€", amount);
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static final String TEMPLATE = """
            <!DOCTYPE html>
            <html lang="fr" xmlns:v="urn:schemas-microsoft-com:vml" xmlns:o="urn:schemas-microsoft-com:office:office">
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <meta http-equiv="X-UA-Compatible" content="IE=edge">
            <meta name="color-scheme" content="light dark">
            <meta name="supported-color-schemes" content="light dark">
            <meta name="format-detection" content="telephone=no, date=no, address=no">
            <title>__TITLE__</title>
            <style type="text/css">
            /* ==============================================================
               REÇU « TICKET » — palette d'accent = thème du restaurant
               (mêmes tokens que l'app client, convertis oklch → sRGB).
               Papier et neutres fixes, quel que soit le thème.
               ============================================================== */
            :root { color-scheme: light dark; supported-color-schemes: light dark; }
            html, body { margin:0 !important; padding:0 !important; height:100% !important; width:100% !important; }
            table { border-collapse:collapse; mso-table-lspace:0pt; mso-table-rspace:0pt; }
            td { mso-line-height-rule:exactly; }
            a { text-decoration:none; }
            img { border:0; outline:none; text-decoration:none; }
            /* — Mobile : le ticket reste étroit, les marges extérieures se resserrent — */
            @media only screen and (max-width:600px) {
              .es-outer { padding:12px 8px !important; }
              .es-ticket { width:100% !important; border-radius:14px !important; }
              .es-pad { padding-left:18px !important; padding-right:18px !important; }
              .es-btn-a { display:block !important; text-align:center !important; }
            }
            /* — Dark mode : fond de page sombre, le papier du ticket reste blanc — */
            @media (prefers-color-scheme: dark) {
              .es-bg { background-color:#131A20 !important; }
              .es-f { color:#9AA4AC !important; }
            }
            [data-ogsc] .es-bg { background-color:#131A20 !important; }
            [data-ogsc] .es-f { color:#9AA4AC !important; }
            /* — États interactifs (clients mail qui appliquent :hover/:focus-visible ;
                 contraste accru au survol, jamais réduit) — */
            .es-btn-a:hover { background-color:__STRONG_HOVER__ !important; }
            .es-btn-a:focus-visible { outline:2px solid __ACCENT__ !important; outline-offset:2px !important; }
            </style>
            <!--[if mso]><xml><o:OfficeDocumentSettings><o:PixelsToInch>96</o:PixelsToInch></o:OfficeDocumentSettings></xml><![endif]-->
            </head>
            <body style="margin:0;padding:0;background-color:__BG__;">

            <!-- Texte de prévisualisation (inbox) -->
            <div style="display:none;font-size:1px;line-height:1px;max-height:0;max-width:0;opacity:0;overflow:hidden;mso-hide:all;">__PREHEADER__&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;</div>

            <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%" class="es-bg" style="background-color:__BG__;">
            <tr>
            <td align="center" class="es-outer" style="padding:28px 16px;">

              <!-- ============ Ticket 440 px ============ -->
              <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="440" class="es-ticket" style="width:100%;max-width:440px;background-color:#FFFFFF;border:1px solid __BORDER__;border-radius:14px;font-family:'Courier New',Courier,monospace;">

                <!-- Header : monogramme (aucune image : les clients mail les bloquent) + nom -->
                <tr>
                  <td class="es-pad" align="center" style="padding:28px 24px 16px 24px;">
                    <div aria-hidden="true" style="width:40px;height:40px;background-color:__ACCENT__;border-radius:14px;font-family:'Courier New',Courier,monospace;font-size:15px;font-weight:700;color:#FFFFFF;line-height:40px;text-align:center;margin:0 auto 10px auto;">__MONOGRAM__</div>
                    <div style="font-family:'Courier New',Courier,monospace;font-size:16px;font-weight:700;letter-spacing:2px;color:__INK__;text-transform:uppercase;">__NAME__</div>
                    <div style="font-family:'Courier New',Courier,monospace;font-size:11px;letter-spacing:3px;color:__MUTED__;padding-top:5px;">RE&Ccedil;U DE COMMANDE</div>
                  </td>
                </tr>

                <!-- Filet pointillé -->
                <tr><td class="es-pad" style="padding:0 24px;"><div style="border-top:1px dashed __BORDER__;font-size:0;line-height:1px;">&nbsp;</div></td></tr>

                <!-- Bandeau de confirmation -->
                <tr>
                  <td class="es-pad" align="center" style="padding:16px 24px 6px 24px;">
                    <div style="font-family:'Courier New',Courier,monospace;font-size:15px;font-weight:700;letter-spacing:1px;color:__SUCCESS_INK__;">PAIEMENT CONFIRM&Eacute; &#10003;</div>
                    <div style="font-family:'Courier New',Courier,monospace;font-size:26px;font-weight:700;color:__INK__;padding-top:4px;">__TOTAL__</div>
                    <div style="font-family:'Courier New',Courier,monospace;font-size:12px;letter-spacing:1px;color:__MUTED__;padding-top:2px;">PAY&Eacute; PAR CARTE</div>
                  </td>
                </tr>

                <!-- Référence · table · date -->
                <tr>
                  <td class="es-pad" style="padding:14px 24px 0 24px;">
                    <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%" style="font-family:'Courier New',Courier,monospace;font-size:13px;color:__INK__;">
                    __META_ROWS__
                    </table>
                  </td>
                </tr>

                <!-- Filet pointillé -->
                <tr><td class="es-pad" style="padding:14px 24px 0 24px;"><div style="border-top:1px dashed __BORDER__;font-size:0;line-height:1px;">&nbsp;</div></td></tr>

                <!-- Détail de la commande -->
                <tr>
                  <td class="es-pad" style="padding:12px 24px 0 24px;">
                    <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%" style="font-family:'Courier New',Courier,monospace;">
                    <tr>
                      <td width="38" style="padding:4px 0 8px 0;font-size:11px;letter-spacing:2px;color:__MUTED__;border-bottom:1px dashed __BORDER__;">QT&Eacute;</td>
                      <td style="padding:4px 0 8px 0;font-size:11px;letter-spacing:2px;color:__MUTED__;border-bottom:1px dashed __BORDER__;">ARTICLE</td>
                      <td align="right" style="padding:4px 0 8px 0;font-size:11px;letter-spacing:2px;color:__MUTED__;border-bottom:1px dashed __BORDER__;">MONTANT</td>
                    </tr>
                    __ITEM_ROWS__
                    </table>
                  </td>
                </tr>

                <!-- Total -->
                <tr>
                  <td class="es-pad" style="padding:0 24px;">
                    <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%" style="font-family:'Courier New',Courier,monospace;border-top:1px dashed __BORDER__;">
                    <tr>
                      <td style="padding:12px 0 4px 0;font-size:15px;font-weight:700;letter-spacing:1.5px;color:__INK__;">TOTAL (CARTE)</td>
                      <td align="right" style="padding:12px 0 4px 0;font-size:18px;font-weight:700;color:__INK__;white-space:nowrap;">__TOTAL__</td>
                    </tr>
                    </table>
                  </td>
                </tr>

                <!-- Filet pointillé -->
                <tr><td class="es-pad" style="padding:10px 24px 0 24px;"><div style="border-top:1px dashed __BORDER__;font-size:0;line-height:1px;">&nbsp;</div></td></tr>

                <!-- Code-barres décoratif (CSS pur ; sans background-image, bande invisible : dégradation propre) + référence -->
                <tr>
                  <td class="es-pad" align="center" style="padding:16px 24px 0 24px;">
                    <div aria-hidden="true" style="width:188px;height:34px;margin:0 auto;background-color:#FFFFFF;background-image:repeating-linear-gradient(90deg,__INK__ 0 2px,transparent 2px 6px,__INK__ 6px 9px,transparent 9px 11px,__INK__ 11px 16px,transparent 16px 18px,__INK__ 18px 20px,transparent 20px 25px);"></div>
                    <div style="font-family:'Courier New',Courier,monospace;font-size:13px;font-weight:700;letter-spacing:4px;color:__INK__;padding-top:6px;">#__REF__</div>
                  </td>
                </tr>

                <!-- Bouton principal (bulletproof : fond coloré + padding) -->
                <tr>
                  <td class="es-pad" style="padding:20px 24px 4px 24px;">
                    <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%">
                    <tr>
                      <td align="center" bgcolor="__STRONG__" style="background-color:__STRONG__;border-radius:14px;">
                        <a href="__TRACKING_URL__" class="es-btn-a" style="display:block;padding:15px 12px;font-family:'Courier New',Courier,monospace;font-size:15px;font-weight:700;color:#FFFFFF;text-decoration:none;border-radius:14px;">Suivre ma commande en direct</a>
                      </td>
                    </tr>
                    </table>
                  </td>
                </tr>

                <!-- Note rassurante -->
                <tr>
                  <td class="es-pad" align="center" style="padding:14px 24px 22px 24px;font-family:'Courier New',Courier,monospace;font-size:12px;line-height:19px;color:__MUTED__;">
                    Votre commande a &eacute;t&eacute; transmise en cuisine.<br>Conservez cet email, il contient votre r&eacute;f&eacute;rence de commande.
                  </td>
                </tr>

                <!-- Filet pointillé bas -->
                <tr><td class="es-pad" style="padding:0 24px 18px 24px;"><div style="border-top:1px dashed __BORDER__;font-size:0;line-height:1px;">&nbsp;</div></td></tr>

              </table>
              <!-- ============ /Ticket ============ -->

              <!-- Footer -->
              <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="440" class="es-ticket" style="width:100%;max-width:440px;">
              <tr>
                <td align="center" class="es-f" style="padding:20px 20px 8px 20px;font-family:'Avenir Next',-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif;font-size:13px;line-height:20px;color:__MUTED__;">
                  Vous recevez cet email car vous<br>avez pass&eacute; commande au restaurant.<br>
                  Email transactionnel, sans inscription.__ADDRESS_LINE__
                  <span style="font-size:12px;">&copy; __YEAR__ __NAME__ &middot; Tous droits r&eacute;serv&eacute;s.</span>
                </td>
              </tr>
              </table>

            </td>
            </tr>
            </table>
            </body>
            </html>
            """;
}
