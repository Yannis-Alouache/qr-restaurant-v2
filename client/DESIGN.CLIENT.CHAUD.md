---
name: L'Appétit Moderne
colors:
  surface: '#f7f9ff'
  surface-dim: '#d7dadf'
  surface-bright: '#f7f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f1f4f9'
  surface-container: '#ebeef3'
  surface-container-high: '#e5e8ee'
  surface-container-highest: '#e0e3e8'
  on-surface: '#181c20'
  on-surface-variant: '#594138'
  inverse-surface: '#2d3135'
  inverse-on-surface: '#eef1f6'
  outline: '#8d7166'
  outline-variant: '#e1bfb2'
  surface-tint: '#a43d00'
  primary: '#a03b00'
  on-primary: '#ffffff'
  primary-container: '#c94c00'
  on-primary-container: '#fffbff'
  inverse-primary: '#ffb597'
  secondary: '#805600'
  on-secondary: '#ffffff'
  secondary-container: '#ffb636'
  on-secondary-container: '#6e4900'
  tertiary: '#982c96'
  on-tertiary: '#ffffff'
  tertiary-container: '#b547b1'
  on-tertiary-container: '#fffbff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdbcd'
  primary-fixed-dim: '#ffb597'
  on-primary-fixed: '#360f00'
  on-primary-fixed-variant: '#7d2d00'
  secondary-fixed: '#ffddb0'
  secondary-fixed-dim: '#ffba46'
  on-secondary-fixed: '#281800'
  on-secondary-fixed-variant: '#614000'
  tertiary-fixed: '#ffd7f6'
  tertiary-fixed-dim: '#ffaaf4'
  on-tertiary-fixed: '#380038'
  on-tertiary-fixed-variant: '#7e0c7f'
  background: '#f7f9ff'
  on-background: '#181c20'
  surface-variant: '#e0e3e8'
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

## Brand & Style

Le design de ce système repose sur une esthétique **Moderne & Tactile**, conçue pour susciter une réponse émotionnelle immédiate de faim et de convivialité. Le style fusionne la clarté du minimalisme contemporain avec la chaleur du design organique pour une application de commande mobile haut de gamme.

L'identité visuelle cible une clientèle urbaine et active qui valorise autant la qualité du produit que la fluidité de l'expérience numérique. L'interface doit paraître "savoureuse" à travers l'utilisation de surfaces généreuses, de transitions fluides et d'une hiérarchie visuelle qui place la photographie culinaire au centre de l'attention. L'objectif est de transformer l'acte utilitaire de commande en une exploration sensorielle.

## Colors

La palette est délibérément chaude pour stimuler l'appétit et l'énergie.

- **Primaire (Orange Profond) :** Utilisé pour les actions principales (CTA) et les indicateurs d'état critiques. Il évoque la cuisson et la passion.
- **Secondaire (Jaune Chaud) :** Utilisé pour les promotions, les badges de catégorie et les éléments d'accentuation joyeux.
- **Neutre (Charbon Riche) :** Remplace le noir pur pour apporter du contraste et de la lisibilité sans la dureté du #000000. Utilisé pour la typographie et les fonds de sections sombres.
- **Fond de Scène :** Un blanc cassé (#F8F9FA) est privilégié pour adoucir l'interface et faire ressortir les ombres portées.

## Typography

Le système utilise **Plus Jakarta Sans** pour les titres afin d'apporter une touche moderne, géométrique et accueillante. Les graisses lourdes (Bold/ExtraBold) sont privilégiées pour créer un impact visuel fort, rappelant l'affichage des menus de bistrots contemporains.

Pour le corps de texte, **Be Vietnam Pro** offre une lisibilité exceptionnelle sur mobile grâce à ses formes ouvertes et son espacement équilibré. Une attention particulière est portée aux étiquettes de prix, traitées comme des éléments graphiques à part entière pour une clarté immédiate lors de la navigation.

## Layout & Spacing

Le système adopte un modèle de **grille fluide** optimisé pour le mobile (4 colonnes) et la tablette (8 colonnes). L'accent est mis sur des marges généreuses pour éviter toute sensation d'encombrement, permettant aux images de produits de "respirer".

- **Rythme Vertical :** Utilisation d'un système de base de 8px.
- **Zones de Sécurité :** Les éléments interactifs respectent une zone cible minimale de 48px.
- **Hiérarchie :** Les espacements entre les sections de menu sont larges (32px+) pour segmenter visuellement les types de plats, tandis que les éléments internes d'une carte de produit sont serrés (8px) pour maintenir l'unité logique.

## Elevation & Depth

La profondeur est exprimée par des **calques tonaux** et des **ombres ambiantes** très douces. 

- **Surfaces :** Les conteneurs de produits utilisent un fond blanc pur sur un fond d'application gris très clair, créant une séparation naturelle sans bordures lourdes.
- **Ombres :** Utilisation de "Soft Shadows" (0px 8px 24px rgba(33, 37, 41, 0.08)). L'ombre doit paraître diffuse et naturelle, évitant tout aspect synthétique.
- **Interaction :** Lors de l'appui (active state), les éléments subissent une légère réduction d'échelle (0.98) et une diminution de l'ombre pour simuler une pression physique réelle.

## Shapes

Le langage des formes est **Pill-shaped (organique)**. Ce choix renforce l'aspect amical et accessible du service.

- **Boutons :** Entièrement arrondis (capsules) pour une invitation tactile maximale.
- **Cartes de produits :** Coins larges (32px) pour encadrer la nourriture avec douceur.
- **Champs de saisie :** Bordures arrondies harmonisées avec les boutons pour une cohérence visuelle totale.
- **Images :** Les photos de plats peuvent déborder des cadres ou utiliser des masques circulaires pour dynamiser la mise en page.

## Components

### Boutons (CTA)
Le bouton principal est une capsule orange vibrante avec du texte blanc en gras. Les boutons secondaires utilisent un fond jaune ou une bordure fine. L'état "Ajouter au panier" doit être le point focal visuel de chaque écran produit.

### Cartes de Produit
Conçues avec une image de haute qualité occupant la partie supérieure (ou le côté gauche), suivies du nom du plat en `title-md` et du prix en `price-tag`. Une ombre portée légère détache la carte du fond.

### Chips & Catégories
Petites capsules horizontales défilantes. La sélection est marquée par un passage du fond neutre au fond primaire (orange), avec un changement de couleur de texte pour garantir le contraste.

### Champs de Saisie
Arrière-plan gris très clair avec une icône discrète à gauche. Lors de la saisie, la bordure s'illumine en orange primaire.

### Barre de Panier Flottante
Un composant persistant en bas de l'écran, utilisant un flou d'arrière-plan (glassmorphism léger) ou une couleur pleine contrastée, affichant le nombre d'articles et le total pour un accès rapide au paiement.