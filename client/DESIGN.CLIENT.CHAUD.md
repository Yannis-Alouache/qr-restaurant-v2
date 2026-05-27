---
name: Feu Gourmand
colors:
  surface: '#fff4ea'
  surface-dim: '#efd5bf'
  surface-bright: '#fffaf5'
  surface-container-lowest: '#fffaf5'
  surface-container-low: '#fff1e4'
  surface-container: '#ffe6d2'
  surface-container-high: '#ffd8bd'
  surface-container-highest: '#f5c7aa'
  on-surface: '#2b170d'
  on-surface-variant: '#7a5646'
  inverse-surface: '#3a2116'
  inverse-on-surface: '#fff1e6'
  outline: '#c99983'
  outline-variant: '#efccb7'
  surface-tint: '#cb4f14'
  primary: '#cb4f14'
  on-primary: '#ffffff'
  primary-container: '#ffd8c1'
  on-primary-container: '#501f08'
  inverse-primary: '#ffb28d'
  secondary: '#d88a14'
  on-secondary: '#ffffff'
  secondary-container: '#ffe4a7'
  on-secondary-container: '#593700'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 40px
    fontWeight: '800'
    lineHeight: 48px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
  title-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Be Vietnam Pro
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Be Vietnam Pro
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-bold:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '700'
    lineHeight: 16px
  price-tag:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '800'
    lineHeight: 24px
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  container-margin: 20px
  stack-gap-lg: 32px
  stack-gap-md: 16px
  stack-gap-sm: 8px
  section-padding: 40px
  grid-gutter: 16px
---

## Intention

Le thème **CHAUD** doit immédiatement évoquer la cuisson, l'appétit et la générosité. C'est la palette la plus démonstrative : elle doit donner une sensation de commande gourmande, presque "snacking premium", sans jamais tomber dans l'effet cheap ou agressif.

## Socle UX commun

Le thème ne change **jamais** la structure d'interface :

- hero mobile compact et premium
- grille de catégories visuelles en **2 à 3 colonnes**
- carte "spotlight" pour la catégorie active
- cartes produits immersives avec image large
- bottom sheets tactiles pour détail produit / composition menu / panier
- barre panier flottante persistante

Ce socle est partagé avec `classique`, `nature` et `elegant`. Ici, seule la palette transforme l'ambiance.

## Palette & ambiance

- **Primaire — Orange braise (`#CB4F14`)** : couleur d'action, CTA, prix et états majeurs.
- **Secondaire — Ambre gourmand (`#D88A14`)** : chaleur lumineuse pour badges, accents de menu, contrepoints joyeux.
- **Surfaces — Crèmes abricotées** : les fonds doivent rester clairs, riches et chaleureux, jamais bleutés.
- **Contrastes — Brun grillé** : remplace le noir pur pour garder de la profondeur sans durcir l'écran.

L'écran doit évoquer une borne de commande premium dans un univers burger / street-food haut de gamme : chaleureux, immédiat, généreux, presque "caramélisé".

## Usage des couleurs

- **Hero / surfaces de tête** : dégradés pêche, orange laiteux, ambre léger.
- **Catégorie active** : renforcement franc de l'orange braise avec halo discret.
- **Prix et CTA** : orange braise ou dégradé orange + ambre.
- **Badges formule** : fond ambre clair avec texte brun profond.
- **Cartes inactives** : surfaces crème très claires avec bordures sable.

## Contrastes

- Les textes principaux restent sur des fonds clairs et doivent conserver une lisibilité immédiate.
- Les accents chauds doivent rester denses pour que le thème soit identifiable en un coup d'oeil.
- Le contraste ne doit pas venir d'un noir dur, mais de bruns toastés et de surfaces ivoire épaisses.

## Matière visuelle

- ombres douces et larges, jamais métalliques
- surfaces rondes, tactiles, "moussées"
- photos culinaires mises en avant avec scrims subtils
- capsules et cartes avec présence tactile forte

## Composants clés

### Catégories
Vraies cartes visuelles dans une grille dense mobile. L'image est prioritaire si disponible, le nom reste très lisible, l'état actif doit se voir immédiatement.

### Produits
Cartes généreuses, immersives, avec image large, prix lisible très vite, et affordance évidente pour ouvrir la fiche.

### Panier flottant
Présence forte, contrastée, presque "CTA global", avec une sensation de décision immédiate.
