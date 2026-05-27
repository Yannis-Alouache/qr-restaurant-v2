# Plan — refonte premium du menu client mobile

## Objectif

Refondre en profondeur l'UI/UX du menu client mobile pour obtenir une sensation de borne de commande premium sur smartphone, tout en gardant une base UX commune entre thèmes et en rendant les palettes réellement distinctes.

## Audit initial

### Référentiel design
- `client/DESIGN.CLIENT.CHAUD.md` a été lu en entier.
- La direction utile à conserver est claire : mobile-first, tactile, organique, généreux, visuel, avec une forte mise en valeur des visuels et une hiérarchie immédiate.
- Le document CHAUD actuel n'est plus totalement aligné avec le besoin produit sur les catégories, car il mentionne encore des chips horizontales alors que le besoin impose une grille visuelle mobile.

### Thèmes
- `client/src/app/shared/services/theme.service.ts` applique les thèmes via `data-theme`.
- `client/src/styles.scss` centralise les variables de palette.
- Le thème `chaud` n'est pas suffisamment chaud aujourd'hui : trop de surfaces froides / gris bleutés, donc l'ambiance n'évoque pas l'intention "appétit / chaleur / premium tactile".
- Les thèmes `classique`, `nature`, `elegant` existent techniquement dans les tokens CSS, mais sans documentation dédiée, et leur différenciation visuelle reste trop faible.

### UI actuelle
- `menu-page.component.ts` pose une structure correcte mais trop générique et peu incarnée.
- `category-grid.component.ts` affiche seulement une ligne de pills horizontales : c'est l'écart principal avec le besoin.
- `item-card.component.ts` utilise bien `item.imagePath`, mais dans une vignette trop petite pour créer un effet premium.
- `item-modal.component.ts` et les surfaces associées sont propres mais encore trop neutres.
- Le parcours donne une impression de page mobile simple, pas d'interface de commande immersive.

### Images
- `category.imagePath` n'est actuellement pas exploité dans la grille de catégories.
- `item.imagePath` est exploité, mais insuffisamment mis en avant.
- Le flux client public ne normalise pas les URLs d'assets. Si une URL relative sans `/` est renvoyée, elle peut casser sur la route imbriquée `/menu/:slug/:tableId`.
- Les données seed locales ne contiennent pas d'images, donc l'audit visuel des vrais médias doit être sécurisé côté code plus que démontré par le seed.

## Décisions de mise en oeuvre

1. **Même socle UX pour tous les thèmes**
   - même structure
   - mêmes composants
   - même logique d'interaction
   - seule l'ambiance colorielle change fortement

2. **Catégories en vraie grille mobile**
   - 3 cartes par ligne quand l'espace le permet
   - sinon 2
   - jamais une simple rangée scrollable comme rendu principal

3. **Produits plus immersifs**
   - cartes verticales plus généreuses
   - image large et visible
   - hiérarchie forte sur nom / description / prix / action

4. **Traitement racine des images**
   - normalisation des URLs côté client
   - exploitation réelle de `category.imagePath` et `item.imagePath`
   - pas de faux correctif cosmétique

5. **Palettes revues**
   - CHAUD devient réellement gourmand et chaleureux
   - CLASSIQUE plus bistrot / intemporel
   - NATURE plus végétal / frais
   - ELEGANT plus profond / raffiné

## Plan d'exécution

### 1. Sécuriser le flux d'assets
- ajouter une normalisation des URLs d'images du menu public
- couvrir ce comportement par des tests ciblés

### 2. Recomposer l'écran menu
- retravailler le hero et l'entrée dans l'expérience
- transformer la navigation catégories en grille visuelle tactile
- renforcer le focus sur la catégorie sélectionnée

### 3. Refaire les cartes produits et les surfaces de détail
- cartes produits plus premium
- modal produit plus incarnée
- cohérence visuelle avec stepper menu, panier flottant et panneau panier

### 4. Revoir le système de palettes
- ajuster les tokens de `styles.scss`
- rendre chaque thème immédiatement perceptible sans changer le layout

### 5. Documentation design
- mettre à jour `client/DESIGN.CLIENT.CHAUD.md` si nécessaire
- créer :
  - `client/DESIGN.CLIENT.CLASSIQUE.md`
  - `client/DESIGN.CLIENT.NATURE.md`
  - `client/DESIGN.CLIENT.ELEGANT.md`

### 6. Validation et transmission
- exécuter les tests/build client
- préparer une PR avec un journal de bord temporaire
- garder ce fichier à jour pour reprise inter-session

## Fichiers pressentis

- `client/src/styles.scss`
- `client/src/app/features/menu/pages/menu-page/menu-page.component.ts`
- `client/src/app/features/menu/components/category-grid/category-grid.component.ts`
- `client/src/app/features/menu/components/item-list/item-list.component.ts`
- `client/src/app/features/menu/components/item-card/item-card.component.ts`
- `client/src/app/features/menu/components/item-modal/item-modal.component.ts`
- `client/src/app/features/menu/components/combo-stepper/combo-stepper.component.ts`
- `client/src/app/features/menu/services/menu.service.ts`
- `client/src/app/features/cart/components/floating-cart-bar/floating-cart-bar.component.ts`
- `client/src/app/features/cart/components/cart-panel/cart-panel.component.ts`
- `client/DESIGN.CLIENT.CHAUD.md`
- `client/DESIGN.CLIENT.CLASSIQUE.md`
- `client/DESIGN.CLIENT.NATURE.md`
- `client/DESIGN.CLIENT.ELEGANT.md`

## Journal de bord

### 2026-05-27
- Audit initial terminé.
- Branche créée depuis `main` : `feat/client-premium-menu-ui`.
- Baseline client validée avant changement :
  - `cd client && npm test -- --watch=false`
  - `cd client && npm run build`
- Correctif commencé sur la normalisation des URLs d'images du menu public.
- Normalisation des assets du menu public implémentée (`logoPath`, `category.imagePath`, `item.imagePath`, `menuItemImagePath`) avec tests dédiés.
- Refonte menée sur :
  - page menu
  - grille catégories
  - cartes produits
  - modal produit
  - combo stepper
  - panier flottant
  - panneau panier
- Palettes client retouchées pour rendre `chaud` réellement chaud et différencier plus nettement `classique`, `nature`, `elegant`.
- Fichiers design ajoutés :
  - `client/DESIGN.CLIENT.CLASSIQUE.md`
  - `client/DESIGN.CLIENT.NATURE.md`
  - `client/DESIGN.CLIENT.ELEGANT.md`
- `client/DESIGN.CLIENT.CHAUD.md` réaligné avec le nouveau socle UX commun.
- Revue ciblée effectuée après implémentation.
- Correctifs de revue appliqués :
  - le bouton **Composer en menu** transmet maintenant le bon item de base au combo stepper
  - les bottom sheets principales exposent désormais des attributs de dialogue et des labels de contrôle minimum
  - un test dédié protège le filtrage du combo stepper par `baseItemId`
- Validation actuelle :
  - `cd client && npm test -- --watch=false` ✅
  - `cd client && npm run build` ✅
  - `npm run test:e2e` lancé après `npm install` à la racine ; la suite s'exécute désormais mais échoue sur des scénarios existants liés au flux commande/confirmation (`e2e/admin-orders.spec.ts`, `e2e/client-confirmation.spec.ts`), pas sur la page menu refondue.
- Prochaines étapes de session :
  - vérifier visuellement le rendu si besoin sur environnement Windows / navigateur
  - commit
  - push
  - PR avec reprise du journal dans la description
